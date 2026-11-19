package com.infiniplay

import android.content.Context
import android.content.pm.ActivityInfo
import com.infiniplay.core.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*
import org.json.*

/** Fresh atomic file reads keep launcher and emulator process settings consistent. */
class PlayStore(private val context: Context) {
    data class Game(
        val id: String,
        val name: String,
        val system: Console,
        val file: String,
        val source: String,
    )

    private fun read(name: String) =
        runCatching {
                JSONObject(File(context.filesDir, name).takeIf { it.isFile }?.readText() ?: "{}")
            }
            .getOrElse { JSONObject() }

    private fun write(name: String, json: JSONObject) {
        val target = File(context.filesDir, name)
        target.parentFile!!.mkdirs()
        val temp = File(target.parentFile, target.name + ".next")
        temp.writeText(json.toString())
        Files.move(temp.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    }

    private fun update(name: String, edit: (JSONObject) -> Unit) {
        val json = read(name)
        edit(json)
        write(name, json)
    }

    fun virtualBoyDisplay() =
        read("settings.json").optString("virtualBoyDisplay", "red & cyan").takeIf {
            it in listOf("disabled", "red & cyan", "red & blue")
        } ?: "red & cyan"

    fun virtualBoyDisplay(value: String) {
        update("settings.json") { it.put("virtualBoyDisplay", value) }
    }

    fun orientation() =
        read("settings.json").optInt("orientation", ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)

    fun orientation(value: Int) {
        update("settings.json") { it.put("orientation", value) }
    }

    fun layouts(system: Console, portrait: Boolean): List<PadLayout.Control> {
        val defaults = PadLayout.defaults(system, portrait)
        return runCatching {
                val data = read("controls/${system.name}.$portrait.json")
                defaults.map { c ->
                    data.optJSONObject(c.id())?.let {
                        c.move(it.getDouble("x"), it.getDouble("y")).resize(it.getDouble("size"))
                    } ?: c
                }
            }
            .getOrDefault(defaults)
    }

    fun saveLayout(system: Console, portrait: Boolean, controls: List<PadLayout.Control>) {
        val data = JSONObject()
        controls.forEach {
            data.put(it.id(), JSONObject().put("x", it.x()).put("y", it.y()).put("size", it.size()))
        }
        write("controls/${system.name}.$portrait.json", data)
    }

    fun gap(system: Console) =
        read("settings.json").optDouble("gap.${system.name}", system.defaultGap).takeIf {
            it.isFinite() && it in 0.0..300.0
        } ?: system.defaultGap

    fun gap(system: Console, value: Double) {
        require(value.isFinite() && value in 0.0..300.0)
        update("settings.json") { it.put("gap.${system.name}", value) }
    }

    fun corePath(id: String): String? =
        read("cores.json")
            .optString(id)
            .takeIf { it.isNotBlank() }
            ?.takeIf {
                runCatching {
                        val root = File(context.codeCacheDir, "cores").canonicalPath
                        File(it).canonicalPath.startsWith(root + "/")
                    }
                    .getOrDefault(false)
            }

    fun corePath(id: String, path: String) {
        update("cores.json") { it.put(id, path) }
    }

    fun controllerPort(descriptor: String, fallback: Int) =
        read("controllers.json").optInt(descriptor, fallback).coerceIn(0, 3)

    fun controllerPort(descriptor: String, port: Int, save: Boolean) {
        if (save) update("controllers.json") { it.put(descriptor, port.coerceIn(0, 3)) }
    }

    fun games(): List<Game> =
        runCatching {
                val data =
                    JSONArray(
                        File(context.filesDir, "games.json").takeIf { it.isFile }?.readText()
                            ?: "[]"
                    )
                (0 until data.length())
                    .map {
                        val g = data.getJSONObject(it)
                        Game(
                            g.getString("id"),
                            g.getString("name"),
                            Console.valueOf(g.getString("system")),
                            g.getString("file"),
                            g.optString("source"),
                        )
                    }
                    .filter {
                        it.id.matches(Regex("[a-f0-9-]{36}")) &&
                            it.file.startsWith(it.id + "/") &&
                            runCatching { MediaManifest.path(it.file) == it.file }
                                .getOrDefault(false) &&
                            File(context.filesDir, "games/${it.file}").isFile
                    }
            }
            .getOrDefault(emptyList())

    fun addGame(game: Game) {
        val data = JSONArray()
        (games().filter { it.id != game.id } + game).forEach {
            data.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("system", it.system.name)
                    .put("file", it.file)
                    .put("source", it.source)
            )
        }
        val tmp = File(context.filesDir, "games.next")
        tmp.writeText(data.toString())
        Files.move(
            tmp.toPath(),
            File(context.filesDir, "games.json").toPath(),
            ATOMIC_MOVE,
            REPLACE_EXISTING,
        )
    }
}
