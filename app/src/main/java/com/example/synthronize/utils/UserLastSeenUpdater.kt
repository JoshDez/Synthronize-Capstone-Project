import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.synthronize.utils.FirebaseUtil
import com.google.firebase.Timestamp

class UserLastSeenUpdater(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 5 * 60 * 1000 // 5 minutes
    private val firstInterval: Long = 5000 // 5 minutes
    private var isFirstInterval = true

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateLastSeen()
            if (isFirstInterval){
                isFirstInterval = false
                handler.postDelayed(this, firstInterval)
            } else {
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    fun startUpdating() {
        handler.post(updateRunnable)
    }

    private fun updateLastSeen() {
        val sharedPreferences: SharedPreferences by lazy {
            context.getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        }
        val isOnlineStatusEnabled = sharedPreferences.getBoolean("online_status_enable", true)
        if (isOnlineStatusEnabled && !isFirstInterval){
            val updates = hashMapOf<String, Any>(
                "currentStatus.lastSeen" to Timestamp.now(),
            )
            FirebaseUtil().currentUserDetails().update(updates)
        }
    }
}
