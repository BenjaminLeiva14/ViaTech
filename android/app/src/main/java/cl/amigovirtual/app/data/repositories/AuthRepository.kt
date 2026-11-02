package cl.amigovirtual.app.data.repositories

import cl.amigovirtual.app.data.firebase.FirebaseProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebase: FirebaseProvider) {
    val sessions = callbackFlow {
        val auth = firebase.auth
        if (auth == null) { trySend(null); close(); return@callbackFlow }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    suspend fun login(email: String, password: String) {
        requireNotNull(firebase.auth).signInWithEmailAndPassword(email.trim(), password).await()
    }
    suspend fun register(email: String, password: String) {
        val auth = requireNotNull(firebase.auth)
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        // No client-created role. Account remains unauthorized until an admin grants access.
    }
    suspend fun recover(email: String) { requireNotNull(firebase.auth).sendPasswordResetEmail(email.trim()).await() }
    fun logout() { firebase.auth?.signOut() }
}
