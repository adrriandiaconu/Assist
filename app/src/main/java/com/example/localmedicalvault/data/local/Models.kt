package com.example.localmedicalvault.data.local

import androidx.room.*

enum class DocumentCategory { BLOOD_TESTS, IMAGING, PRESCRIPTION, DIAGNOSIS, DISCHARGE_LETTER, VACCINATION, ALLERGY, OTHER }

@Entity(tableName = "patients")
data class PatientEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val fullName: String, val dateOfBirth: String, val notes: String = "", val relationType: String = "")

@Entity(tableName = "documents")
data class MedicalDocumentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val patientId: Long, val title: String, val category: DocumentCategory, val documentDate: String, val doctorName: String = "", val clinic: String = "", val tags: String = "", val notes: String = "", val localPath: String, val originalFileName: String = "", val internalFileName: String, val fileType: String, val createdAt: Long = System.currentTimeMillis(), val updatedAt: Long = System.currentTimeMillis())

@Entity(tableName = "visits")
data class VisitEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val patientId: Long, val visitDate: String, val doctor: String = "", val clinic: String = "", val reason: String = "", val notes: String = "")

@Entity(primaryKeys = ["visitId", "documentId"], tableName = "visit_documents")
data class VisitDocumentCrossRef(val visitId: Long, val documentId: Long)
