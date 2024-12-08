package sh.siava.AOSPMods.myListeners.helper

import android.animation.Animator
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout

object Helper {

    private const val NOTIF_TAG = "uvneshNotifIcons"
    const val NOTIF_TAG_SCROLL = "uvneshNotifIconsScroll"

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

    fun applyMarginToAodIcons(innerScrollLayout: View, aodIconPosition: Int) {
        val params = innerScrollLayout.layoutParams as? ViewGroup.MarginLayoutParams
        params?.topMargin = aodIconPosition
        innerScrollLayout.setLayoutParams(params)
        if (aodIconPosition == 0) {
            innerScrollLayout.visibility = View.GONE
        }
    }

    fun manageNotificationHere(
        linearLayout: LinearLayout,
        notifications: ArrayList<StatusBarNotification>,
        aodIconPosition: Int
    ) {
        if (appListItems.isEmpty() && !isLoading) {
            loadAppIcons(linearLayout.context)
        }
        if (linearLayout.findViewWithTag<View>(NOTIF_TAG_SCROLL) == null) {
            val innerLayout = LinearLayout(linearLayout.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                tag = NOTIF_TAG
            }
            val innerScrollLayout = HorizontalScrollView(linearLayout.context).apply {
                val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val marginParams = ViewGroup.MarginLayoutParams(params)
                marginParams.topMargin = aodIconPosition
                layoutParams = marginParams
                tag = NOTIF_TAG_SCROLL
                setPadding(30.px, 12.px, 30.px, 0)
                isHorizontalScrollBarEnabled = false
                clipToPadding = false
                visibility = View.GONE
            }
            linearLayout.addView(innerScrollLayout)
            innerScrollLayout.addView(innerLayout)
        }
        val innerScrollLayout = linearLayout.findViewWithTag<HorizontalScrollView>(NOTIF_TAG_SCROLL)
        val params = innerScrollLayout.layoutParams as? ViewGroup.MarginLayoutParams
        params?.topMargin = aodIconPosition
        innerScrollLayout.setLayoutParams(params)
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
        if (aodIconPosition == 0) {
            innerScrollLayout.visibility = View.GONE
        }
    }

    val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()

    fun View.animateAlpha(duration: Long = 400, isReverse: Boolean = false) {
        alpha = if (isReverse) 1f else 0f
        if (!isReverse) {
            visibility = View.VISIBLE
        }
        Handler(Looper.getMainLooper()).post {
            animate().alpha(if (isReverse) 0f else 1f)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        if (isReverse) {
                            this@animateAlpha.visibility = View.GONE
                        }
                    }
                }).duration = duration
        }
    }
}