package com.infiniplay

import android.app.*
import android.content.res.ColorStateList
import android.content.pm.ActivityInfo
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.*
import android.widget.*
import com.infiniplay.core.*

object PlayUi {
    const val background = 0xff111922.toInt()
    const val text = 0xffd8e6ee.toInt()
    const val accent = 0xff65dbca.toInt()

    fun label(context: Activity, text: String, size: Float = 14f) =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(PlayUi.text)
            setPadding(12, 8, 12, 8)
        }

    fun button(context: Activity, text: String, action: () -> Unit) =
        Button(context).apply {
            this.text = text
            isAllCaps = false
            minHeight = (48 * resources.displayMetrics.density).toInt()
            setOnClickListener { action() }
        }

    fun sessionMenuButton(context: Activity, action: () -> Unit) =
        TextView(context).apply {
            text = "☰"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(0xffd8e6ee.toInt())
            contentDescription = "Pause and settings"
            isClickable = true
            isFocusable = true
            val density = resources.displayMetrics.density
            val shape =
                GradientDrawable().apply {
                    setColor(0xff0b1017.toInt())
                    cornerRadius = 24 * density
                    setStroke(density.toInt().coerceAtLeast(1), 0x66899aa6)
                }
            background = RippleDrawable(
                ColorStateList.valueOf(0x3365dbca),
                InsetDrawable(shape, (6 * density).toInt()),
                null,
            )
            alpha = .62f
            setOnClickListener { action() }
        }

    fun orientation(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Screen orientation")
            .setItems(arrayOf("Follow device rotation", "Portrait", "Landscape")) { _, n ->
                activity.requestedOrientation =
                    when (n) {
                        1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        2 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    }
                PlayStore(activity).orientation(activity.requestedOrientation)
            }
            .show()
    }

    fun controllerDialog(activity: Activity, router: GamepadRouter) {
        val devices = router.devices()
        if (devices.isEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle("Controllers")
                .setMessage(
                    "Connect a USB HID gamepad or joystick. Android gamepad buttons, D-pad, sticks and triggers are supported. Bluetooth gamepads use the same input path."
                )
                .setPositiveButton("Done", null)
                .show()
            return
        }
        AlertDialog.Builder(activity)
            .setTitle("Controllers")
            .setItems(devices.map { "${it.name} · Player ${router.port(it)+1}" }.toTypedArray()) {
                _,
                i ->
                val d = devices[i]
                AlertDialog.Builder(activity)
                    .setTitle(d.name)
                    .setSingleChoiceItems(
                        arrayOf("Player 1", "Player 2", "Player 3", "Player 4"),
                        router.port(d),
                    ) { dialog, n ->
                        router.assign(d, n)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton("Done", null)
            .show()
    }

    fun controlSettings(activity: Activity, pad: PadView) {
        val c =
            pad.selected()
                ?: run {
                    Toast.makeText(
                            activity,
                            "Tap a control first, then open Size / position",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    return
                }
        val box =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
            }
        fun slider(title: String, value: Int, max: Int, update: (Int) -> Unit) {
            val label = label(activity, "$title: $value")
            box.addView(label)
            box.addView(
                SeekBar(activity).apply {
                    this.max = max
                    progress = value
                    setOnSeekBarChangeListener(
                        object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                                if (user) {
                                    label.text = "$title: $p"
                                    update(p)
                                }
                            }

                            override fun onStartTrackingTouch(s: SeekBar?) {}

                            override fun onStopTrackingTouch(s: SeekBar?) {}
                        }
                    )
                }
            )
        }
        slider("Size (%)", (c.size() * 100).toInt(), 60) {
            pad.selected()?.let { c -> pad.replace(c.resize(maxOf(4, it) / 100.0)) }
        }
        slider("Horizontal position (%)", (c.x() * 100).toInt(), 100) {
            pad.selected()?.let { c -> pad.replace(c.move(it / 100.0, c.y())) }
        }
        slider("Vertical position (%)", (c.y() * 100).toInt(), 100) {
            pad.selected()?.let { c -> pad.replace(c.move(c.x(), it / 100.0)) }
        }
        AlertDialog.Builder(activity)
            .setTitle(c.label())
            .setView(box)
            .setPositiveButton("Done", null)
            .show()
    }

    fun gapDialog(activity: Activity, pad: PadView, store: PlayStore, refresh: () -> Unit) {
        val box =
            EditText(activity).apply {
                inputType =
                    android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(store.gap(pad.system).toString())
            }
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle("Hinge gap · native screen pixels")
                .setMessage(
                    "The gap scales with the screens in both orientations. DS defaults to 64 native pixels. Calibrate the 3DS gap for your hardware variant (0–300). Screen widths retain their original proportions."
                )
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = box.text.toString().toDoubleOrNull()
                if (n == null || !n.isFinite() || n !in 0.0..300.0) {
                    box.error = "Enter 0–300"
                } else {
                    store.gap(pad.system, n)
                    refresh()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
