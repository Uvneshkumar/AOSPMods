package sh.siava.AOSPMods.myListeners.helper

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import sh.siava.AOSPMods.R
import sh.siava.AOSPMods.XPrefs
import sh.siava.AOSPMods.utils.SystemUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class CustomDateAlarmLayout(context: Context) : LinearLayout(context) {

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (enabled) {
                setSmartspaceText(torchText)
                context.sendBroadcast(Intent(MyBroadcastReceiver.TORCH_ON))
            } else {
                setSmartspaceText("")
                context.sendBroadcast(Intent(MyBroadcastReceiver.TORCH_OFF))
                showCorrectText()
            }
        }
    }

    private var userName: String? = null

    private val dateTextView: TextView
    private val temperatureTextView: TextView
    private val temperatureIcon: ImageView
    private val alarmTextView: TextView
    private val alarmIcon: ImageView
    private val dndIcon: ImageView
    private val firstLine: LinearLayout
    private val secondLine: LinearLayout
    private val smartspaceText: TextView
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private val timeRunnable: Runnable
    private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val alarmTimeFormat = SimpleDateFormat("h:mm", Locale.getDefault())

    private var lastShownDate = ""
    private var lastShownAlarmTime = ""
    private var lastShownAlarmVisibility = GONE

    private val torchText = "Torch on  ·  Tap to turn off"

    val boldTextVariation = "'wght' 600, 'ROND' 100"

    init {
        userName = XPrefs.Xprefs.getString("myUserName", "");
        orientation = VERTICAL
        setPadding(0, dpToPx(24), dpToPx(24), dpToPx(24))
        firstLine = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
        }
        secondLine = LinearLayout(context).apply {
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
        temperatureIcon = ImageView(context).apply {
            visibility = GONE
        }
        val temperatureIconParams = LayoutParams(dpToPx(20), dpToPx(20)).apply {
            leftMargin = dpToPx(8)
        }
        firstLine.addView(temperatureIcon, temperatureIconParams)
        temperatureTextView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
            visibility = GONE
        }
        val temperatureTextParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dpToPx(8)
            }
        firstLine.addView(temperatureTextView, temperatureTextParams)
        alarmIcon = ImageView(context).apply {
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    XPrefs.modRes, R.drawable.ic_alarm, null
                )
            )
            visibility = GONE
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
            visibility = GONE
        }
        val alarmTextParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dpToPx(8)
            }
        firstLine.addView(alarmTextView, alarmTextParams)
        smartspaceText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = XPrefs.modRes.getFont(R.font.google_sans_flex)
            fontVariationSettings = boldTextVariation
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            text = getGreeting()
        }
        setOnClickListener {
            doCorrectAction()
        }
        secondLine.addView(smartspaceText)
        secondLine.updatePadding(top = dpToPx(8))
        addView(firstLine)
        addView(secondLine)
        updateSilentState()
        cameraManager.registerTorchCallback(torchCallback, null)
        val filter = IntentFilter(ACTION_INTERRUPTION_FILTER_CHANGED)
        context.registerReceiver(DoNotDisturbChangeReceiver(), filter)
        timeRunnable = object : Runnable {
            override fun run() {
                showCurrentDate()
                showNextAlarmIfExists()
                showCorrectText()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timeRunnable)
    }

    fun showOnlySmall() {
        firstLine.visibility = INVISIBLE
    }

    fun showAll() {
        firstLine.visibility = VISIBLE
    }

    fun setBig() {
        firstLine.gravity = Gravity.CENTER
        secondLine.visibility = GONE
        post {
            layoutParams.width = LayoutParams.MATCH_PARENT
        }
    }

    private fun doCorrectAction() {
        if (secondLine.isVisible && smartspaceText.text.toString() == torchText) {
            SystemUtils.TurnOffFlash()
        } else {
            SystemUtils.Sleep()
        }
    }

    private var currentMusic: String? = null

    fun setMusicInfo(musicInfo: String?) {
        currentMusic = musicInfo
        showCorrectText()
    }

    private fun showCorrectText() {
        if (smartspaceText.text == torchText) {
            return
        }
        if (currentMusic != null) {
            setSmartspaceText(currentMusic)
        } else {
            setSmartspaceText(getGreeting())
        }
    }

    private fun setSmartspaceText(text: String?) {
        if (smartspaceText.text.toString() != text) {
            smartspaceText.text = text
        }
    }

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 3 until 12 -> "Good morning"
            in 12 until 18 -> "Good afternoon"
            in 18 until 22 -> "Good evening"
            else -> "Good night"
        }
        return if (userName.orEmpty().isNotBlank()) {
            "$greeting, $userName"
        } else {
            greeting
        }
    }

    private fun showCurrentDate() {
        val currentDate = dateFormat.format(Date())
        if (currentDate != lastShownDate) {
            lastShownDate = currentDate
            dateTextView.text = currentDate
        }
    }

    fun setTemperature(temperature: String, leftDrawable: Drawable) {
        temperatureIcon.setImageDrawable(leftDrawable)
        temperatureTextView.text = temperature;
        temperatureIcon.visibility = VISIBLE
        temperatureTextView.visibility = VISIBLE
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

    private fun isDndEnabled(): Boolean {
        return notificationManager?.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun updateSilentState() {
        dndIcon.isVisible = isDndEnabled()
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    private inner class DoNotDisturbChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateSilentState()
        }
    }
}
