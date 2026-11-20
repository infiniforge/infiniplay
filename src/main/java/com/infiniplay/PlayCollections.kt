package com.infiniplay

import android.content.Context
import android.net.Uri
import com.infiniplay.core.Console
import io.github.gretchycat.infiniselect.FileShortcut
import io.github.gretchycat.infiniselect.ShortcutCollections
import java.io.File

/** Shared links reference original sources; imported cores/saves remain private. */
class PlayCollections(private val context: Context) {
    private val storage = ShortcutCollections(context, "infiniplay")

    fun directory(kind: String, system: Console? = null): Uri {
        require(kind in listOf("recents", "favorites"))
        return storage.directory(kind, system?.name?.lowercase(java.util.Locale.ROOT))
    }

    /** Preserve the old private library as recent links during the first shared-library setup. */
    fun initialize(games: List<PlayStore.Game>) {
        val marker =
            File(context.filesDir, "collections-v2-" + if (storage.relayed) "relay" else "private")
        if (marker.exists()) return
        directory("recents")
        directory("favorites")
        games
            .filter { it.source.isNotBlank() }
            .forEach { game ->
                storage.put(
                    "recents",
                    game.system.name.lowercase(java.util.Locale.ROOT),
                    FileShortcut(game.source, File(game.file).name, system = game.system.name),
                )
                directory("favorites", game.system)
            }
        marker.writeText("1")
    }

    fun link(kind: String, game: PlayStore.Game): Uri =
        storage.put(
            kind,
            game.system.name.lowercase(java.util.Locale.ROOT),
            FileShortcut(game.source, File(game.file).name, system = game.system.name),
            touch = kind == "recents",
        )

    fun favorite(link: FileShortcut, system: Console, add: Boolean) {
        val profile = system.name.lowercase(java.util.Locale.ROOT)
        if (add) storage.put("favorites", profile, link.copy(system = system.name))
        else storage.remove("favorites", profile, link)
    }

    fun removeFavorite(game: PlayStore.Game) =
        favorite(
            FileShortcut(game.source, File(game.file).name, system = game.system.name),
            game.system,
            false,
        )
}
