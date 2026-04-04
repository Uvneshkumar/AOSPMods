package sh.siava.AOSPMods.myListeners.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import sh.siava.AOSPMods.XPrefs
import sh.siava.AOSPMods.utils.Helpers.myLog

object Helper {

    // From System UI
    val below_clock_padding_start_icons = 31.px
    val bcSmartspaceViewPadding = 17.px

    private val appListItems: MutableMap<String, Drawable> = mutableMapOf()

    val widthPixels = getSystem().displayMetrics.widthPixels
    val heightPixels = getSystem().displayMetrics.heightPixels

    var lastTapX: Float = widthPixels / 2f
    var lastTapY: Float = heightPixels * 1.05f

    fun resetTapPosition() {
        // Bottom Center - Charging Port Location usually
        lastTapX = widthPixels / 2f
        lastTapY = heightPixels * 1.05f
    }

    @Suppress("LocalVariableName", "UNCHECKED_CAST")
    fun getPlatformPermissionsOfGroup(
        platformPermissionGroups: Any, pm: PackageManager, group: String
    ): List<PermissionInfo> {
        val PLATFORM_PERMISSION_GROUPS: MutableMap<String, MutableList<String>> =
            platformPermissionGroups as MutableMap<String, MutableList<String>>
        val permInfos = mutableListOf<PermissionInfo>()
        for (permName in PLATFORM_PERMISSION_GROUPS[group] ?: emptyList()) {
            try {
                val permInfo: PermissionInfo = pm.getPermissionInfo(permName, 0)
                permInfos.add(permInfo)
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        }
        return permInfos
    }

    private fun loadAppIcons(context: Context, postAction: () -> Unit) {
        if (appListItems.isEmpty()) {
            Thread {
                val tempMap = mutableMapOf<String, Drawable>()
                val appList: List<ApplicationInfo> =
                    context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in appList) {
                    val appName: String = appInfo.packageName
                    val appIcon: Drawable = context.packageManager.getApplicationIcon(appInfo)
                    tempMap[appName] = appIcon
                }
                Handler(Looper.getMainLooper()).post {
                    appListItems.putAll(tempMap)
                    postAction()
                }
            }.start()
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
        isFirstTry: Boolean = true,
        isFromOneUi: Boolean = false
    ) {
        val iconScaleFactor = 1.2f
        val rootScaleFactor = 1.3f
        loadAppIcons(imageView.context) {
            val iconDrawable = appListItems[statusBarNotification.packageName]
            if (iconDrawable == null && isFirstTry) {
                setNotificationIcon(imageView, statusBarNotification, false, isFromOneUi)
                return@loadAppIcons
            }
            (imageView.parent as? View)?.apply {
                if (!isFromOneUi) {
                    val aodIconsCenterMargin: Float =
                        XPrefs.Xprefs.getString("aodIconsCenterMargin", "-2").orEmpty()
                            .ifEmpty { "-2" }.toFloat()
                    scaleX = rootScaleFactor
                    scaleY = rootScaleFactor
                    setPadding(
                        ((below_clock_padding_start_icons * (iconScaleFactor + rootScaleFactor)) + aodIconsCenterMargin).toInt(),
                        paddingTop,
                        paddingRight,
                        paddingBottom
                    )
                }
                imageView.apply {
                    setImageDrawable(iconDrawable)
                    if (!isFromOneUi) {
                        viewTreeObserver.addOnDrawListener {
                            scaleX = iconScaleFactor
                            scaleY = iconScaleFactor
                        }
                    }
                }
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

    fun animateAlpha(view: View, duration: Long) {
        view.alpha = 0.00001f
        view.animate().alpha(0.99999f).setDuration(duration).start()
    }

    fun animateAlpha(view: View) {
        animateAlpha(view, 500)
    }

    fun animateAlphaHandle(view: View, runnable: Runnable) {
        val duration = 200L
        view.alpha = 0f
        view.animate().alpha(0.99999f).setDuration(duration).start()
        Handler(Looper.getMainLooper()).postDelayed(runnable, duration)
    }

    fun animateAlphaReverse(view: View) {
        view.animate().alpha(0f).setDuration(100).start()
    }

    private fun logViewDetails(view: View, indent: String) {
        myLog("$indent- ${view::class.java.simpleName} (id: ${view.id}) ${view.layoutParams}")
    }

    private fun getAllViews(view: View, indent: String = ""): List<View> {
        val result = mutableListOf<View>()
        if (view is ViewGroup) {
            result.add(view)
            logViewDetails(view, indent)
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                result.addAll(getAllViews(child, "$indent "))
            }
        } else {
            logViewDetails(view, indent)
            result.add(view)
        }
        return result
    }

    fun debugView(view: View) {
        getAllViews(view)
    }

    val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
}