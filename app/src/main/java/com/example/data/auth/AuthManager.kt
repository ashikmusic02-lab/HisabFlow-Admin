package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.config.AdminConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthGateState {
    data object Checking : AuthGateState
    data class FirebaseNotConfigured(val message: String) : AuthGateState
    data class SignedOut(val errorMessage: String? = null) : AuthGateState
    data class SignedIn(
        val uid: String,
        val email: String,
        val displayName: String?
    ) : AuthGateState
}

object AdminAuthManager {
    private const val TAG = "AdminAuthManager"

    fun isAuthorizedAdmin(user: FirebaseUser?): Boolean {
        val rawEmail = user?.email
        Log.d(TAG, "Admin login check - incoming email: $rawEmail")
        val email = rawEmail?.trim()?.lowercase() ?: return false
        return email == "sfforhadvai@gmail.com"
    }
    suspend fun evaluateCurrentSession(context: Context): AuthGateState {
        if (!AdminConfig.isFirebaseConfigured(context)) {
            return AuthGateState.FirebaseNotConfigured(
                context.getString(R.string.auth_firebase_not_configured)
            )
        }
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return if (isAuthorizedAdmin(currentUser)) {
                AuthGateState.SignedIn(
                    uid = currentUser.uid,
                    email = currentUser.email.orEmpty(),
                    displayName = currentUser.displayName
                )
            } else {
                signOut(context)
                AuthGateState.SignedOut(context.getString(R.string.auth_unauthorized))
            }
        }
        return attemptSilentSignIn(context)
    }

    private suspend fun attemptSilentSignIn(context: Context): AuthGateState {
        val clientId = AdminConfig.resolveWebClientId(context)
            ?: return AuthGateState.SignedOut()

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            handleCredentialResult(context, result.credential)
        } catch (_: NoCredentialException) {
            AuthGateState.SignedOut()
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google Sign-In cancelled: ${e.message}")
            AuthGateState.SignedOut()
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google Sign-In skipped: ${e.message}")
            AuthGateState.SignedOut()
        }
    }

    suspend fun signInWithGoogle(activity: Activity): AuthGateState {
        if (!AdminConfig.isFirebaseConfigured(activity)) {
            return AuthGateState.FirebaseNotConfigured(
                activity.getString(R.string.auth_firebase_not_configured)
            )
        }
        val clientId = AdminConfig.resolveWebClientId(activity)
            ?: return AuthGateState.SignedOut(
                activity.getString(R.string.auth_firebase_not_configured)
            )

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            handleCredentialResult(activity, result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            AuthGateState.SignedOut()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            AuthGateState.SignedOut(e.localizedMessage ?: "সাইন ইন ব্যর্থ হয়েছে")
        }
    }

    private suspend fun handleCredentialResult(
        context: Context,
        credential: androidx.credentials.Credential
    ): AuthGateState {
        if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return AuthGateState.SignedOut("অপ্রত্যাশিত ক্রেডেনশিয়াল ধরন")
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = Firebase.auth.signInWithCredential(authCredential).await()
        val user = authResult.user
        return if (isAuthorizedAdmin(user)) {
            AuthGateState.SignedIn(
                uid = user!!.uid,
                email = user.email.orEmpty(),
                displayName = user.displayName
            )
        } else {
            signOut(context)
            AuthGateState.SignedOut(context.getString(R.string.auth_unauthorized))
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                if (AdminConfig.isFirebaseConfigured(context)) {
                    Firebase.auth.signOut()
                }
            }
        }
    }
}
