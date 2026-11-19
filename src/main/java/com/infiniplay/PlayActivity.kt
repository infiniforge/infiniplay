package com.infiniplay

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.os.*
import android.view.*
import android.widget.*
import com.infiniplay.core.*
import io.github.gretchycat.infiniselect.*
import java.io.File
import java.util.concurrent.Executors

class PlayActivity : Activity() {
    private lateinit var store: PlayStore
    private lateinit var pad: PadView
    private lateinit var list: LinearLayout
    private lateinit var root: LinearLayout
    private var system = Console.NES
    private var coreId = "quicknes"
    private var pendingGame: String? = null
    private var picking = "game"
    private var firmwareName = ""
    private var busy = false
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var router: GamepadRouter
    private val digital = DigitalInput()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = PlayStore(this)
        system =
            runCatching { Console.valueOf(state?.getString("system") ?: "NES") }
                .getOrDefault(Console.NES)
        coreId = state?.getString("core") ?: "quicknes"
        pendingGame = state?.getString("pending")
        picking = state?.getString("picking") ?: "game"
        firmwareName = state?.getString("firmware") ?: ""
        requestedOrientation = store.orientation()
        router =
            GamepadRouter(
                this,
                { s, _, k ->
                    digital.update(s, k)
                    if (::pad.isInitialized) {
                        pad.pressed = digital.held()
                        pad.invalidate()
                    }
                },
                { _, _, _, _, _ -> },
                { prefix ->
                    digital.removePrefix(prefix)
                    if (::pad.isInitialized) {
                        pad.pressed = digital.held()
                        pad.invalidate()
                    }
                },
                {},
            )
        build()
    }

    private fun build() {
        root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(PlayUi.background)
                fitsSystemWindows = true
            }
        setContentView(root)
        root.addView(PlayUi.label(this, "InfiniPlay", 24f))
        val toolbar = LinearLayout(this)
        root.addView(toolbar)
        toolbar.addView(
            PlayUi.button(this, "Import game") {
                if (!busy) {
                    picking = "game"
                    browse()
                }
            },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        toolbar.addView(
            PlayUi.button(this, "Settings") { settings() },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        val systems = Console.values()
        val spinner = Spinner(this)
        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                systems.map { it.title },
            )
        spinner.setSelection(systems.indexOf(system))
        root.addView(spinner)
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p: AdapterView<*>?) {}

                override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                    val next = systems[position]
                    if (next != system) {
                        system = next
                        coreId = CoreCatalog.ALL.first { it.system() == system }.id()
                        refreshPad()
                        refreshLibrary()
                    }
                }
            }
        val actions = LinearLayout(this)
        root.addView(actions)
        actions.addView(
            PlayUi.button(this, "Core / firmware") { coreSettings() },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        actions.addView(
            PlayUi.button(this, "Edit controls") { editControls() },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        pad = PadView(this)
        pad.onGeometry = { refreshPad() }
        pad.onKeys = { s, k ->
            digital.update(s, k)
            pad.pressed = digital.held()
            pad.invalidate()
        }
        root.addView(pad, LinearLayout.LayoutParams(-1, 0, 1.4f))
        refreshPad()
        refreshLibrary()
    }

    private fun refreshPad() {
        if (!::pad.isInitialized) return
        val portrait = resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        pad.system = system
        pad.controls = store.layouts(system, portrait)
        if (pad.width > 0 && pad.height > 0)
            pad.screens =
                ScreenLayout.fit(
                    system,
                    pad.width.toDouble(),
                    pad.height.toDouble(),
                    portrait,
                    store.gap(system),
                )
        pad.invalidate()
    }

    private fun refreshLibrary() {
        list.removeAllViews()
        val games = store.games().filter { it.system == system }
        list.addView(
            PlayUi.label(
                this,
                if (busy) "Importing…"
                else if (games.isEmpty())
                    "Import a game to begin. M3U playlists keep multi-disc games together."
                else "${games.size} games · ${CoreCatalog.get(coreId).title()}",
            )
        )
        for (g in games) list.addView(
            PlayUi.button(this, "▶  ${g.name}") {
                if (!busy) {
                    pendingGame = g.id
                    launchPending()
                }
            }
        )
    }

    private fun settings() {
        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(
                arrayOf(
                    "Screen orientation",
                    "USB / Bluetooth controllers",
                    "Screen spacing",
                    "Virtual Boy display",
                    "About / acknowledgments",
                )
            ) { _, n ->
                when (n) {
                    0 -> PlayUi.orientation(this)
                    1 -> PlayUi.controllerDialog(this, router)
                    2 ->
                        if (system.dual()) PlayUi.gapDialog(this, pad, store) { refreshPad() }
                        else message("Screen spacing", "Choose DS or 3DS to adjust the hinge gap.")
                    3 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Virtual Boy display · next launch")
                            .setItems(
                                arrayOf(
                                    "Red / black · no glasses",
                                    "Red / cyan · anaglyph glasses",
                                    "Red / blue · anaglyph glasses",
                                )
                            ) { _, i ->
                                store.virtualBoyDisplay(
                                    listOf("disabled", "red & cyan", "red & blue")[i]
                                )
                            }
                            .show()
                    }
                    4 -> {
                        val text =
                            PlayUi.label(
                                this,
                                assets.open("notices.txt").bufferedReader().use { it.readText() },
                            )
                        val scroll = ScrollView(this).apply { addView(text) }
                        AlertDialog.Builder(this)
                            .setTitle("InfiniPlay · Acknowledgments / licenses")
                            .setView(scroll)
                            .setPositiveButton("Done", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun coreSettings() {
        val cores = CoreCatalog.ALL.filter { it.system() == system }
        AlertDialog.Builder(this)
            .setTitle("${system.title} core")
            .setSingleChoiceItems(
                cores
                    .map { it.title() + if (it.bundled()) " · built in" else " · import .so" }
                    .toTypedArray(),
                cores.indexOfFirst { it.id() == coreId },
            ) { d, n ->
                coreId = cores[n].id()
                d.dismiss()
                refreshLibrary()
                val core = CoreCatalog.get(coreId)
                AlertDialog.Builder(this)
                    .setTitle(core.title())
                    .setItems(
                        arrayOf("Use this core", "Import / replace core", "Firmware requirements")
                    ) { _, action ->
                        when (action) {
                            1 ->
                                if (core.bundled())
                                    message(
                                        "Built-in core",
                                        "This core is compiled into InfiniPlay.",
                                    )
                                else {
                                    picking = "core"
                                    browse()
                                }
                            2 -> {
                                val req = Firmware.requirements(coreId)
                                message(
                                    "Firmware",
                                    if (req.isEmpty())
                                        "No mandatory firmware is declared for this core. Some games may need additional system files; see the core's own documentation."
                                    else
                                        req.joinToString("\n") {
                                            it.name() + " · " + it.description()
                                        },
                                )
                            }
                        }
                    }
                    .show()
            }
            .setNegativeButton("Done", null)
            .show()
    }

    private fun editControls() {
        val dialog = Dialog(this, android.R.style.Theme_Material_NoActionBar)
        val box =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(PlayUi.background)
            }
        val preview =
            PadView(this).apply {
                system = this@PlayActivity.system
                editing = true
                onGeometry = {
                    val p =
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    controls = store.layouts(system, p)
                    if (width > 0 && height > 0)
                        screens =
                            ScreenLayout.fit(
                                system,
                                width.toDouble(),
                                height.toDouble(),
                                p,
                                store.gap(system),
                            )
                    invalidate()
                }
                onChanged = {
                    store.saveLayout(
                        system,
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE,
                        controls,
                    )
                }
            }
        box.addView(
            PlayUi.label(this, "Drag controls to move. Pinch a selected control to resize.")
        )
        val row = LinearLayout(this)
        box.addView(row)
        row.addView(
            PlayUi.button(this, "Size / position") { PlayUi.controlSettings(this, preview) },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        row.addView(
            PlayUi.button(this, "Reset") {
                preview.controls =
                    PadLayout.defaults(
                        system,
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE,
                    )
                preview.onChanged()
                preview.invalidate()
            },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        row.addView(
            PlayUi.button(this, "Done") { dialog.dismiss() },
            LinearLayout.LayoutParams(0, -2, 1f),
        )
        box.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        dialog.setContentView(box)
        dialog.setOnDismissListener { refreshPad() }
        dialog.show()
        dialog.window?.setLayout(-1, -1)
    }

    private fun browse() {
        InfiniSelect.open(
            this,
            OpenRequest(
                clientId = "infiniplay",
                displayName =
                    when (picking) {
                        "core" -> "Select Android arm64 Libretro core"
                        "firmware" -> "Select $firmwareName"
                        else -> "Select game or M3U disc set"
                    },
                supportedExtensions =
                    when (picking) {
                        "core" -> listOf("so")
                        "firmware" -> listOf("bin", "rom", "img", "pce", "zip")
                        else ->
                            listOf(
                                "a26",
                                "a52",
                                "car",
                                "a78",
                                "lnx",
                                "lyx",
                                "sms",
                                "sg",
                                "gg",
                                "pce",
                                "sgx",
                                "32x",
                                "neo",
                                "vb",
                                "vboy",
                                "nes",
                                "fds",
                                "gb",
                                "gbc",
                                "sfc",
                                "smc",
                                "gba",
                                "md",
                                "gen",
                                "z64",
                                "n64",
                                "v64",
                                "cue",
                                "chd",
                                "pbp",
                                "iso",
                                "bin",
                                "rom",
                                "img",
                                "m3u",
                                "nds",
                                "3ds",
                                "cci",
                                "cxi",
                            )
                    },
            ),
            41,
        )
    }

    @Deprecated("Android activity result bridge")
    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (request != 41) return
        if (result != RESULT_OK || data == null) {
            pendingGame = null
            return
        }
        StorageAccess.persist(this, data)
        val kind = picking
        val selectedSystem = system
        val selectedCore = coreId
        val expectedName = firmwareName
        work({
            val item = InfiniSelect.readResult(this, data).items.single()
            val importer = MediaImport(this)
            when (kind) {
                "game" -> {
                    val game = importer.game(item, selectedSystem)
                    store.addGame(game)
                    game.system.name
                }
                "core" -> {
                    val source =
                        InfiniSelect.requireSelectedSource(
                            this,
                            item.contentUri,
                            item.authoritativeUri.toString(),
                            item.name,
                            item.mimeType,
                            item.sizeBytes,
                        )
                    val path =
                        File(codeCacheDir, "cores/$selectedCore-${java.util.UUID.randomUUID()}.so")
                    importer.copy(source, path, 64 * 1024 * 1024L)
                    try {
                        validateCore(path)
                        check(path.setReadOnly()) { "Cannot protect imported core" }
                        store.corePath(selectedCore, path.absolutePath)
                    } catch (e: Exception) {
                        path.delete()
                        throw e
                    }
                    ""
                }
                else -> {
                    val req =
                        Firmware.requirements(selectedCore).single { it.name() == expectedName }
                    val source =
                        InfiniSelect.requireSelectedSource(
                            this,
                            item.contentUri,
                            item.authoritativeUri.toString(),
                            item.name,
                            item.mimeType,
                            item.sizeBytes,
                        )
                    val target = File(filesDir, "firmware/$selectedCore/${req.name()}")
                    val candidate = File(target.parentFile, "candidate.bin")
                    try {
                        importer.copy(source, candidate, 32 * 1024 * 1024L)
                        Firmware.validateFile(req, candidate)
                        java.nio.file.Files.move(
                            candidate.toPath(),
                            target.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        )
                    } finally {
                        candidate.delete()
                    }
                    ""
                }
            }
        }) { resultText ->
            if (kind == "game") {
                system = Console.valueOf(resultText)
                coreId =
                    if (system == selectedSystem) selectedCore
                    else CoreCatalog.ALL.first { it.system() == system }.id()
                build()
            } else {
                refreshLibrary()
                if (pendingGame != null) launchPending()
            }
        }
    }

    private fun validateCore(file: File) {
        val b = ByteArray(20)
        file.inputStream().use { java.io.DataInputStream(it).readFully(b) }
        require(
            b.size == 20 &&
                b[0] == 0x7f.toByte() &&
                b[1] == 69.toByte() &&
                b[2] == 76.toByte() &&
                b[3] == 70.toByte() &&
                b[4] == 2.toByte() &&
                b[5] == 1.toByte() &&
                (b[18].toInt() and 255) == 183 &&
                b[19] == 0.toByte()
        ) {
            "Select an Android arm64 ELF Libretro .so core"
        }
    }

    private fun launchPending() {
        val game = store.games().firstOrNull { it.id == pendingGame } ?: return
        val core = CoreCatalog.get(coreId)
        if (core.system() != game.system) {
            pendingGame = null
            message("Wrong core", "Choose a core for ${game.system.title}.")
            return
        }
        if (!core.bundled() && (store.corePath(coreId)?.let { File(it).isFile } != true)) {
            AlertDialog.Builder(this)
                .setTitle("Core required")
                .setMessage(
                    "Select a compatible Android arm64 Libretro ${core.title()} core. It will run inside InfiniPlay."
                )
                .setPositiveButton("Browse") { _, _ ->
                    picking = "core"
                    browse()
                }
                .setNegativeButton("Cancel") { _, _ -> pendingGame = null }
                .setOnCancelListener { pendingGame = null }
                .show()
            return
        }
        val missing = Firmware.missing(coreId, File(filesDir, "firmware/$coreId"))
        if (missing.isNotEmpty()) {
            val next = missing.first()
            firmwareName = next.name()
            AlertDialog.Builder(this)
                .setTitle("${next.name()} required")
                .setMessage(
                    "${next.description()} is missing or has the wrong size. Select your firmware file to continue launching ${game.name}."
                )
                .setPositiveButton("Browse") { _, _ ->
                    picking = "firmware"
                    browse()
                }
                .setNegativeButton("Cancel") { _, _ -> pendingGame = null }
                .setOnCancelListener { pendingGame = null }
                .show()
            return
        }
        pendingGame = null
        startActivity(
            Intent(this, SessionActivity::class.java)
                .putExtra("game", game.id)
                .putExtra("core", coreId)
        )
    }

    private fun <T> work(action: () -> T, done: (T) -> Unit) {
        busy = true
        refreshLibrary()
        worker.execute {
            val result = runCatching(action)
            runOnUiThread {
                if (!isDestroyed) {
                    busy = false
                    result.fold(done) {
                        pendingGame = null
                        message("Unable to import", it.message ?: it.javaClass.simpleName)
                    }
                    refreshLibrary()
                }
            }
        }
    }

    private fun message(title: String, text: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton("Done", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        router.start()
        refreshPad()
        refreshLibrary()
    }

    override fun onStop() {
        router.stop()
        pad.release()
        super.onStop()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        refreshPad()
    }

    override fun dispatchKeyEvent(event: KeyEvent) =
        if (router.key(event)) true else super.dispatchKeyEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent) =
        if (router.motion(event)) true else super.onGenericMotionEvent(event)

    override fun onSaveInstanceState(out: Bundle) {
        out.putString("system", system.name)
        out.putString("core", coreId)
        out.putString("pending", pendingGame)
        out.putString("picking", picking)
        out.putString("firmware", firmwareName)
        super.onSaveInstanceState(out)
    }
}
