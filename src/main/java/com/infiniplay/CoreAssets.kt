package com.infiniplay

import android.content.Context
import android.util.AtomicFile
import java.io.File
import org.json.JSONObject

/** Installs shipped core data on the import worker before entering the emulator process. */
internal object CoreAssets {
    fun prepare(context: Context, coreId: String) {
        if (coreId != "ppsspp") return
        val base = "cores/ppsspp"
        val manifest =
            context.assets.open("$base/manifest.json").bufferedReader().use {
                JSONObject(it.readText())
            }
        val root = File(context.filesDir, "firmware/ppsspp")
        val marker = File(root, ".bundled-revision")
        val revision = manifest.getString("revision")
        val files = manifest.getJSONArray("files")
        if (
            marker.isFile &&
                marker.readText() == revision &&
                (0 until files.length()).all { File(root, files.getString(it)).isFile }
        )
            return
        for (i in 0 until files.length()) {
            if (Thread.currentThread().isInterrupted)
                throw java.io.InterruptedIOException("Core data preparation cancelled")
            val relative = files.getString(i)
            val target = File(root, relative)
            target.parentFile!!.mkdirs()
            write(target) { output ->
                context.assets.open("$base/$relative").use { it.copyTo(output) }
            }
        }
        write(marker) { it.write(revision.toByteArray(Charsets.UTF_8)) }
    }

    private fun write(target: File, copy: (java.io.OutputStream) -> Unit) {
        val file = AtomicFile(target)
        val output = file.startWrite()
        try {
            copy(output)
            file.finishWrite(output)
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }
}
