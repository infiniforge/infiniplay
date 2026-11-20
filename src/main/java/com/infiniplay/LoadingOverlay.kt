package com.infiniplay

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/** A separate modal window covers even Folder's SurfaceView with setZOrderOnTop(true). */
internal class LoadingOverlay(private val activity: Activity) {
    private val dialog = Dialog(activity)
    private val card = LinearLayout(activity)
    private val title = TextView(activity)
    private val spinner = ProgressBar(activity)

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val cover =
            FrameLayout(activity).apply {
                isClickable = true
                isFocusable = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        card.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(24))
        }
        spinner.apply {
            isIndeterminate = true
            contentDescription = "Loading"
        }
        title.apply {
            textSize = 19f
            gravity = Gravity.CENTER
            maxWidth = dp(280)
            setPadding(0, dp(20), 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        card.addView(spinner, LinearLayout.LayoutParams(dp(64), dp(64)))
        card.addView(title, LinearLayout.LayoutParams(-2, -2))
        cover.addView(card, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        dialog.setContentView(cover)
    }

    fun show(label: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        val dark =
            when (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> Color.luminance(PlayUi.background) < .5f
            }
        title.text = label
        title.setTextColor(if (dark) Color.WHITE else 0xff18242c.toInt())
        spinner.indeterminateTintList =
            ColorStateList.valueOf(if (dark) PlayUi.accent else 0xff087d70.toInt())
        card.background =
            GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(if (dark) 0xff24313c.toInt() else 0xfff6f8fa.toInt())
            }
        if (!dialog.isShowing) dialog.show()
        dialog.window?.apply {
            // The scrim alone supplies 50% opacity; do not add Android's default dim layer.
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
            setBackgroundDrawable(
                ColorDrawable(if (dark) 0x80000000.toInt() else 0x80ffffff.toInt())
            )
            decorView.setPadding(0, 0, 0, 0)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
        }
    }

    fun hide() {
        if (dialog.isShowing) dialog.dismiss()
    }
}
