package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome()
    data class NeedsResolution(val intentSenderRequest: IntentSenderRequest) : DriveAuthOutcome()
    data class Error(val message: String) : DriveAuthOutcome()
}

object DriveAuthManager {
    private val driveReadonlyScope = Scope("https://www.googleapis.com/auth/drive.readonly")

    suspend fun requestDriveAuthorization(activity: Activity): DriveAuthOutcome =
        suspendCancellableCoroutine { continuation ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(driveReadonlyScope))
                .build()

            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val pendingIntent = result.pendingIntent
                    val token = result.accessToken
                    when {
                        result.hasResolution() && pendingIntent != null -> {
                            continuation.resume(
                                DriveAuthOutcome.NeedsResolution(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            )
                        }
                        !token.isNullOrBlank() -> {
                            continuation.resume(DriveAuthOutcome.Authorized(token))
                        }
                        else -> {
                            continuation.resume(
                                DriveAuthOutcome.Error("Token akses Google Drive tidak diterima.")
                            )
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (!continuation.isActive) return@addOnFailureListener
                    val msg = if (e is ApiException) {
                        "Otorisasi Google Drive gagal (kode ${e.statusCode}): ${e.localizedMessage ?: "Periksa konfigurasi OAuth Android"}"
                    } else {
                        e.localizedMessage ?: "Gagal terhubung ke Google Identity Services"
                    }
                    continuation.resume(DriveAuthOutcome.Error(msg))
                }
        }

    fun extractTokenFromIntent(context: Context, data: Intent?): Result<String> {
        return runCatching {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (token.isNullOrBlank()) {
                error("Token akses Google Drive kosong setelah konfirmasi.")
            }
            token
        }
    }
}
