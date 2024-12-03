package sh.siava.AOSPMods.myListeners.helper

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

object Helper {

    private const val NOTIF_TAG = "uvneshNotifIcons"

    private val appListItems: MutableSet<Pair<String, Drawable>> = mutableSetOf()
    private var isLoading = false

    fun loadAppIcons(context: Context) {
        if (isLoading) return
        isLoading = true
        appListItems.clear()
        Handler(Looper.getMainLooper()).post {
            val appList: List<ApplicationInfo> =
                context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in appList) {
                val appName: String = appInfo.packageName
                val appIcon: Drawable = context.packageManager.getApplicationIcon(appInfo)
                appListItems.add(Pair(appName, appIcon))
            }
            isLoading = false
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

    fun manageNotificationHere(
        linearLayout: LinearLayout, notifications: ArrayList<StatusBarNotification>
    ) {
        if (appListItems.isEmpty() && !isLoading) {
            loadAppIcons(linearLayout.context)
        }
        if (linearLayout.findViewWithTag<View>(NOTIF_TAG) == null) {
            val innerLayout = LinearLayout(linearLayout.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tag = NOTIF_TAG
                setPadding(30.px, 12.px, 30.px, 0)
            }
            linearLayout.addView(innerLayout)
        }
        val notificationSmall = linearLayout.findViewWithTag<LinearLayout>(NOTIF_TAG)
        notificationSmall.removeAllViews()
        notifications.forEach { notification ->
            val iconDrawable = appListItems.find { it.first == notification.packageName }?.second
            val notificationIcon = ImageView(notificationSmall.context).apply {
                post {
                    setPadding(0, 5.px, 5.px, 5.px)
                    layoutParams.height = 36.px
                    layoutParams.width = 36.px
                    requestLayout()
                    setImageDrawable(iconDrawable)
                }
            }
            notificationSmall.addView(notificationIcon)
        }
    }
}

val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
