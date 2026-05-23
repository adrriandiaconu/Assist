package com.example.localmedicalvault.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class DocumentStorage(private val context: Context) {
    fun importToPrivateStorage(uri: Uri, originalName: String?): Triple<String, String, String> {
        val ext = originalName?.substringAfterLast('.', "bin") ?: "bin"
        val fileType = when (ext.lowercase()) {"jpg","jpeg","png","webp" -> "image"; "pdf" -> "pdf"; else -> "other"}
        val internalName = "doc_${UUID.randomUUID()}.$ext"
        val outFile = File(context.filesDir, internalName)
        context.contentResolver.openInputStream(uri)?.use { input -> outFile.outputStream().use { input.copyTo(it) } }
        return Triple(outFile.absolutePath, internalName, fileType)
    }
    fun deleteLocalFile(path: String) { runCatching { File(path).delete() } }
}
