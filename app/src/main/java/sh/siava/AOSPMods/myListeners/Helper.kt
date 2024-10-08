package sh.siava.AOSPMods.myListeners

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

object Helper {

    fun addView(x: Float, y: Float, rootView: FrameLayout): View {
        while (rootView.findViewWithTag<View>("uvneshST2S") != null) {
            rootView.removeView(rootView.findViewWithTag("uvneshST2S"))
        }
        val innerFrame = FrameLayout(rootView.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            tag = "uvneshST2S"
        }
        val size = rootView.width
        val revealView = View(rootView.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                size, size
            ).apply {
                // To Start Reveal from touch point
//                leftMargin = (x - (size / 2)).toInt()
//                topMargin = (y - (size / 2)).toInt()
//                leftMargin = (rootView.width - size) / 2
                topMargin = Resources.getSystem().displayMetrics.heightPixels * 4
            }
        }
        rootView.addView(innerFrame)
        innerFrame.addView(revealView)
        return revealView
    }
}