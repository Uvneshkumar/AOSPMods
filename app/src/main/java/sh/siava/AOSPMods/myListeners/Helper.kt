package sh.siava.AOSPMods.myListeners

import android.view.View
import android.widget.FrameLayout

object Helper {

    fun addView(x: Float, y: Float, rootView: FrameLayout, statusBarHeight: Int): View {
        while (rootView.findViewWithTag<View>("uvneshST2S") != null) {
            rootView.removeView(rootView.findViewWithTag("uvneshST2S"))
        }
        val innerFrame = FrameLayout(rootView.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                rootView.width, rootView.height
            )
            setY((-statusBarHeight).toFloat())
            tag = "uvneshST2S"
//            setBackgroundColor(ContextCompat.getColor(rootView.context, android.R.color.black))
//            alpha = 0f
        }
        val size = rootView.width
        val revealView = View(rootView.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                size, size
            ).apply {
                // To Start Reveal from touch point
//                leftMargin = (x - (size / 2)).toInt()
//                topMargin = (y - (size / 2)).toInt()
                leftMargin = (rootView.width - size) / 2
                topMargin = (rootView.height - size) / 2
            }
        }
        rootView.addView(innerFrame)
        innerFrame.addView(revealView)
        return revealView
    }
}