package com.sentinelshield.antitheft.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.sentinelshield.antitheft.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

object GoogleDriveSyncManager {
    private const val TAG = "GoogleDriveSync"
    const val FOLDER_NAME = "SentinelShield"
    val DRIVE_FILE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")

    fun getGoogleSignInClient(context: Context, requestDriveScopeDirectly: Boolean = true): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (requestDriveScopeDirectly) {
            builder.requestScopes(DRIVE_FILE_SCOPE)
        }

        return GoogleSignIn.getClient(context, builder.build())
    }

    fun isUserSignedIn(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val hasScope = account != null && GoogleSignIn.hasPermissions(account, DRIVE_FILE_SCOPE)
        val savedEmail = SecurityPreferences.getGoogleDriveEmail(context)
        val isScopeGranted = SecurityPreferences.isGoogleDriveScopeGranted(context)
        return (account != null && (hasScope || isScopeGranted)) || (!savedEmail.isNullOrBlank() && (hasScope || isScopeGranted))
    }

    fun isAccountSelected(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null || !SecurityPreferences.getGoogleDriveEmail(context).isNullOrBlank()
    }

    fun getSignedInAccountEmail(context: Context): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && account.email != null) {
            return account.email
        }
        return SecurityPreferences.getGoogleDriveEmail(context)
    }

    fun recordSuccessfulSignIn(context: Context, account: GoogleSignInAccount) {
        account.email?.let { email ->
            SecurityPreferences.setGoogleDriveEmail(context, email)
        }
        if (GoogleSignIn.hasPermissions(account, DRIVE_FILE_SCOPE)) {
            SecurityPreferences.setGoogleDriveScopeGranted(context, true)
        }
        DebugLogger.log(context, TAG, "Recorded active Google Drive account: ${account.email}", force = true)
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        val client = getGoogleSignInClient(context, requestDriveScopeDirectly = false)
        SecurityPreferences.setGoogleDriveEmail(context, null)
        SecurityPreferences.setGoogleDriveFolderId(context, null)
        SecurityPreferences.setGoogleDriveScopeGranted(context, false)
        client.signOut().addOnCompleteListener {
            DebugLogger.log(context, TAG, "Google account signed out successfully.", force = true)
            onComplete()
        }
    }

    fun getSigningCertificateSha1(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "Unable to read certificate"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            digest.joinToString(":") { String.format("%02X", it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun getAccessToken(context: Context, account: GoogleSignInAccount?): String? = withContext(Dispatchers.IO) {
        val email = account?.email ?: SecurityPreferences.getGoogleDriveEmail(context)
        val googleAccount = account?.account ?: (email?.let { android.accounts.Account(it, "com.google") })
        if (googleAccount == null) {
            DebugLogger.log(context, TAG, "OAuth error: No Android account or email associated with Google account.", force = true)
            return@withContext null
        }
        try {
            val scopeString = "oauth2:https://www.googleapis.com/auth/drive.file"
            GoogleAuthUtil.getToken(context, googleAccount, scopeString)
        } catch (e: Exception) {
            DebugLogger.log(context, TAG, "Failed to get Google OAuth token via GoogleAuthUtil: ${e.message}", force = true)
            null
        }
    }

    private suspend fun getOrCreateFolderId(context: Context, accessToken: String): String? = withContext(Dispatchers.IO) {
        val cachedFolderId = SecurityPreferences.getGoogleDriveFolderId(context)
        if (!cachedFolderId.isNullOrBlank()) {
            return@withContext cachedFolderId
        }

        try {
            // 1. Search for existing folder named FOLDER_NAME
            val query = URLEncoder.encode("name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
            val searchUrl = URL("https://www.googleapis.com/drive/v3/files?q=$query&spaces=drive&fields=files(id,name)")
            val searchConn = searchUrl.openConnection() as HttpURLConnection
            searchConn.requestMethod = "GET"
            searchConn.setRequestProperty("Authorization", "Bearer $accessToken")
            searchConn.connectTimeout = 10000
            searchConn.readTimeout = 10000

            try {
                val searchCode = searchConn.responseCode
                if (searchCode in 200..299) {
                    val responseText = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        val folderId = files.getJSONObject(0).getString("id")
                        SecurityPreferences.setGoogleDriveFolderId(context, folderId)
                        DebugLogger.log(context, TAG, "Found existing Drive folder '$FOLDER_NAME': $folderId", force = true)
                        return@withContext folderId
                    }
                }
            } finally {
                searchConn.disconnect()
            }

            // 2. Create the folder if not found
            val createUrl = URL("https://www.googleapis.com/drive/v3/files")
            val createConn = createUrl.openConnection() as HttpURLConnection
            createConn.requestMethod = "POST"
            createConn.doOutput = true
            createConn.setRequestProperty("Authorization", "Bearer $accessToken")
            createConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            createConn.connectTimeout = 10000
            createConn.readTimeout = 10000

            val folderMeta = JSONObject().apply {
                put("name", FOLDER_NAME)
                put("mimeType", "application/vnd.google-apps.folder")
            }

            try {
                OutputStreamWriter(createConn.outputStream, "UTF-8").use { writer ->
                    writer.write(folderMeta.toString())
                    writer.flush()
                }

                val createCode = createConn.responseCode
                if (createCode in 200..299) {
                    val createResponse = createConn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(createResponse)
                    val folderId = json.getString("id")
                    SecurityPreferences.setGoogleDriveFolderId(context, folderId)
                    DebugLogger.log(context, TAG, "Created new Drive folder '$FOLDER_NAME': $folderId", force = true)
                    return@withContext folderId
                } else {
                    val errorBody = createConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    DebugLogger.log(context, TAG, "Failed to create Drive folder '$FOLDER_NAME' (HTTP $createCode): $errorBody", force = true)
                }
            } finally {
                createConn.disconnect()
            }
        } catch (e: Exception) {
            DebugLogger.log(context, TAG, "Folder resolution error: ${e.message}", force = true)
        }
        null
    }

    suspend fun uploadBytes(context: Context, data: ByteArray, fileName: String, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        if (!isUserSignedIn(context)) {
            DebugLogger.log(context, TAG, "Upload skipped: Google Drive account not connected ($fileName)", force = true)
            return@withContext false
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        DebugLogger.log(context, TAG, "Initiating Google Drive cloud upload for: $fileName (${data.size} bytes)...", force = true)

        var currentAccessToken = getAccessToken(context, account)
        if (currentAccessToken == null) {
            DebugLogger.log(context, TAG, "Upload skipped: Unable to acquire OAuth2 access token.", force = true)
            return@withContext false
        }

        // Retry loop: max 2 attempts (handles token refresh on 401 or folder recreation on 404)
        for (attempt in 1..2) {
            val token = currentAccessToken ?: break
            val folderId = getOrCreateFolderId(context, token)
            val boundary = "SentinelShieldBoundary${System.currentTimeMillis()}"

            try {
                val uploadUrl = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                val conn = uploadUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.connectTimeout = 15000
                conn.readTimeout = 30000

                val metadataJson = JSONObject().apply {
                    put("name", fileName)
                    put("mimeType", mimeType)
                    if (!folderId.isNullOrBlank()) {
                        put("parents", org.json.JSONArray().put(folderId))
                    }
                }

                val metadataPart = "--$boundary\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                        metadataJson.toString() + "\r\n"

                val mediaHeader = "--$boundary\r\n" +
                        "Content-Type: $mimeType\r\n\r\n"

                val endBoundary = "\r\n--$boundary--\r\n"

                conn.outputStream.use { os ->
                    os.write(metadataPart.toByteArray(Charsets.UTF_8))
                    os.write(mediaHeader.toByteArray(Charsets.UTF_8))
                    os.write(data)
                    os.write(endBoundary.toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    DebugLogger.log(context, TAG, "Successfully uploaded $fileName (${data.size} bytes) to Google Drive folder '$FOLDER_NAME' (HTTP $responseCode)!", force = true)
                    conn.disconnect()
                    return@withContext true
                } else if (responseCode == 401) {
                    // Token expired; clear cache and try to fetch fresh token for attempt 2
                    try {
                        GoogleAuthUtil.clearToken(context, token)
                    } catch (_: Exception) {}
                    val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    DebugLogger.log(context, TAG, "Upload unauthorized (HTTP 401, attempt $attempt): $errorBody. Refreshing token...", force = true)
                    conn.disconnect()
                    currentAccessToken = getAccessToken(context, account)
                    if (currentAccessToken == null) break
                } else if (responseCode == 404) {
                    // Folder parent might be invalid or removed
                    SecurityPreferences.setGoogleDriveFolderId(context, null)
                    val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    DebugLogger.log(context, TAG, "Upload target folder not found (HTTP 404, attempt $attempt): $errorBody. Re-creating folder...", force = true)
                    conn.disconnect()
                } else {
                    val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    DebugLogger.log(context, TAG, "Google Drive upload failed (HTTP $responseCode): $errorBody", force = true)
                    conn.disconnect()
                    break
                }
            } catch (e: Exception) {
                DebugLogger.log(context, TAG, "Google Drive upload network error for $fileName (attempt $attempt): ${e.message}", force = true)
            }
        }
        false
    }

    suspend fun uploadUri(context: Context, uri: Uri, fileName: String, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        try {
            var bytes: ByteArray? = null
            // Retry reading up to 3 times in case MediaStore file handle is pending or being finalized
            for (i in 0..2) {
                bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                if (bytes != null && bytes.isNotEmpty()) break
                kotlinx.coroutines.delay(400L)
            }
            if (bytes == null || bytes.isEmpty()) {
                DebugLogger.log(context, TAG, "Upload skipped: Empty or unreadable content URI: $uri", force = true)
                return@withContext false
            }
            uploadBytes(context, bytes, fileName, mimeType)
        } catch (e: Exception) {
            DebugLogger.log(context, TAG, "Error reading content URI $uri: ${e.message}", force = true)
            false
        }
    }

    suspend fun uploadFile(context: Context, file: File, mimeType: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            DebugLogger.log(context, TAG, "Upload skipped: File does not exist (${file.name})", force = true)
            return@withContext false
        }
        try {
            val bytes = file.readBytes()
            uploadBytes(context, bytes, file.name, mimeType)
        } catch (e: Exception) {
            DebugLogger.log(context, TAG, "Error reading file ${file.name}: ${e.message}", force = true)
            false
        }
    }
}
