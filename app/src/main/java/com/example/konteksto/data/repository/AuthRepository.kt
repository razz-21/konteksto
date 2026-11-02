package com.example.konteksto.data.repository

import com.example.konteksto.domain.model.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    val currentUser: AuthUser?
    fun getAuthStateStream(): Flow<AuthUser?>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser>
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<AuthUser>
    suspend fun signInAnonymously(): Result<AuthUser>
    fun signOut()
}

class FirebaseAuthRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
) : AuthRepository {

    private val firebaseAuthOrNull: FirebaseAuth?
        get() = try {
            authProvider()
        } catch (_: Exception) {
            null
        }

    override val currentUser: AuthUser?
        get() = firebaseAuthOrNull?.currentUser?.toDomainUser()

    override fun getAuthStateStream(): Flow<AuthUser?> = callbackFlow {
        val auth = firebaseAuthOrNull
        if (auth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomainUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> = runCatching {
        val auth = requireFirebaseAuth()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        authResult.user?.toDomainUser()
            ?: throw IllegalStateException("Firebase sign-in succeeded, but user was null.")
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<AuthUser> = runCatching {
        val auth = requireFirebaseAuth()
        val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
        authResult.user?.toDomainUser()
            ?: throw IllegalStateException("Firebase sign-in succeeded, but user was null.")
    }

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Result<AuthUser> = runCatching {
        val auth = requireFirebaseAuth()
        val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        authResult.user?.toDomainUser()
            ?: throw IllegalStateException("Firebase sign-up succeeded, but user was null.")
    }

    override suspend fun signInAnonymously(): Result<AuthUser> = runCatching {
        val auth = requireFirebaseAuth()
        val authResult = auth.signInAnonymously().await()
        authResult.user?.toDomainUser()
            ?: throw IllegalStateException("Anonymous sign-in succeeded, but user was null.")
    }

    override fun signOut() {
        firebaseAuthOrNull?.signOut()
    }

    private fun requireFirebaseAuth(): FirebaseAuth {
        return firebaseAuthOrNull
            ?: throw IllegalStateException(
                "Firebase is not configured yet. Please add google-services.json to the app/ directory and enable the google-services plugin.",
            )
    }

    private fun FirebaseUser.toDomainUser(): AuthUser = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
        isAnonymous = isAnonymous,
    )
}
