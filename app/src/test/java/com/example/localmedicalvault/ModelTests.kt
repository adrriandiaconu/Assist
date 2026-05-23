package com.example.localmedicalvault

import com.example.localmedicalvault.backup.MetadataBackup
import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import com.example.localmedicalvault.data.local.PatientEntity
import com.example.localmedicalvault.domain.filterDocuments
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelTests {
 @Test fun patientCreationLogic() { val p = PatientEntity(fullName = "Kid", dateOfBirth = "2019-01-01", relationType = "child"); assertEquals("child", p.relationType) }
 @Test fun documentMetadataCreation() { val d=MedicalDocumentEntity(patientId=1,title="Xray",category=DocumentCategory.IMAGING,documentDate="2026-05-20",localPath="/f",internalFileName="i",fileType="image"); assertEquals(DocumentCategory.IMAGING,d.category) }
 @Test fun categoryHandling() { assertEquals("ALLERGY", DocumentCategory.ALLERGY.name) }
 @Test fun searchFilter() { val docs=listOf(MedicalDocumentEntity(patientId=1,title="Blood",category=DocumentCategory.BLOOD_TESTS,documentDate="",localPath="/f",internalFileName="1",fileType="pdf")); assertEquals(1, filterDocuments(docs,"blood",1,DocumentCategory.BLOOD_TESTS).size) }
 @Test fun backupSerialization() { val json = Json.encodeToString(MetadataBackup(1,2,3)); assertTrue(json.contains("\"patients\":1")) }
}
