package com.infiniplay

import android.content.Context
import android.os.Environment
import com.infiniplay.core.Console
import io.github.gretchycat.infiniselect.FileShortcut
import java.io.File

/** Shared links reference original sources; imported cores/saves remain private. */
class PlayCollections(private val context: Context) {
    val root
        get() = File(Environment.getExternalStorageDirectory(), "infini/play")

    fun directory(kind: String, system: Console? = null): File {
        require(kind in listOf("recents", "favorites"))
        return File(
                root,
                kind + (system?.let { "/" + it.name.lowercase(java.util.Locale.ROOT) } ?: ""),
            )
            .apply {
                check(isDirectory || mkdirs()) {
                    "Allow internal storage access to create infini/play/$kind"
                }
            }
    }

    /** Preserve the old private library as recent links during the first shared-library setup. */
    fun initialize(games: List<PlayStore.Game>) {
        val marker = File(context.filesDir, "shared-collections-v1")
        if (marker.exists()) return
        directory("recents")
        directory("favorites")
        games
            .filter { it.source.isNotBlank() }
            .forEach { game ->
                FileShortcut(game.source, File(game.file).name, system = game.system.name)
                    .writeTo(directory("recents", game.system))
                directory("favorites", game.system)
            }
        marker.writeText("1")
    }

    fun link(kind: String, game: PlayStore.Game): File {
        val dir = directory(kind, game.system)
        val target =
            FileShortcut(game.source, File(game.file).name, system = game.system.name).writeTo(dir)
        if (kind == "recents") target.setLastModified(System.currentTimeMillis())
        return target
    }

    fun favorite(link: FileShortcut, system: Console, add: Boolean) {
        val directory = directory("favorites", system)
        if (add) link.copy(system = system.name).writeTo(directory)
        else
            directory.listFiles().orEmpty().forEach { file ->
                if (
                    file.isFile &&
                        file.extension.equals("iflink", true) &&
                        file.length() <= FileShortcut.MAX_BYTES &&
                        runCatching {
                                FileShortcut.decode(file.readText()).targetUri == link.targetUri
                            }
                            .getOrDefault(false)
                )
                    check(file.delete()) { "Could not remove favorite link" }
            }
    }

    fun removeFavorite(game: PlayStore.Game) {
        favorite(
            FileShortcut(game.source, File(game.file).name, system = game.system.name),
            game.system,
            false,
        )
    }
}
