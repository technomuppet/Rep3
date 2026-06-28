package com.replog.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStream
import java.io.Writer

/**
 * Writes export files to a user-visible, permanent location instead of app-private
 * storage:
 *
 *  - If the user has chosen an export folder via the Storage Access Framework
 *    (a persisted tree URI), the file is written there.
 *  - Otherwise the file is written to the public Downloads/RepLog folder using
 *    MediaStore (no runtime permission needed on Android 10+; on API 26-28 a
 *    direct file write to the public Downloads dir is used as a fallback).
 *
 * Sprint 10 P3: content is streamed straight to the destination OutputStream via
 * a buffered Writer, so a large CSV/JSON export never has to be held in memory as
 * one giant String. [save] (String) is kept for small callers and delegates to
 * the streaming path.
 */
object FileExporter {

    private const val SUBFOLDER = "RepLog"

    data class ExportResult(
        val displayPath: String,   // e.g. "Downloads/RepLog/replog_export.csv"
        val shareUri: Uri?,        // content:// uri for sharing, when available
        val isPublic: Boolean      // true when saved to a user-visible folder
    )

    /** Save a pre-built string. Delegates to the streaming writer. */
    fun save(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String,
        treeUriString: String?
    ): ExportResult = saveStreaming(context, fileName, mimeType, treeUriString) { it.write(content) }

    /**
     * Stream the export. [writeBody] is invoked with a buffered Writer connected
     * directly to the destination file; nothing is buffered in memory beyond the
     * writer's buffer.
     */
    fun saveStreaming(
        context: Context,
        fileName: String,
        mimeType: String,
        treeUriString: String?,
        writeBody: (Writer) -> Unit
    ): ExportResult {
        if (!treeUriString.isNullOrBlank()) {
            runCatching { return saveToTree(context, treeUriString, fileName, mimeType, writeBody) }
        }
        return saveToDownloads(context, fileName, mimeType, writeBody)
    }

    private fun OutputStream.writeAll(writeBody: (Writer) -> Unit) {
        BufferedWriter(this.writer()).use { writeBody(it); it.flush() }
    }

    private fun saveToTree(
        context: Context,
        treeUriString: String,
        fileName: String,
        mimeType: String,
        writeBody: (Writer) -> Unit
    ): ExportResult {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString))
            ?: throw IllegalStateException("Export folder no longer accessible")
        // Replace an existing same-named file so re-exports overwrite cleanly.
        tree.findFile(fileName)?.delete()
        val doc = tree.createFile(mimeType, fileName)
            ?: throw IllegalStateException("Could not create file in export folder")
        (context.contentResolver.openOutputStream(doc.uri)
            ?: throw IllegalStateException("Could not write to export folder")).writeAll(writeBody)
        val label = tree.name?.let { "$it/$fileName" } ?: fileName
        return ExportResult(displayPath = label, shareUri = doc.uri, isPublic = true)
    }

    private fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        writeBody: (Writer) -> Unit
    ): ExportResult {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + SUBFOLDER)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create download entry")
            (resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Could not write download")).writeAll(writeBody)
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return ExportResult(displayPath = "Downloads/$SUBFOLDER/$fileName", shareUri = uri, isPublic = true)
        }
        // API 26-28: write directly into the public Downloads dir.
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SUBFOLDER)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        file.outputStream().writeAll(writeBody)
        val shareUri = runCatching {
            androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
        return ExportResult(displayPath = "Downloads/$SUBFOLDER/$fileName", shareUri = shareUri, isPublic = true)
    }
}
