package com.example.localmedicalvault

import com.example.localmedicalvault.backup.FullBackup
import com.example.localmedicalvault.backup.BackupDocument
import com.example.localmedicalvault.backup.BackupPatient
import com.example.localmedicalvault.backup.BackupVisit
import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import com.example.localmedicalvault.data.local.PatientEntity
import com.example.localmedicalvault.domain.filterDocuments
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelTests {
 @Test fun patientCreationLogic() { val p = PatientEntity(fullName = "Kid", dateOfBirth = "", relationType = "child"); assertEquals("child", p.relationType) }
 @Test fun documentMetadataCreation() { val d=MedicalDocumentEntity(patientId=1,title="Xray",category=DocumentCategory.IMAGING,documentDate="2026-05-20",doctorName="D",clinic="C",notes="N",localPath="/f",internalFileName="i",fileType="image"); assertEquals(DocumentCategory.IMAGING,d.category) }
 @Test fun searchFilter() { val docs=listOf(MedicalDocumentEntity(patientId=1,title="Blood",category=DocumentCategory.BLOOD_TESTS,documentDate="",doctorName="A",clinic="City",notes="urgent",tags="lab",localPath="/f",internalFileName="1",fileType="pdf")); assertEquals(1, filterDocuments(docs,"urgent",1,DocumentCategory.BLOOD_TESTS).size) }
 @Test fun backupSerializationStructure() {
  val backup = FullBackup(
   patients = listOf(BackupPatient(1,"Kid","","","child")),
   documents = listOf(BackupDocument(2,1,"Blood","BLOOD_TESTS","/p","doc.bin","pdf")),
   visits = listOf(BackupVisit(3,1,"2026-05-20","Dr A","Clinic","Checkup","ok"))
  )
  val json = Json.encodeToString(backup)
  assertTrue(json.contains("\"patients\""))
  assertTrue(json.contains("\"documents\""))
  assertTrue(json.contains("\"visits\""))
 }
}
