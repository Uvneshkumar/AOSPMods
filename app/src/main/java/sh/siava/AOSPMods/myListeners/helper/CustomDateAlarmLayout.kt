package sh.siava.AOSPMods.myListeners.helper

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import sh.siava.AOSPMods.R
import sh.siava.AOSPMods.XPrefs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomDateAlarmLayout(context: Context) : LinearLayout(context) {

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val dateTextView: TextView
    private val alarmTextView: TextView
    private val alarmIcon: ImageView
    private val dndIcon: ImageView
    private val firstLine: LinearLayout
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable: Runnable
    private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val alarmTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private var lastShownDate = ""
    private var lastShownAlarmTime = ""
    private var lastShownAlarmVisibility = GONE

    val boldTextVariation = "'wght' 600, 'ROND' 100"

    init {
        orientation = VERTICAL
        setPadding(0, dpToPx(4), 0, dpToPx(4))
        firstLine = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
        }
        dndIcon = ImageView(context).apply {
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    XPrefs.modRes, R.drawable.ic_do_not_disturb_on, null
                )
            )
            visibility = GONE
        }
        val dndParams = LayoutParams(dpToPx(24), dpToPx(24)).apply {
            rightMargin = dpToPx(8)
        }
        firstLine.addView(dndIcon, dndParams)
        dateTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
        }
        firstLine.addView(dateTextView)
        alarmIcon = ImageView(context).apply {
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    XPrefs.modRes, R.drawable.ic_alarm, null
                )
            )
        }
        val iconParams = LayoutParams(dpToPx(20), dpToPx(20)).apply {
            leftMargin = dpToPx(8)
        }
        firstLine.addView(alarmIcon, iconParams)
        alarmTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
        }
        val alarmTextParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dpToPx(8)
            }
        firstLine.addView(alarmTextView, alarmTextParams)
        addView(firstLine)
        updateSilentState()
        val filter = IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED)
        context.registerReceiver(DoNotDisturbChangeReceiver(), filter)
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
        var alarmVisibility = GONE
        if (nextAlarm != null) {
            val alarmTimeMillis = nextAlarm.triggerTime
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = alarmTimeMillis
            val alarmTimeString = alarmTimeFormat.format(calendar.time)
            val timeDifferenceMillis = alarmTimeMillis - System.currentTimeMillis()
            val hoursDifference = timeDifferenceMillis / (1000 * 60 * 60)
            if (hoursDifference < 12) {
                timeToShow = alarmTimeString
                alarmVisibility = VISIBLE
            }
        }
        if (timeToShow != lastShownAlarmTime) {
            lastShownAlarmTime = timeToShow
            alarmTextView.text = timeToShow
        }
        if (alarmVisibility != lastShownAlarmVisibility) {
            lastShownAlarmVisibility = alarmVisibility
            alarmIcon.visibility = alarmVisibility
            alarmTextView.visibility = alarmVisibility
        }
    }

    fun isDndEnabled(): Boolean {
        return notificationManager?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    fun updateSilentState() {
        dndIcon.isVisible = isDndEnabled()
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    private inner class DoNotDisturbChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateSilentState()
        }
    }
}
