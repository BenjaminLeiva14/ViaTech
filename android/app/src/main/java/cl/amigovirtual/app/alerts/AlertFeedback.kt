package cl.amigovirtual.app.alerts

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Señal breve local; no publica notificaciones ni ejecuta servicios en segundo plano. */
class AlertFeedback(context: Context) {
    private val app = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var tone: Ringtone? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        app.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") (app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    private val stopTask = Runnable { stop() }

    fun play(sound: Boolean, vibration: Boolean) {
        stop()
        val audio = app.getSystemService(AudioManager::class.java) ?: return
        val notifications = app.getSystemService(NotificationManager::class.java) ?: return
        // No solicitar acceso para eludir No molestar ni subir el volumen del usuario.
        if (notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        if (audio.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        if (sound && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            tone = uri?.let { RingtoneManager.getRingtone(app, it) }
            tone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            if (Build.VERSION.SDK_INT >= 28) tone?.isLooping = false
            tone?.play()
        }
        if (vibration) runCatching {
            if (vibrator?.hasVibrator() == true) {
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1)
                if (Build.VERSION.SDK_INT >= 33) vibrator.vibrate(effect,
                    VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_NOTIFICATION).build())
                else vibrateLegacy(effect)
            }
        }
        handler.postDelayed(stopTask, 1500)
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(effect: VibrationEffect) {
        vibrator?.vibrate(effect, AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
    }

    fun stop() {
        handler.removeCallbacks(stopTask)
        runCatching { tone?.stop() }
        tone = null
        runCatching { vibrator?.cancel() }
    }
}
