package com.example.data.repository

import com.example.config.AdminConfig
import com.example.data.model.AdField
import com.example.data.model.AdsConfig
import com.example.data.model.DailyStat
import com.example.data.model.DashboardData
import com.example.data.model.PushHistoryItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AdminRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val functions: FirebaseFunctions = Firebase.functions(AdminConfig.FUNCTIONS_REGION)
) {
    private fun utcDateKeysLast7Days(): List<Pair<String, String>> {
        val utc = TimeZone.getTimeZone("UTC")
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = utc }
        val shortFormat = SimpleDateFormat("dd/MM", Locale.ENGLISH).apply { timeZone = utc }
        val cal = Calendar.getInstance(utc, Locale.ENGLISH).apply {
            add(Calendar.DAY_OF_YEAR, -6)
        }
        return buildList(7) {
            repeat(7) {
                val date = cal.time
                add(keyFormat.format(date) to shortFormat.format(date))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    fun observeDashboardStats(): Flow<DashboardData> {
        val dateKeys = utcDateKeysLast7Days()
        val totalFlow = firestore.collection("stats")
            .document("total")
            .snapshots()
            .map { snap -> snap.readLongOrZero("installs") }

        val dailyFlows = dateKeys.map { (dateKey, shortLabel) ->
            firestore.collection("stats")
                .document("daily_$dateKey")
                .snapshots()
                .map { snap ->
                    DailyStat(
                        dateUtc = dateKey,
                        shortLabel = shortLabel,
                        active = snap.readLongOrZero("active")
                    )
                }
        }

        val combinedDailyFlow: Flow<List<DailyStat>> = combine(dailyFlows) { array ->
            array.toList()
        }

        return combine(totalFlow, combinedDailyFlow) { totalInstalls, dailyList ->
            DashboardData(
                totalInstalls = totalInstalls,
                todayActive = dailyList.lastOrNull()?.active ?: 0L,
                last7Days = dailyList,
                lastRefreshedMillis = System.currentTimeMillis()
            )
        }
    }

    suspend fun refreshDashboardFromServer(): DashboardData = coroutineScope {
        val dateKeys = utcDateKeysLast7Days()
        val totalDeferred = async {
            fetchDocWithServerFallback("stats", "total").readLongOrZero("installs")
        }
        val dailyDeferred = dateKeys.map { (dateKey, shortLabel) ->
            async {
                val snap = fetchDocWithServerFallback("stats", "daily_$dateKey")
                DailyStat(
                    dateUtc = dateKey,
                    shortLabel = shortLabel,
                    active = snap.readLongOrZero("active")
                )
            }
        }
        val totalInstalls = totalDeferred.await()
        val dailyList = dailyDeferred.awaitAll()
        DashboardData(
            totalInstalls = totalInstalls,
            todayActive = dailyList.lastOrNull()?.active ?: 0L,
            last7Days = dailyList,
            lastRefreshedMillis = System.currentTimeMillis()
        )
    }

    private suspend fun fetchDocWithServerFallback(collection: String, docId: String): DocumentSnapshot {
        val ref = firestore.collection(collection).document(docId)
        return try {
            ref.get(Source.SERVER).await()
        } catch (_: Exception) {
            ref.get(Source.DEFAULT).await()
        }
    }

    fun observeAdsConfig(): Flow<AdsConfig> {
        return firestore.collection("app_config")
            .document("ads")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) {
                    AdsConfig(existsOnServer = false)
                } else {
                    AdsConfig(
                        bannerEnabled = snap.getBoolean("banner_enabled") ?: true,
                        nativeEnabled = snap.getBoolean("native_enabled") ?: true,
                        interstitialEnabled = snap.getBoolean("interstitial_enabled") ?: true,
                        rewardedEnabled = snap.getBoolean("rewarded_enabled") ?: true,
                        interstitialMinIntervalSec = (snap.getLong("interstitial_min_interval_sec")
                            ?: AdminConfig.DEFAULT_INTERSTITIAL_INTERVAL_SEC.toLong()).toInt(),
                        updatedAtMillis = snap.getTimestamp("updated_at")?.toDate()?.time,
                        existsOnServer = true
                    )
                }
            }
    }

    suspend fun updateAdSwitch(field: AdField, enabled: Boolean, currentConfig: AdsConfig) {
        val docRef = firestore.collection("app_config").document("ads")
        if (!currentConfig.existsOnServer) {
            val initialPayload = hashMapOf<String, Any>(
                AdField.BANNER.firestoreKey to (if (field == AdField.BANNER) enabled else true),
                AdField.NATIVE.firestoreKey to (if (field == AdField.NATIVE) enabled else true),
                AdField.INTERSTITIAL.firestoreKey to (if (field == AdField.INTERSTITIAL) enabled else true),
                AdField.REWARDED.firestoreKey to (if (field == AdField.REWARDED) enabled else true),
                "interstitial_min_interval_sec" to AdminConfig.DEFAULT_INTERSTITIAL_INTERVAL_SEC,
                "updated_at" to FieldValue.serverTimestamp()
            )
            docRef.set(initialPayload).await()
        } else {
            val partialUpdate = hashMapOf<String, Any>(
                field.firestoreKey to enabled,
                "updated_at" to FieldValue.serverTimestamp()
            )
            docRef.set(partialUpdate, SetOptions.merge()).await()
        }
    }

    suspend fun updateInterstitialInterval(seconds: Int, currentConfig: AdsConfig) {
        require(seconds in AdminConfig.MIN_INTERSTITIAL_INTERVAL_SEC..AdminConfig.MAX_INTERSTITIAL_INTERVAL_SEC) {
            "Interval must be between 10 and 600 seconds"
        }
        val docRef = firestore.collection("app_config").document("ads")
        if (!currentConfig.existsOnServer) {
            val initialPayload = hashMapOf<String, Any>(
                AdField.BANNER.firestoreKey to true,
                AdField.NATIVE.firestoreKey to true,
                AdField.INTERSTITIAL.firestoreKey to true,
                AdField.REWARDED.firestoreKey to true,
                "interstitial_min_interval_sec" to seconds,
                "updated_at" to FieldValue.serverTimestamp()
            )
            docRef.set(initialPayload).await()
        } else {
            val partialUpdate = hashMapOf<String, Any>(
                "interstitial_min_interval_sec" to seconds,
                "updated_at" to FieldValue.serverTimestamp()
            )
            docRef.set(partialUpdate, SetOptions.merge()).await()
        }
    }

    fun observePushHistory(): Flow<List<PushHistoryItem>> {
        return firestore.collection("push_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.map { doc ->
                    val ts = doc.get("timestamp") as? Timestamp
                    PushHistoryItem(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        target = doc.getString("target").orEmpty().ifBlank { "all" },
                        screen = doc.getString("screen").orEmpty(),
                        imageUrl = doc.getString("imageUrl").orEmpty(),
                        timestampMillis = ts?.toDate()?.time
                    )
                }
            }
    }

    suspend fun sendPushNotification(
        title: String,
        body: String,
        imageUrl: String,
        screen: String,
        target: String
    ) {
        val data = hashMapOf(
            "title" to title.trim(),
            "body" to body.trim(),
            "imageUrl" to imageUrl.trim(),
            "screen" to screen.trim(),
            "target" to target
        )
        functions.getHttpsCallable("sendPush")
            .call(data)
            .await()
    }

    private fun DocumentSnapshot.readLongOrZero(field: String): Long {
        if (!exists()) return 0L
        return (get(field) as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
    }
}
