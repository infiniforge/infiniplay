package com.infiniplay

import android.content.Context
import android.graphics.*
import android.view.*
import com.infiniplay.core.*
import kotlin.math.*

class PadView(context: Context) : View(context) {
    var system = Console.NES
    var controls: List<PadLayout.Control> = emptyList()
    var screens: List<ScreenLayout.Screen> = emptyList()
    var preview = true
    var editing = false
    var selectedId: String? = null
    var pressed: Set<Int> = emptySet()
    var onKeys: (String, Set<Int>) -> Unit = { _, _ -> }
    var onAnalog: (Float, Float) -> Unit = { _, _ -> }
    var onStylus: (Float, Float) -> Unit = { _, _ -> }
    var onChanged: () -> Unit = {}
    var onSelected: () -> Unit = {}
    var onGeometry: () -> Unit = {}
    private val fingers = mutableMapOf<Int, String>()
    private var stylus: Int? = null
    private var dragX = 0f
    private var dragY = 0f
    private var pinchSpan = 0.0
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

    private fun rect(c: PadLayout.Control): RectF {
        val size = c.size().toFloat() * min(width, height)
        val w = if (c.kind() == PadLayout.Kind.PILL) size * 1.6f else size
        val h = if (c.kind() == PadLayout.Kind.PILL) size * .65f else size
        val x = (c.x() * width).toFloat().coerceIn(w / 2, max(w / 2, width - w / 2))
        val y = (c.y() * height).toFloat().coerceIn(h / 2, max(h / 2, height - h / 2))
        return RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2)
    }

    fun selected() = controls.firstOrNull { it.id() == selectedId }

    fun replace(c: PadLayout.Control) {
        controls = controls.map { if (it.id() == c.id()) c else it }
        invalidate()
        onChanged()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        release()
        onGeometry()
    }

    override fun onDraw(canvas: Canvas) {
        if (preview) canvas.drawColor(0xff111922.toInt())
        if (preview)
            for ((index, screen) in screens.withIndex()) {
                val d = screen.destination()
                val r =
                    RectF(
                        d.left().toFloat(),
                        d.top().toFloat(),
                        d.right().toFloat(),
                        d.bottom().toFloat(),
                    )
                paint.color = if (index == 0) 0xff1d3442.toInt() else 0xff253d48.toInt()
                canvas.drawRect(r, paint)
                paint.color = 0xffa7c9cf.toInt()
                paint.textSize = 14 * resources.displayMetrics.scaledDensity
                canvas.drawText(
                    if (system.dual()) if (index == 0) "Upper screen" else "Touch screen"
                    else system.title,
                    r.centerX(),
                    r.centerY(),
                    paint,
                )
            }
        for (c in controls) {
            val r = rect(c)
            val active =
                if (c.kind() == PadLayout.Kind.DPAD) {
                    val keys =
                        if (c.key() == 2) setOf(104, 105, 106, 107) else setOf(19, 20, 21, 22)
                    pressed.any { it in keys }
                } else c.key() != 0 && c.key() in pressed
            paint.style = Paint.Style.FILL
            paint.color = if (active) 0xff87e7d5.toInt() else ControllerTheme.of(system).shell()
            canvas.drawRoundRect(
                RectF(r.left - 3, r.top - 3, r.right + 3, r.bottom + 3),
                r.height() / 2,
                r.height() / 2,
                paint,
            )
            paint.color = if (active) 0xff569f96.toInt() else c.color()
            when (c.kind()) {
                PadLayout.Kind.DPAD -> {
                    if (c.label() == "Joystick") {
                        paint.color = 0xff222427.toInt()
                        canvas.drawRoundRect(r, 8f, 8f, paint)
                        paint.color = 0xff43474d.toInt()
                        canvas.drawCircle(r.centerX(), r.centerY(), r.width() * .34f, paint)
                        paint.color = 0xff14171a.toInt()
                        canvas.drawCircle(r.centerX(), r.centerY(), r.width() * .19f, paint)
                    } else {
                        val p = Path()
                        val a = r.width() / 3
                        val x = r.left
                        val y = r.top
                        p.moveTo(x + a, y)
                        p.lineTo(x + 2 * a, y)
                        p.lineTo(x + 2 * a, y + a)
                        p.lineTo(x + 3 * a, y + a)
                        p.lineTo(x + 3 * a, y + 2 * a)
                        p.lineTo(x + 2 * a, y + 2 * a)
                        p.lineTo(x + 2 * a, y + 3 * a)
                        p.lineTo(x + a, y + 3 * a)
                        p.lineTo(x + a, y + 2 * a)
                        p.lineTo(x, y + 2 * a)
                        p.lineTo(x, y + a)
                        p.lineTo(x + a, y + a)
                        p.close()
                        canvas.drawPath(p, paint)
                        paint.color = 0xff8792a0.toInt()
                        paint.textSize = r.width() * .18f
                        canvas.drawText("▲", r.centerX(), r.top + a * .72f, paint)
                        canvas.drawText("▼", r.centerX(), r.bottom - a * .2f, paint)
                    }
                }
                PadLayout.Kind.STICK -> {
                    paint.color = 0xff3c4652.toInt()
                    canvas.drawCircle(r.centerX(), r.centerY(), r.width() / 2, paint)
                    paint.color = 0xffb1b8c2.toInt()
                    canvas.drawCircle(r.centerX(), r.centerY(), r.width() * .30f, paint)
                }
                else -> {
                    val ps = system == Console.PSX || system == Console.PSP
                    paint.color =
                        if (ps && c.kind() == PadLayout.Kind.BUTTON) 0xff303743.toInt()
                        else paint.color
                    canvas.drawRoundRect(
                        r,
                        if (system == Console.MASTERSYSTEM) 5f else r.height() / 2,
                        if (system == Console.MASTERSYSTEM) 5f else r.height() / 2,
                        paint,
                    )
                    paint.color =
                        if (ps && c.kind() == PadLayout.Kind.BUTTON) c.color() else Color.WHITE
                    paint.textSize =
                        r.height() * (if (c.kind() == PadLayout.Kind.PILL) .40f else .46f)
                    canvas.drawText(
                        c.label(),
                        r.centerX(),
                        r.centerY() + paint.textSize * .34f,
                        paint,
                    )
                }
            }
            if (editing && c.id() == selectedId) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3 * resources.displayMetrics.density
                paint.color = 0xff65dbca.toInt()
                canvas.drawRect(RectF(r.left - 5, r.top - 5, r.right + 5, r.bottom + 5), paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun hit(x: Float, y: Float) =
        controls.asReversed().firstOrNull { rect(it).contains(x, y) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        if (editing) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    release()
                    selectedId = hit(event.x, event.y)?.id()
                    dragX = event.x
                    dragY = event.y
                    onSelected()
                    invalidate()
                }
                MotionEvent.ACTION_POINTER_DOWN ->
                    if (event.pointerCount >= 2)
                        pinchSpan =
                            hypot(
                                (event.getX(0) - event.getX(1)).toDouble(),
                                (event.getY(0) - event.getY(1)).toDouble(),
                            )
                MotionEvent.ACTION_MOVE -> {
                    val c = selected()
                    if (c != null) {
                        if (event.pointerCount >= 2) {
                            val span =
                                hypot(
                                    (event.getX(0) - event.getX(1)).toDouble(),
                                    (event.getY(0) - event.getY(1)).toDouble(),
                                )
                            if (pinchSpan > 4 && span > 4)
                                replace(c.resize(c.size() * span / pinchSpan))
                            pinchSpan = span
                        } else {
                            replace(
                                c.move(
                                    c.x() + (event.x - dragX) / max(1, width),
                                    c.y() + (event.y - dragY) / max(1, height),
                                )
                            )
                            dragX = event.x
                            dragY = event.y
                        }
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    pinchSpan = 0.0
                    val i = if (event.actionIndex == 0) 1 else 0
                    if (i < event.pointerCount) {
                        dragX = event.getX(i)
                        dragY = event.getY(i)
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    pinchSpan = 0.0
                    onChanged()
                    performClick()
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                val id = event.getPointerId(i)
                val c = hit(event.getX(i), event.getY(i))
                if (c != null) fingers[id] = c.id()
                else if (
                    stylus == null &&
                        screens.isNotEmpty() &&
                        screens
                            .last()
                            .destination()
                            .contains(event.getX(i).toDouble(), event.getY(i).toDouble())
                )
                    stylus = id
            }
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP -> {
                val id = event.getPointerId(event.actionIndex)
                fingers.remove(id)
                onKeys("finger:$id", emptySet())
                if (stylus == id) {
                    stylus = null
                    onStylus(-10f, -10f)
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    release()
                    performClick()
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                release()
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        var analog = false
        for (i in 0 until event.pointerCount) {
            if (
                (event.actionMasked == MotionEvent.ACTION_POINTER_UP ||
                    event.actionMasked == MotionEvent.ACTION_UP) && i == event.actionIndex
            )
                continue
            val id = event.getPointerId(i)
            val c = controls.firstOrNull { it.id() == fingers[id] }
            if (c != null) {
                val r = rect(c)
                val x = (event.getX(i) - r.centerX()) / (r.width() / 2)
                val y = (event.getY(i) - r.centerY()) / (r.height() / 2)
                when (c.kind()) {
                    PadLayout.Kind.STICK -> {
                        onAnalog(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
                        analog = true
                    }
                    PadLayout.Kind.DPAD -> {
                        val keys = mutableSetOf<Int>()
                        if (x < -.25) keys += if (c.key() == 2) 105 else 21
                        if (x > .25) keys += if (c.key() == 2) 107 else 22
                        if (y < -.25) keys += if (c.key() == 2) 104 else 19
                        if (y > .25) keys += if (c.key() == 2) 106 else 20
                        onKeys("finger:$id", keys)
                    }
                    else ->
                        onKeys(
                            "finger:$id",
                            if (r.contains(event.getX(i), event.getY(i))) setOf(c.key())
                            else emptySet(),
                        )
                }
            }
            if (stylus == id) {
                val p =
                    ScreenLayout.touch(screens, event.getX(i).toDouble(), event.getY(i).toDouble())
                onStylus(p[0].toFloat(), p[1].toFloat())
            }
        }
        if (!analog) onAnalog(0f, 0f)
        return true
    }

    fun release() {
        for (id in fingers.keys) onKeys("finger:$id", emptySet())
        fingers.clear()
        stylus = null
        onAnalog(0f, 0f)
        onStylus(-10f, -10f)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
