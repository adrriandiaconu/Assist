package com.example.localmedicalvault.data.repo

import android.content.Context
import com.example.localmedicalvault.data.local.*
import kotlinx.coroutines.flow.Flow

class VaultRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val patients = db.patientDao()
    private val documents = db.documentDao()
    private val visits = db.visitDao()
    private val visitDocs = db.visitDocDao()

    fun observePatients(): Flow<List<PatientEntity>> = patients.observeAll()
    suspend fun getPatient(id: Long) = patients.getById(id)
    suspend fun addPatient(patient: PatientEntity) = patients.insert(patient)
    suspend fun updatePatient(patient: PatientEntity) = patients.update(patient)
    suspend fun deletePatient(patient: PatientEntity) = patients.delete(patient)

    fun documentsByPatient(patientId: Long) = documents.byPatient(patientId)
    fun allDocuments() = documents.all()
    suspend fun addDocument(document: MedicalDocumentEntity) = documents.insert(document)
    suspend fun updateDocument(document: MedicalDocumentEntity) = documents.update(document)
    suspend fun deleteDocument(document: MedicalDocumentEntity) = documents.delete(document)
    suspend fun search(q: String, patientId: Long?, category: DocumentCategory?, from: String, to: String) = documents.search("%$q%", patientId, category, from, to)

    fun visitsByPatient(patientId: Long) = visits.byPatient(patientId)
    suspend fun addVisit(v: VisitEntity) = visits.insert(v)
    suspend fun updateVisit(v: VisitEntity) = visits.update(v)
    suspend fun deleteVisit(v: VisitEntity) = visits.delete(v)

    suspend fun linkDocumentToVisit(visitId: Long, documentId: Long) = visitDocs.link(VisitDocumentCrossRef(visitId, documentId))
    suspend fun docIdsForVisit(visitId: Long) = visitDocs.getDocIds(visitId)
}
