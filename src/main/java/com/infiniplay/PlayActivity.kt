package com.infiniplay

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import com.infiniplay.core.*
import io.github.gretchycat.infiniselect.*
import java.io.File
import java.util.concurrent.Executors

class PlayActivity : Activity() {
    private lateinit var store: PlayStore
    private lateinit var root: LinearLayout
    private var system = Console.NES
    private var coreId = "quicknes"
    private var pendingGame: String? = null
    private var picking = "game"
    private var firmwareName = ""
    private var busy = false
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var browser: EmbeddedFileBrowser
    private lateinit var statusText: TextView
    private var page = "recents"
    private lateinit var collections: PlayCollections
    private var incoming: Intent? = null
    private var queuedIncoming: Intent? = null
    private lateinit var router: GamepadRouter

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = PlayStore(this)
        collections = PlayCollections(this)
        page = state?.getString("page") ?: "recents"
        system =
            runCatching { Console.valueOf(state?.getString("system") ?: "NES") }
                .getOrDefault(Console.NES)
        coreId = state?.getString("core") ?: "quicknes"
        pendingGame = state?.getString("pending")
        picking = state?.getString("picking") ?: "game"
        firmwareName = state?.getString("firmware") ?: ""
        requestedOrientation = store.orientation()
        router = GamepadRouter(this, { _, _, _ -> }, { _, _, _, _, _ -> }, {}, {})
        build()
        if (state == null) handleIncoming(intent)
    }

    private fun build() {
        root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(PlayUi.background)
                fitsSystemWindows = true
            }
        setContentView(root)
        val top = LinearLayout(this)
        top.addView(
            PlayUi.button(this, "☰") { setupMenu() }.apply { contentDescription = "Setup menu" }
        )
        top.addView(PlayUi.label(this, "InfiniPlay", 24f), LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)
        val tabs = LinearLayout(this)
        for ((id, label) in
            listOf("recents" to "Recents", "favorites" to "Favorites", "open" to "Open")) {
            tabs.addView(
                PlayUi.button(this, label) { if (!busy) showPage(id) },
                LinearLayout.LayoutParams(0, -2, 1f),
            )
        }
        root.addView(tabs)
        statusText = PlayUi.label(this, "")
        root.addView(statusText)
        browser =
            EmbeddedFileBrowser(this).apply {
                onSelected = { if (!busy) openSelection(it) }
                fileActions = {
                    if (busy) emptyList()
                    else
                        listOf(
                            if (page == "favorites")
                                SelectionAction("remove_favorite", "Remove favorite")
                            else SelectionAction("add_favorite", "Add to favorites")
                        )
                }
                onFileAction = { action, item ->
                    if (!busy)
                        openSelection(item, favorite = action == "add_favorite", play = false)
                }
                onRequestOpen = {
                    picking = "game"
                    browse()
                }
                onRequestTree = { initial ->
                    StorageAccess.selectDirectory(this@PlayActivity, 42, initial)
                }
                onError = { message("Storage", it) }
            }
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        if (intent?.action !in listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND)) showPage(page)
        else browser.open()
    }

    private fun setupMenu() {
        AlertDialog.Builder(this)
            .setTitle("Setup")
            .setItems(
                arrayOf(
                    "Core / firmware",
                    "Edit controller layouts",
                    "Display / controllers / about",
                    if (browser.breadcrumbsEnabled) "Hide breadcrumb trail"
                    else "Show breadcrumb trail",
                )
            ) { _, n ->
                when (n) {
                    0 -> setupSystem { coreSettings() }
                    1 -> setupSystem { editControls() }
                    2 -> settings()
                    3 -> {
                        browser.breadcrumbsEnabled = !browser.breadcrumbsEnabled
                    }
                }
            }
            .show()
    }

    private fun setupSystem(action: () -> Unit) {
        val systems = Console.values()
        AlertDialog.Builder(this)
            .setTitle("Configure system")
            .setItems(systems.map { it.title }.toTypedArray()) { _, i ->
                system = systems[i]
                coreId = store.preferredCore(system)
                action()
            }
            .show()
    }

    private fun showPage(next: String) {
        page = next
        work({
            if (next == "open") null
            else {
                collections.initialize(store.games())
                collections.directory(next)
            }
        }) { directory ->
            browser.sortByModified = next == "recents"
            browser.open(
                directory,
                if (next == "open") "Virtual root" else next.replaceFirstChar { it.uppercase() },
            )
        }
    }

    private fun refreshLibrary() {
        if (::statusText.isInitialized) statusText.text = if (busy) "Opening…" else ""
    }

    private data class Opened(
        val item: SelectionItem,
        val hint: com.infiniplay.core.Console?,
        val detected: com.infiniplay.core.Console?,
    )

    private fun openSelection(
        item: SelectionItem,
        favorite: Boolean? = null,
        play: Boolean = true,
    ) {
        if (favorite != null) {
            changeFavorite(item, favorite)
            return
        }
        work({
            var selected = item
            var hint: com.infiniplay.core.Console? = null
            val seen = hashSetOf<String>()
            repeat(8) {
                if (
                    selected.name.endsWith(".iflink", true) ||
                        selected.mimeType == FileShortcut.MIME
                ) {
                    check(seen.add(selected.authoritativeUri.toString())) { "Cyclic game link" }
                    val link = FileShortcut.read(this, selected.contentUri)
                    hint =
                        link.system?.let {
                            runCatching { com.infiniplay.core.Console.valueOf(it) }.getOrNull()
                        }
                    val source =
                        InfiniSelect.requireSource(
                            this,
                            link.targetUri,
                            link.displayName,
                            link.mimeType,
                        )
                    selected =
                        SelectionItem(
                            source.contentUri,
                            source.displayName ?: link.displayName,
                            source.mimeType ?: link.mimeType,
                            source.sizeBytes,
                            sourceReference = source.authoritativeUri.toString(),
                        )
                }
            }
            require(!selected.name.endsWith(".iflink", true)) { "Too many nested game links" }
            val known =
                store.games().firstOrNull { it.source == selected.authoritativeUri.toString() }
            val header =
                StorageAccess.openRead(this, selected.contentUri).use { input ->
                    val bytes = ByteArray(512)
                    var offset = 0
                    while (offset < bytes.size) {
                        val n = input.read(bytes, offset, bytes.size - offset)
                        if (n < 0) break
                        offset += n
                    }
                    bytes.copyOf(offset)
                }
            Opened(selected, hint ?: known?.system, MediaSystem.identify(selected.name, header))
        }) { opened ->
            val detected = opened.detected ?: opened.hint
            if (detected != null) finishOpening(opened.item, detected, favorite, play)
            else {
                val systems = com.infiniplay.core.Console.values()
                AlertDialog.Builder(this)
                    .setTitle("System for ${opened.item.name}")
                    .setItems(systems.map { it.title }.toTypedArray()) { _, i ->
                        finishOpening(opened.item, systems[i], favorite, play)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun changeFavorite(item: SelectionItem, add: Boolean) {
        work({
            if (item.name.endsWith(".iflink", true) || item.mimeType == FileShortcut.MIME)
                FileShortcut.read(this, item.contentUri)
            else FileShortcut(item.authoritativeUri.toString(), item.name, item.mimeType)
        }) { link ->
            val selected =
                link.system?.let { runCatching { Console.valueOf(it) }.getOrNull() }
                    ?: MediaSystem.identify(link.displayName, byteArrayOf())
                    ?: store.games().firstOrNull { it.source == link.targetUri }?.system
            fun apply(system: Console) {
                work({ collections.favorite(link, system, add) }) {
                    browser.refresh()
                    Toast.makeText(
                            this,
                            if (add) "Added to favorites" else "Favorite removed",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            }
            if (selected != null) apply(selected)
            else {
                val systems = Console.values()
                AlertDialog.Builder(this)
                    .setTitle("System for ${link.displayName}")
                    .setItems(systems.map { it.title }.toTypedArray()) { _, i -> apply(systems[i]) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun finishOpening(
        item: SelectionItem,
        selected: com.infiniplay.core.Console,
        favorite: Boolean?,
        play: Boolean,
    ) {
        work({
            collections.initialize(store.games())
            val game =
                store.games().firstOrNull {
                    it.source == item.authoritativeUri.toString() && it.system == selected
                } ?: MediaImport(this).game(item, selected).also { store.addGame(it) }
            collections.directory("favorites", game.system)
            collections.link("recents", game)
            if (favorite == true) collections.link("favorites", game)
            if (favorite == false) collections.removeFavorite(game)
            game
        }) { game ->
            system = game.system
            coreId = store.preferredCore(system)
            incoming = null
            if (play) {
                pendingGame = game.id
                launchPending()
            } else {
                browser.refresh()
                Toast.makeText(
                        this,
                        if (favorite == true) "Added to favorites" else "Favorite removed",
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }

    private fun handleIncoming(data: Intent?) {
        if (data?.action !in listOf(Intent.ACTION_VIEW, Intent.ACTION_SEND)) return
        incoming = data
        val uri =
            if (data?.action == Intent.ACTION_SEND) {
                @Suppress("DEPRECATION")
                data.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    ?: data.clipData?.getItemAt(0)?.uri
            } else data?.data
        if (uri == null) return
        val result = Intent(data).setData(uri)
        StorageAccess.persist(this, result)
        work({ InfiniSelect.readResult(this, result).items.single() }) { openSelection(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (busy) queuedIncoming = intent else handleIncoming(intent)
    }

    @Deprecated("Activity back bridge")
    override fun onBackPressed() {
        if (!browser.goBack()) super.onBackPressed()
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
                        setupSystem {
                            if (system.dual())
                                PlayUi.gapDialog(
                                    this,
                                    PadView(this).apply { system = this@PlayActivity.system },
                                    store,
                                ) {}
                            else message("Screen spacing", "Screen spacing applies to DS and 3DS.")
                        }
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
                store.preferredCore(system, coreId)
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
        dialog.show()
        dialog.window?.setLayout(-1, -1)
    }

    private fun browse() {
        InfiniSelect.open(
            this,
            OpenRequest(
                clientId = "infiniplay",
                fileActions =
                    if (picking == "game")
                        listOf(SelectionAction("add_favorite", "Add to favorites"))
                    else emptyList(),
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
        if (request == 42) {
            if (result == RESULT_OK && data != null) {
                StorageAccess.persist(this, data)
                browser.open()
            }
            return
        }
        if (request != 41) return
        if (result != RESULT_OK || data == null) {
            pendingGame = null
            return
        }
        StorageAccess.persist(this, data)
        if (picking == "game") {
            work({ InfiniSelect.readResult(this, data) }) { selected ->
                if (selected.actionId == "add_favorite")
                    openSelection(selected.items.single(), favorite = true, play = false)
                else openSelection(selected.items.single())
            }
            return
        }
        val kind = picking
        val selectedCore = coreId
        val expectedName = firmwareName
        work({
            val item = InfiniSelect.readResult(this, data).items.single()
            val importer = MediaImport(this)
            when (kind) {
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
        }) {
            refreshLibrary()
            if (pendingGame != null) launchPending()
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
                    if (!busy && queuedIncoming != null) {
                        val next = queuedIncoming
                        queuedIncoming = null
                        handleIncoming(next)
                    }
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
        refreshLibrary()
        if (::browser.isInitialized) browser.refresh()
    }

    override fun onStop() {
        router.stop()
        super.onStop()
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
    }

    override fun dispatchKeyEvent(event: KeyEvent) =
        if (router.key(event)) true else super.dispatchKeyEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent) =
        if (router.motion(event)) true else super.onGenericMotionEvent(event)

    override fun onSaveInstanceState(out: Bundle) {
        out.putString("page", page)
        out.putString("system", system.name)
        out.putString("core", coreId)
        out.putString("pending", pendingGame)
        out.putString("picking", picking)
        out.putString("firmware", firmwareName)
        super.onSaveInstanceState(out)
    }
}
