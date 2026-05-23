package com.example.localmedicalvault.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.localmedicalvault.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class BackupDocument(val id:Long,val patientId:Long,val title:String,val category:String,val localPath:String,val internalFileName:String,val fileType:String)
@Serializable data class BackupPatient(val id:Long,val fullName:String,val dateOfBirth:String,val notes:String,val relationType:String)
@Serializable data class BackupVisit(val id:Long,val patientId:Long,val visitDate:String,val doctor:String,val clinic:String,val reason:String,val notes:String)
@Serializable data class FullBackup(val patients:List<BackupPatient>,val documents:List<BackupDocument>,val visits:List<BackupVisit>)

class BackupManager(private val context: Context, private val db: AppDatabase) {
    suspend fun export(treeUri: Uri): Boolean = runCatching {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        val backup = root.findFile("MedicalVaultBackup") ?: root.createDirectory("MedicalVaultBackup") ?: return false
        val filesDir = backup.findFile("files") ?: backup.createDirectory("files") ?: return false

        val patients = db.patientDao().observeAll().first()
        val docs = db.documentDao().all().first()
        val visits = db.visitDao().all().first()
        val payload = FullBackup(
            patients.map { BackupPatient(it.id,it.fullName,it.dateOfBirth,it.notes,it.relationType) },
            docs.map { BackupDocument(it.id,it.patientId,it.title,it.category.name,it.localPath,it.internalFileName,it.fileType) },
            visits.map { BackupVisit(it.id,it.patientId,it.visitDate,it.doctor,it.clinic,it.reason,it.notes) }
        )
        val metadata = backup.findFile("metadata.json") ?: backup.createFile("application/json", "metadata") ?: return false
        context.contentResolver.openOutputStream(metadata.uri)?.use { it.write(Json { prettyPrint = true }.encodeToString(payload).toByteArray()) } ?: return false
        docs.forEach { doc ->
            val src = java.io.File(doc.localPath)
            if (src.exists()) {
                val target = filesDir.findFile(doc.internalFileName) ?: filesDir.createFile("application/octet-stream", doc.internalFileName) ?: return@forEach
                context.contentResolver.openOutputStream(target.uri)?.use { out -> src.inputStream().use { input -> input.copyTo(out) } }
            }
        }
        true
    }.getOrDefault(false)
}
