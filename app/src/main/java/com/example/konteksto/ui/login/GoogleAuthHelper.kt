package com.example.konteksto.ui.login

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.konteksto.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

object GoogleAuthHelper {

    suspend fun requestGoogleIdToken(
        context: Context,
        autoSelectEnabled: Boolean = false,
    ): GoogleSignInResult {
        val webClientId = resolveWebClientId(context)
        if (webClientId.isBlank() || webClientId == "YOUR_WEB_CLIENT_ID") {
            return GoogleSignInResult.Error(
                "Missing Web Client ID. Add google-services.json to app/ or set firebase_web_client_id in strings.xml.",
            )
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(autoSelectEnabled)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = context,
                request = request,
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Error("Received an unsupported credential type from Google Sign-In.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(
                e.errorMessage?.toString()
                    ?: e.localizedMessage
                    ?: "Google Sign-In failed. Ensure SHA-1 is added in Firebase Console.",
            )
        } catch (e: Exception) {
            GoogleSignInResult.Error(
                e.localizedMessage ?: "An unexpected error occurred during Google Sign-In.",
            )
        }
    }

    suspend fun clearCredentialState(context: Context) {
        runCatching {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun resolveWebClientId(context: Context): String {
        val generatedId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (generatedId != 0) {
            val value = context.getString(generatedId)
            if (value.isNotBlank()) return value
        }
        return context.getString(R.string.firebase_web_client_id)
    }
}
