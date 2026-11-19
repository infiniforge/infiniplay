package com.infiniplay

import android.content.Context
import com.infiniplay.core.*
import com.infiniplay.core.Console
import io.github.gretchycat.infiniselect.*
import java.io.*
import java.util.*

class MediaImport(private val context: Context) {
    fun copy(source: ResolvedSource, target: File, limit: Long): Long {
        require(source.sizeBytes == null || source.sizeBytes!! <= limit) {
            "File exceeds the import limit"
        }
        target.parentFile!!.mkdirs()
        val tmp = File(target.parentFile, target.name + ".part")
        var total = 0L
        try {
            StorageAccess.openRead(context, source.contentUri).use { input ->
                tmp.outputStream().use { out ->
                    val b = ByteArray(128 * 1024)
                    while (true) {
                        if (Thread.currentThread().isInterrupted)
                            throw InterruptedIOException("Import cancelled")
                        val n = input.read(b)
                        if (n < 0) break
                        total += n
                        require(total <= limit) { "File exceeds the import limit" }
                        if (total % (8 * 1024 * 1024) < b.size)
                            require(target.parentFile!!.usableSpace > 16 * 1024 * 1024) {
                                "Not enough free space"
                            }
                        out.write(b, 0, n)
                    }
                }
            }
            require(total > 0) { "Selected file is empty" }
            java.nio.file.Files.move(
                tmp.toPath(),
                target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
            return total
        } finally {
            tmp.delete()
        }
    }

    fun game(item: SelectionItem, system: Console): PlayStore.Game {
        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "games/$id")
        directory.mkdirs()
        val entry = MediaManifest.path(item.name.substringAfterLast('/').substringAfterLast('\\'))
        val source =
            InfiniSelect.requireSelectedSource(
                context,
                item.contentUri,
                item.authoritativeUri.toString(),
                item.name,
                item.mimeType,
                item.sizeBytes,
            )
        val seen = mutableSetOf<String>()
        var bytes = 0L
        fun visit(relative: String, src: ResolvedSource, depth: Int) {
            require(depth <= 3) { "Media references are nested too deeply" }
            if (!seen.add(relative)) return
            require(seen.size <= MediaManifest.MAX_FILES) { "Too many media files" }
            val target = File(directory, relative)
            bytes += copy(src, target, 2L * 1024 * 1024 * 1024)
            require(bytes <= 8L * 1024 * 1024 * 1024) { "Media set exceeds 8 GiB" }
            if (target.extension.lowercase() in setOf("m3u", "cue")) {
                require(target.length() <= 1024 * 1024) { "Manifest is too large" }
                for (ref in MediaManifest.references(target.name, target.readText())) {
                    val uri =
                        InfiniSelect.resolveRelativeLocation(context, src.authoritativeUri, ref)
                            ?: error(
                                "Cannot resolve $ref; select this set from a directory accessible through InfiniFolder"
                            )
                    val child = InfiniSelect.requireSource(context, uri.toString(), ref)
                    val parent = relative.substringBeforeLast('/', "")
                    visit(if (parent.isEmpty()) ref else "$parent/$ref", child, depth + 1)
                }
            }
        }
        try {
            visit(entry, source, 0)
            return PlayStore.Game(
                id,
                item.name.substringBeforeLast('.'),
                when (entry.substringAfterLast('.').lowercase()) {
                    "fds" -> Console.FDS
                    "nes" -> Console.NES
                    "gb" -> Console.GB
                    "gbc" -> Console.GBC
                    "sg" -> Console.SG1000
                    "sms" -> Console.MASTERSYSTEM
                    "gg" -> Console.GAMEGEAR
                    "32x" -> Console.SEGA32X
                    "pce" -> Console.PCE
                    "lnx",
                    "lyx" -> Console.LYNX
                    "a26" -> Console.ATARI2600
                    "a52",
                    "car" -> Console.ATARI5200
                    "a78" -> Console.ATARI7800
                    "vb",
                    "vboy" -> Console.VIRTUALBOY
                    "neo" -> Console.NEOGEO
                    "sfc",
                    "smc" -> Console.SNES
                    else -> system
                },
                "$id/$entry",
                source.authoritativeUri.toString(),
            )
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }
    }
}
