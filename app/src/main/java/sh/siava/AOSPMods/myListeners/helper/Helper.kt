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

object Helper {

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

    fun setNotificationIcon(
        imageView: ImageView,
        statusBarNotification: StatusBarNotification,
        isFirstTry: Boolean = true
    ) {
        val iconScaleFactor = 1.2f
        val rootScaleFactor = 1.3f
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
            (imageView.parent as? View)?.apply {
                scaleX = rootScaleFactor
                scaleY = rootScaleFactor
                // From System UI
                val below_clock_padding_start_icons = 31.px
                setPadding(
                    (below_clock_padding_start_icons * (iconScaleFactor + rootScaleFactor)).toInt() + 3,
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

    val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()
}