package com.example.auth

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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthManager"

class AuthenticationManager(
  private val context: Context,
  private val auth: FirebaseAuth = Firebase.auth
) {
  private val credentialManager: CredentialManager by lazy {
    CredentialManager.create(context)
  }

  val currentUser: FirebaseUser?
    get() = auth.currentUser

  val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In configuration missing: default_web_client_id not found", e)
      return Result.failure(IllegalStateException("Configuration error: default_web_client_id not found in app resources"))
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    return try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val user = withContext(NonCancellable) {
          val authResult = auth.signInWithCredential(authCredential).await()
          checkNotNull(authResult.user) { "Authentication returned empty user record" }
        }
        Log.i(TAG, "Interactive Google Sign-In succeeded for user: ${user.uid}")
        Result.success(user)
      } else {
        Result.failure(IllegalArgumentException("Unexpected credential format returned by system"))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow was dismissed by user: ${e.message}")
      Result.failure(e)
    } catch (e: CancellationException) {
      // Cooperative cancellation must propagate without being logged as an error
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      Result.failure(e)
    }
  }

  suspend fun attemptSilentSignIn(): Result<FirebaseUser?> {
    if (auth.currentUser != null) {
      return Result.success(auth.currentUser)
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    return try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val user = withContext(NonCancellable) {
          val authResult = auth.signInWithCredential(authCredential).await()
          authResult.user
        }
        Result.success(user)
      } else {
        Result.success(null)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: NoCredentialException) {
      // Normal for clean devices where account hasn't been explicitly granted
      Result.success(null)
    } catch (e: Exception) {
      Log.d(TAG, "Silent sign-in not available: ${e.message}")
      Result.success(null)
    }
  }

  suspend fun signOut(): Result<Unit> {
    return try {
      withContext(NonCancellable) {
        auth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      }
      Log.i(TAG, "Sign out completed and credential state cleared")
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Error during sign out", e)
      Result.failure(e)
    }
  }
}
