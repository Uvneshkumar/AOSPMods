package sh.siava.AOSPMods.myListeners.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import sh.siava.AOSPMods.XPrefs

object Helper {

    // From System UI
    val below_clock_padding_start_icons = 31.px
    val bcSmartspaceViewPadding = 17.px

    private val appListItems: MutableSet<Pair<String, Drawable>> = mutableSetOf()

    private fun loadAppIcons(context: Context, postAction: () -> Unit) {
        if (appListItems.isEmpty()) {
            Handler(Looper.getMainLooper()).post {
                val appList: List<ApplicationInfo> =
                    context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in appList) {
                    val appName: String = appInfo.packageName
                    val appIcon: Drawable = context.packageManager.getApplicationIcon(appInfo)
                    appListItems.add(Pair(appName, appIcon))
                }
                postAction()
            }
        } else {
            postAction()
        }
    }

    private fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val lockChannel = NotificationChannel(
            "LOCK_CHANNEL", "Lock Channel", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = name.toString()
        }
        notificationManager?.createNotificationChannel(lockChannel)
    }

    fun playSound(context: Context) {
        createNotificationChannel(context)
        if (XPrefs.Xprefs.getBoolean(
                "enableST2SMyAodVolume", false
            ) && isRingerModeNormal(context)
        ) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = notificationManager?.getNotificationChannel("LOCK_CHANNEL")
            RingtoneManager.getRingtone(context, channel?.sound).play()
        }
    }

    private fun isRingerModeNormal(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerMode = audioManager?.ringerMode
        return ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    fun setNotificationIcon(
        imageView: ImageView,
        statusBarNotification: StatusBarNotification,
        isFirstTry: Boolean = true
    ) {
        val iconScaleFactor = 1.2f
        val rootScaleFactor = 1.3f
        val differenceInRootScale = if (Build.PRODUCT == "flame") 0.1f else 0f
        loadAppIcons(imageView.context) {
            val iconDrawable =
                appListItems.find { it.first == statusBarNotification.packageName }?.second
            if (iconDrawable == null && isFirstTry) {
                appListItems.clear()
                setNotificationIcon(imageView, statusBarNotification, false)
                return@loadAppIcons
            }
            imageView.apply {
                post {
                    scaleX = iconScaleFactor
                    scaleY = iconScaleFactor
                    setImageDrawable(iconDrawable)
                }
            }
            val aodIconsCenterMargin: Float =
                XPrefs.Xprefs.getString("aodIconsCenterMargin", "-2").orEmpty().ifEmpty { "-2" }
                    .toFloat()
            (imageView.parent as? View)?.apply {
                scaleX = rootScaleFactor + differenceInRootScale
                scaleY = rootScaleFactor + differenceInRootScale
                setPadding(
                    ((below_clock_padding_start_icons * (iconScaleFactor + rootScaleFactor + (differenceInRootScale * 4))).toInt() + aodIconsCenterMargin).toInt(),
                    paddingTop,
                    paddingRight,
                    paddingBottom
                )
            }
        }
    }

    fun addView(rootView: FrameLayout, statusBarHeight: Int): View {
        while (rootView.findViewWithTag<View>("uvneshST2S") != null) {
            rootView.removeView(rootView.findViewWithTag("uvneshST2S"))
        }
        val innerFrame = FrameLayout(rootView.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                rootView.width, rootView.height
            )
            y = (-statusBarHeight).toFloat()
            tag = "uvneshST2S"
        }
        rootView.addView(innerFrame)
        return innerFrame
    }

    fun createOvalDrawable(): ShapeDrawable {
        val size =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, getSystem().displayMetrics)
                .toInt()
        val circleDrawable = ShapeDrawable(OvalShape()).apply {
            paint.color = Color.WHITE
            paint.strokeWidth = 6f
            paint.style = Paint.Style.STROKE
            setIntrinsicWidth(size)
            setIntrinsicHeight(size)
        }
        return circleDrawable
    }

    fun animateAppear(view: View) {
        val viewGroup = view as? ViewGroup
        val isLargeClock =
            viewGroup?.getChildAt(viewGroup.childCount - 1)?.visibility == View.VISIBLE
        view.translationY = if (isLargeClock) -40f else -20f
        view.alpha = 0.00001f
        view.animate().alpha(0.99999f).translationY(0f).setDuration(500).start()
    }

    fun animateAlpha(view: View) {
        view.alpha = 0.00001f
        view.animate().alpha(0.99999f).setDuration(500).start()
    }

    fun animateAlphaReverse(view: View) {
        view.animate().alpha(0f).setDuration(100).start()
    }

    val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
}