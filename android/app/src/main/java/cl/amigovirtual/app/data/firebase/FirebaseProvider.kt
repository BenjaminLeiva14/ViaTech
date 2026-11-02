package cl.amigovirtual.app.data.firebase

import android.content.Context
import cl.amigovirtual.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

class FirebaseProvider(context: Context) {
    val configured = BuildConfig.FIREBASE_PROJECT.isNotBlank() && BuildConfig.FIREBASE_API_KEY.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank()
    val app: FirebaseApp? = if (configured) FirebaseApp.initializeApp(context,
        FirebaseOptions.Builder().setProjectId(BuildConfig.FIREBASE_PROJECT)
            .setApiKey(BuildConfig.FIREBASE_API_KEY).setApplicationId(BuildConfig.FIREBASE_APP_ID).build(), "ViaTech") else null
    val auth = app?.let(FirebaseAuth::getInstance)
    val db = app?.let(FirebaseFirestore::getInstance)
    init {
        // Memory cache prevents a subsequent account from seeing another session's disk cache.
        // The simulator owns the durable event queue; commands require a confirmed connection.
        db?.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        if (BuildConfig.USE_EMULATORS) {
            auth?.useEmulator(BuildConfig.EMULATOR_HOST, 9099)
            db?.useEmulator(BuildConfig.EMULATOR_HOST, 8080)
        }
    }
}
