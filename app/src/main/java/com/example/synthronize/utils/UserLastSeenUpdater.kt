import android.os.Handler
import android.os.Looper
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Timestamp

class UserLastSeenUpdater() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 5 * 60 * 1000 // 5 minutes

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateLastSeen()
            handler.postDelayed(this, updateInterval)
        }
    }

    fun startUpdating() {
        handler.post(updateRunnable)
    }

    private fun updateLastSeen() {
        val updates = hashMapOf<String, Any>(
            "currentStatus.lastSeen" to Timestamp.now(),
        )
        FirebaseUtil().currentUserDetails().update(updates)
    }
}
