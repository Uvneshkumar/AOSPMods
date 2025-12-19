package sh.siava.AOSPMods.myListeners.helper

import android.app.AlarmManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
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
    private var lastShownAlarmVisibility = GONE

    val boldTextVariation = "'wght' 600, 'ROND' 100"

    init {
        orientation = VERTICAL
        setPadding(0, dpToPx(4), 0, dpToPx(4))
        dateTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
        }
        addView(dateTextView)
        alarmLayout = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
            visibility = GONE
        }
        alarmIcon = ImageView(context).apply {
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    XPrefs.modRes, R.drawable.ic_alarm, null
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
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
        }
        alarmLayout.addView(alarmTextView)
        val alarmParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(4)
        }
        addView(alarmLayout, alarmParams)
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
            alarmLayout.visibility = alarmVisibility
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}
