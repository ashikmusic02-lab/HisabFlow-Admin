package com.example.data.model

import androidx.annotation.Keep

@Keep
data class DailyStat(
    val dateUtc: String = "",
    val shortLabel: String = "",
    val active: Long = 0L
)

@Keep
data class DashboardData(
    val totalInstalls: Long = 0L,
    val todayActive: Long = 0L,
    val last7Days: List<DailyStat> = emptyList(),
    val lastRefreshedMillis: Long = 0L
)

@Keep
data class AdsConfig(
    val bannerEnabled: Boolean = true,
    val nativeEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
    val rewardedEnabled: Boolean = true,
    val interstitialMinIntervalSec: Int = 120,
    val updatedAtMillis: Long? = null,
    val existsOnServer: Boolean = false
)

enum class AdField(val firestoreKey: String) {
    BANNER("banner_enabled"),
    NATIVE("native_enabled"),
    INTERSTITIAL("interstitial_enabled"),
    REWARDED("rewarded_enabled")
}

enum class TargetScreen(val routeKey: String, val displayLabel: String) {
    NONE("", "নির্ধারিত নেই"),
    HOME("home", "App home"),
    REPORTS("reports", "Reports"),
    BUDGETS("budgets", "Budgets"),
    GOALS("goals", "Goals"),
    SETTINGS("settings", "Settings")
}

@Keep
data class PushHistoryItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val target: String = "all",
    val screen: String = "",
    val imageUrl: String = "",
    val timestampMillis: Long? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
