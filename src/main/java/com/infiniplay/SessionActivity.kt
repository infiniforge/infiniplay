package com.infiniplay

import android.app.*
import android.content.res.Configuration
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.lifecycle.*
import com.infiniplay.core.*
import com.swordfish.libretrodroid.*
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlinx.coroutines.launch

class SessionActivity : Activity(), LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = registry

    private lateinit var retro: GLRetroView
    private lateinit var pad: PadView
    private lateinit var store: PlayStore
    private lateinit var game: PlayStore.Game
    private lateinit var core: CoreCatalog.Core
    private lateinit var router: GamepadRouter
    private val digital = Array(4) { DigitalInput() }

    private data class Axis(val port: Int, val stick: Int, val x: Float, val y: Float)

    private val axes = mutableMapOf<String, Axis>()
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var loadingOverlay: LoadingOverlay
    private var starting = true
    private var ready = false
    private var active = false
    private var menu = false
    private var closing = false
    private lateinit var mediaRow: LinearLayout
    private lateinit var eject: Button
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var mediaText: TextView
    private var media = intArrayOf(0, 0, 0, 0)
    private var labels = emptyList<String>()
    private var frames = 0
    private val stateFile
        get() = File(filesDir, "saves/${game.id}/${core.id()}/slot.state")

    private val ramFile
        get() = File(filesDir, "saves/${game.id}/${core.id()}/save.srm")

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        store = PlayStore(this)
        game =
            store.games().firstOrNull { it.id == intent.getStringExtra("game") }
                ?: run {
                    finish()
                    return
                }
        core =
            runCatching { CoreCatalog.get(intent.getStringExtra("core") ?: "") }
                .getOrElse {
                    finish()
                    return
                }
        if (
            core.system() != game.system ||
                Firmware.missing(core.id(), File(filesDir, "firmware/${core.id()}")).isNotEmpty()
        ) {
            finish()
            return
        }
        requestedOrientation = store.orientation()
        val path =
            if (core.bundled()) File(applicationInfo.nativeLibraryDir, core.file()).absolutePath
            else
                store.corePath(core.id())
                    ?: run {
                        finish()
                        return
                    }
        val data =
            GLRetroViewData(this).apply {
                coreFilePath = path
                gameFilePath = File(filesDir, "games/${game.file}").absolutePath
                systemDirectory =
                    File(filesDir, "firmware/${core.id()}").apply { mkdirs() }.absolutePath
                savesDirectory = ramFile.parentFile!!.apply { mkdirs() }.absolutePath
                saveRAMState =
                    runCatching {
                            ramFile
                                .takeIf { it.isFile && it.length() in 1..32 * 1024 * 1024 }
                                ?.readBytes()
                        }
                        .getOrNull()
                val options = CoreSettings.defaults(core.id()).toMutableMap()
                if (game.system == Console.VIRTUALBOY)
                    options["vb_anaglyph_preset"] = store.virtualBoyDisplay()
                variables = options.map { Variable(it.key, it.value) }.toTypedArray()
            }
        labels =
            runCatching {
                    val entry = File(filesDir, "games/${game.file}")
                    if (entry.extension.equals("m3u", true) && entry.length() <= 1024 * 1024)
                        MediaManifest.references(entry.name, entry.readText()).map {
                            it.substringAfterLast('/')
                        }
                    else emptyList()
                }
                .getOrDefault(emptyList())
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xff0b1017.toInt())
                fitsSystemWindows = true
            }
        setContentView(box)
        loadingOverlay = LoadingOverlay(this)
        val toolbar = LinearLayout(this)
        box.addView(toolbar)
        toolbar.addView(
            PlayUi.button(this, "☰  Pause / settings") { openMenu() },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        toolbar.addView(PlayUi.label(this, game.name), LinearLayout.LayoutParams(0, -2, 1f))
        val area = FrameLayout(this)
        box.addView(area, LinearLayout.LayoutParams(-1, 0, 1f))
        retro = GLRetroView(this, data)
        area.addView(retro, FrameLayout.LayoutParams(-1, -1))
        pad =
            PadView(this).apply {
                system = game.system
                preview = false
                onGeometry = { geometry() }
                onKeys = { s, k -> keys("touch:$s", 0, k) }
                onAnalog = { x, y ->
                    analog("touch:stick", 0, GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y)
                }
                onStylus = { x, y ->
                    if (ready) retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_POINTER, x, y)
                }
                onChanged = { store.saveLayout(game.system, portrait(), controls) }
            }
        area.addView(pad, FrameLayout.LayoutParams(-1, -1))
        mediaRow =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
            }
        val theme = ControllerTheme.of(game.system)
        fun mediaButton(label: String, action: () -> Unit) =
            PlayUi.button(this, label, action).apply {
                setTextColor(
                    when (game.system) {
                        Console.FDS -> 0xff681e22.toInt()
                        Console.PCECD -> 0xff343238.toInt()
                        else -> theme.ink()
                    }
                )
                background =
                    GradientDrawable().apply {
                        setColor(
                            when (game.system) {
                                Console.FDS -> 0xffd6bc78.toInt()
                                Console.PCECD -> theme.shell()
                                else -> theme.cap()
                            }
                        )
                        cornerRadius =
                            (if (theme.roundMedia()) 48f else 5f) * resources.displayMetrics.density
                        setStroke((2 * resources.displayMetrics.density).toInt(), theme.shell())
                    }
                setPadding(12, 8, 12, 8)
            }
        eject = mediaButton(theme.mediaLabel()) { mediaCommand(2, 0) }
        previous =
            mediaButton("◀") { mediaCommand(3, (media[1] - 1 + media[0]) % maxOf(1, media[0])) }
        next = mediaButton("▶") { mediaCommand(3, (media[1] + 1) % maxOf(1, media[0])) }
        mediaText =
            PlayUi.label(this, "").apply {
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        mediaRow.addView(eject)
        mediaRow.addView(previous)
        mediaRow.addView(mediaText, LinearLayout.LayoutParams(0, -2, 1f))
        mediaRow.addView(next)
        box.addView(mediaRow)
        router =
            GamepadRouter(
                this,
                { s, p, k -> keys(s, p, k) },
                { s, p, a, x, y -> analog(s, p, a, x, y) },
                { clear(it) },
                {},
            )
        lifecycleScope.launch {
            retro.getGLRetroEvents().collect { event ->
                when (event) {
                    is GLRetroView.GLRetroEvents.SurfaceCreated -> {
                        ready = true
                        retro.queueEvent {
                            for (p in 0..3) {
                                NativeScreens.key(p, -1, false)
                                if (game.system == Console.ATARI5200)
                                    retro.setControllerType(p, 769)
                            }
                        }
                        geometry()
                        mediaCommand(0, 0)
                    }
                    is GLRetroView.GLRetroEvents.FrameRendered -> {
                        if (starting) {
                            starting = false
                            loadingOverlay.hide()
                        }
                        if (++frames % 120 == 0) mediaCommand(0, 0)
                    }
                }
            }
        }
        lifecycleScope.launch {
            retro.getGLRetroErrors().collect { code ->
                if (!closing) {
                    ready = false
                    starting = false
                    loadingOverlay.hide()
                    AlertDialog.Builder(this@SessionActivity)
                        .setTitle("Core could not run")
                        .setMessage(
                            "${core.title()} reported error $code. Check that this core, game and firmware are compatible. Your source files are unchanged."
                        )
                        .setPositiveButton("Return to library") { _, _ -> finish() }
                        .setOnCancelListener { finish() }
                        .show()
                }
            }
        }
        registry.currentState = Lifecycle.State.CREATED
        registry.addObserver(retro)
    }

    private fun portrait() =
        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    private fun geometry() {
        if (!::pad.isInitialized || pad.width <= 0 || pad.height <= 0) return
        pad.release()
        pad.controls = store.layouts(game.system, portrait())
        pad.screens =
            ScreenLayout.fit(
                game.system,
                pad.width.toDouble(),
                pad.height.toDouble(),
                portrait(),
                store.gap(game.system),
            )
        pad.invalidate()
        if (!ready) return
        val screens = pad.screens
        val w = pad.width.toFloat()
        val h = pad.height.toFloat()
        if (game.system.dual()) {
            val values =
                screens
                    .flatMap { s ->
                        val r = s.source()
                        val d = s.destination()
                        listOf(
                            r.left().toFloat(),
                            r.top().toFloat(),
                            r.right().toFloat(),
                            r.bottom().toFloat(),
                            d.left().toFloat() / w,
                            d.top().toFloat() / h,
                            d.right().toFloat() / w,
                            d.bottom().toFloat() / h,
                        )
                    }
                    .toFloatArray()
            retro.queueEvent {
                retro.viewport = RectF(0f, 0f, 1f, 1f)
                NativeScreens.set(values)
            }
        } else {
            val d =
                if (game.system in setOf(Console.WONDERSWAN, Console.WONDERSWANCOLOR))
                    ScreenLayout.availableArea(w.toDouble(), h.toDouble(), portrait())
                else screens.single().destination()
            retro.queueEvent {
                NativeScreens.set(null)
                retro.viewport =
                    RectF(
                        d.left().toFloat() / w,
                        d.top().toFloat() / h,
                        d.right().toFloat() / w,
                        d.bottom().toFloat() / h,
                    )
            }
        }
    }

    private fun applyChanges(port: Int, changes: List<DigitalInput.Change>) {
        if (!ready) return
        for (c in changes) if (c.key() >= 2000) {
            retro.queueEvent { NativeScreens.key(port, c.key() - 2000, c.pressed()) }
        } else if (c.key() < 1000)
            retro.sendKeyEvent(
                if (c.pressed()) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                c.key(),
                port,
            )
        val held = digital[port].held()
        analog(
            "cbuttons:$port",
            port,
            GLRetroView.MOTION_SOURCE_ANALOG_RIGHT,
            (if (1004 in held) 1f else 0f) - (if (1003 in held) 1f else 0f),
            (if (1002 in held) 1f else 0f) - (if (1001 in held) 1f else 0f),
        )
        if (port == 0) {
            pad.pressed = held
            pad.invalidate()
        }
    }

    private fun keys(source: String, port: Int, keys: Set<Int>) {
        if (!ready || starting || menu || !active) return
        applyChanges(port, digital[port].update(source, keys))
    }

    private fun analog(source: String, port: Int, stick: Int, x: Float, y: Float) {
        if (!ready) return
        if (
            stick == GLRetroView.MOTION_SOURCE_ANALOG_LEFT &&
                game.system !in setOf(Console.ATARI5200, Console.N64, Console.PSP, Console.THREEDS)
        ) {
            applyChanges(
                port,
                digital[port].update(
                    source + ":digital",
                    if (active && !starting && !menu)
                        PadLayout.digitalAxes(x.toDouble(), y.toDouble())
                    else emptySet(),
                ),
            )
        }
        if (x == 0f && y == 0f) axes.remove(source)
        else if (active && !starting && !menu) axes[source] = Axis(port, stick, x, y)
        val values = axes.values.filter { it.port == port && it.stick == stick }
        fun strongest(get: (Axis) -> Float): Float =
            values.maxByOrNull { abs(get(it)) }?.let(get) ?: 0f
        retro.sendMotionEvent(stick, strongest { it.x }, strongest { it.y }, port)
    }

    private fun clear(prefix: String) {
        if (!::retro.isInitialized) return
        for (p in 0..3) applyChanges(p, digital[p].removePrefix(prefix))
        val affected = axes.filterKeys { it.startsWith(prefix) }.values.map { it.port to it.stick }
        axes.keys.removeAll { it.startsWith(prefix) }
        for ((p, s) in affected) analog("removed", p, s, 0f, 0f)
    }

    private fun mediaCommand(command: Int, index: Int) {
        if (!ready) return
        retro.queueEvent {
            runCatching { NativeScreens.media(command, index) }
                .onSuccess { status ->
                    runOnUiThread {
                        if (!isDestroyed) {
                            media = status
                            updateMedia()
                            if (status[3] < 0) toast("The core could not change media")
                        }
                    }
                }
                .onFailure { runOnUiThread { toast(it.message ?: "Media control failed") } }
        }
    }

    private fun updateMedia() {
        mediaRow.visibility = if (media[0] > 0) View.VISIBLE else View.GONE
        val out = media[2] != 0
        val theme = ControllerTheme.of(game.system)
        eject.text = if (out) "⏏ INSERT" else theme.mediaLabel()
        previous.visibility = if (out && media[0] > 1) View.VISIBLE else View.GONE
        next.visibility = previous.visibility
        mediaText.text =
            if (game.system == Console.FDS)
                "Disk ${media[1]/2+1} · Side ${if(media[1]%2==0)"A"else"B"}${if(out)" · ejected"else""}"
            else
                "${labels.getOrNull(media[1])?:"Disc ${media[1]+1} / ${media[0]}"}${if(out)" · open"else""}"
    }

    private fun pauseForMenu() {
        pad.release()
        clear("")
        menu = true
        registry.currentState = Lifecycle.State.STARTED
    }

    private fun resumeFromMenu() {
        menu = false
        pad.editing = false
        (pad.parent as? FrameLayout)?.let { parent ->
            for (i in parent.childCount - 1 downTo 0) {
                val child = parent.getChildAt(i)
                if (child.tag == "editDone") parent.removeView(child)
            }
        }
        pad.invalidate()
        if (active) registry.currentState = Lifecycle.State.RESUMED
    }

    private fun openMenu() {
        if (!ready || menu) return
        pauseForMenu()
        AlertDialog.Builder(this)
            .setTitle(game.name)
            .setItems(
                arrayOf(
                    "Resume",
                    "Save state",
                    "Load state",
                    "Edit controls",
                    "Controllers",
                    "Screen orientation",
                    "Screen spacing",
                    "Reset game",
                    "Return to library",
                )
            ) { _, n ->
                when (n) {
                    0 -> resumeFromMenu()
                    1 -> {
                        resumeFromMenu()
                        saveState()
                    }
                    2 -> {
                        resumeFromMenu()
                        loadState()
                    }
                    3 -> editControls()
                    4 -> {
                        PlayUi.controllerDialog(this, router)
                        resumeFromMenu()
                    }
                    5 -> {
                        PlayUi.orientation(this)
                        resumeFromMenu()
                    }
                    6 -> {
                        PlayUi.gapDialog(this, pad, store) { geometry() }
                        resumeFromMenu()
                    }
                    7 -> {
                        resumeFromMenu()
                        retro.queueEvent { retro.reset(false) }
                    }
                    8 -> {
                        resumeFromMenu()
                        quit()
                    }
                }
            }
            .setOnCancelListener { resumeFromMenu() }
            .show()
    }

    private fun editControls() {
        pad.editing = true
        val actions =
            arrayOf("Size / position of selected control", "Reset this orientation", "Done")
        val done =
            PlayUi.button(this, "Edit: tap / drag / pinch · Done") {
                    AlertDialog.Builder(this)
                        .setTitle("Edit controls")
                        .setItems(actions) { _, n ->
                            when (n) {
                                0 -> PlayUi.controlSettings(this, pad)
                                1 -> {
                                    pad.controls = PadLayout.defaults(game.system, portrait())
                                    pad.onChanged()
                                    pad.invalidate()
                                }
                                2 -> {
                                    (pad.parent as? FrameLayout)?.let { parent ->
                                        for (i in parent.childCount - 1 downTo 0) {
                                            val child = parent.getChildAt(i)
                                            if (child.tag == "editDone") parent.removeView(child)
                                        }
                                    }
                                    resumeFromMenu()
                                }
                            }
                        }
                        .show()
                }
                .apply { tag = "editDone" }
        (pad.parent as FrameLayout).addView(
            done,
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        )
        toast("Tap a control, drag it, or pinch it to resize")
    }

    private fun write(file: File, bytes: ByteArray) {
        file.parentFile!!.mkdirs()
        val temp = File(file.parentFile, file.name + ".next")
        temp.outputStream().use {
            it.write(bytes)
            it.flush()
        }
        java.nio.file.Files.move(
            temp.toPath(),
            file.toPath(),
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun saveRam(done: (() -> Unit)? = null) {
        if (!ready) {
            done?.invoke()
            return
        }
        retro.queueEvent {
            val result = runCatching { retro.serializeSRAM(false) }
            worker.execute {
                result.onSuccess {
                    if (it.isNotEmpty())
                        runCatching { write(ramFile, it) }
                            .onFailure {
                                runOnUiThread { toast("Save memory failed: ${it.message}") }
                            }
                }
                if (done != null) runOnUiThread(done)
            }
        }
    }

    private fun saveState() {
        retro.queueEvent {
            val result = runCatching { retro.serializeState(false) }
            worker.execute {
                val saved =
                    result.mapCatching {
                        require(it.isNotEmpty() && it.size <= 128 * 1024 * 1024) {
                            "Core does not support a bounded save state"
                        }
                        write(stateFile, it)
                    }
                runOnUiThread {
                    saved.fold({ toast("State saved") }, { toast("Unable to save: ${it.message}") })
                }
            }
        }
    }

    private fun loadState() {
        worker.execute {
            val result = runCatching {
                require(stateFile.isFile && stateFile.length() in 1..128 * 1024 * 1024) {
                    "No saved state for this game and core"
                }
                stateFile.readBytes()
            }
            result
                .onSuccess { bytes ->
                    retro.queueEvent {
                        val loaded =
                            runCatching { retro.unserializeState(bytes, false) }.getOrDefault(false)
                        runOnUiThread {
                            toast(if (loaded) "State loaded" else "Core rejected the saved state")
                        }
                    }
                }
                .onFailure { runOnUiThread { toast(it.message ?: "Unable to load state") } }
        }
    }

    private fun quit() {
        if (closing) return
        closing = true
        pad.release()
        clear("")
        saveRam { finish() }
    }

    private fun toast(message: String) {
        if (!isDestroyed) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        if (::router.isInitialized) router.start()
        if (::retro.isInitialized) registry.currentState = Lifecycle.State.STARTED
    }

    override fun onResume() {
        super.onResume()
        active = true
        if (starting && ::loadingOverlay.isInitialized)
            loadingOverlay.show("Starting ${game.name}…")
        if (::retro.isInitialized && !menu) registry.currentState = Lifecycle.State.RESUMED
    }

    override fun onPause() {
        if (::loadingOverlay.isInitialized) loadingOverlay.hide()
        if (::pad.isInitialized) {
            pad.release()
            clear("")
            if (!closing) saveRam()
        }
        active = false
        if (::retro.isInitialized) registry.currentState = Lifecycle.State.STARTED
        super.onPause()
    }

    override fun onStop() {
        if (::router.isInitialized) router.stop()
        if (::retro.isInitialized) registry.currentState = Lifecycle.State.CREATED
        super.onStop()
    }

    override fun onDestroy() {
        if (::loadingOverlay.isInitialized) loadingOverlay.hide()
        ready = false
        if (::retro.isInitialized) registry.currentState = Lifecycle.State.DESTROYED
        worker.shutdown()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (active && starting && ::loadingOverlay.isInitialized)
            loadingOverlay.show("Starting ${game.name}…")
        pad.post { geometry() }
    }

    override fun dispatchKeyEvent(event: KeyEvent) =
        if (::router.isInitialized && router.key(event)) true else super.dispatchKeyEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent) =
        if (::router.isInitialized && router.motion(event)) true
        else super.onGenericMotionEvent(event)

    @Deprecated("Activity back bridge")
    override fun onBackPressed() {
        if (menu) resumeFromMenu() else openMenu()
    }
}
