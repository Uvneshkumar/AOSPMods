package sh.siava.AOSPMods.myListeners

import android.view.View
import android.widget.FrameLayout

object Helper {

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
}