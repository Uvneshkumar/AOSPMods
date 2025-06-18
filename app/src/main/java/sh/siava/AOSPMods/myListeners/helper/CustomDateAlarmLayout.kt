package sh.siava.AOSPMods.myListeners.helper

import android.app.AlarmManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import sh.siava.AOSPMods.R
import sh.siava.AOSPMods.XPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomDateAlarmLayout(context: Context) : LinearLayout(context) {

    private val dateTextView: TextView
    private val alarmTextView: TextView
    private val alarmIcon: ImageView
    private val alarmLayout: LinearLayout
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable: Runnable
    private val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    private val alarmTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private var lastShownDate = ""
    private var lastShownAlarmTime = ""
    private var lastShownAlarmVisibility = View.INVISIBLE

    init {
        orientation = VERTICAL
        setPadding(0, dpToPx(4), 0, dpToPx(4))
        dateTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            includeFontPadding = false
        }
        val dateParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpToPx(8)
        }
        addView(dateTextView, dateParams)
        alarmLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            visibility = View.INVISIBLE
        }
        alarmIcon = ImageView(context).apply {
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    XPrefs.modRes,
                    R.drawable.ic_alarm,
                    null
                )
            )
        }
        val iconParams = LayoutParams(dpToPx(20), dpToPx(20)).apply {
            rightMargin = dpToPx(8)
        }
        alarmLayout.addView(alarmIcon, iconParams)
        alarmTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            includeFontPadding = false
        }
        alarmLayout.addView(alarmTextView)
        addView(alarmLayout)
        timeRunnable = object : Runnable {
            override fun run() {
                showCurrentDate()
                showNextAlarmIfExists()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timeRunnable)
    }

    private fun showCurrentDate() {
        val currentDate = dateFormat.format(Date())
        if (currentDate != lastShownDate) {
            lastShownDate = currentDate
            dateTextView.text = currentDate
        }
    }

    private fun showNextAlarmIfExists() {
        val nextAlarm = alarmManager?.nextAlarmClock
        var timeToShow = ""
        var alarmVisibility = View.INVISIBLE
        if (nextAlarm != null) {
            val alarmTimeMillis = nextAlarm.triggerTime
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = alarmTimeMillis
            val alarmTimeString = alarmTimeFormat.format(calendar.time)
            val timeDifferenceMillis = alarmTimeMillis - System.currentTimeMillis()
            val hoursDifference = timeDifferenceMillis / (1000 * 60 * 60)
            if (hoursDifference < 12) {
                timeToShow = alarmTimeString
                alarmVisibility = View.VISIBLE
            }
        }
        if (timeToShow != lastShownAlarmTime) {
            lastShownAlarmTime = timeToShow
            alarmTextView.text = timeToShow
        }
        if (alarmVisibility != lastShownAlarmVisibility) {
            lastShownAlarmVisibility = alarmVisibility
            alarmLayout.visibility = alarmVisibility
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
