package cl.amigovirtual.app
import android.app.Application
import cl.amigovirtual.app.data.firebase.FirebaseProvider
class ViaTechApplication : Application() {
    lateinit var firebase: FirebaseProvider
    override fun onCreate() { super.onCreate(); firebase = FirebaseProvider(this) }
}
