package com.example.localmedicalvault.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface PatientDao { @Query("SELECT * FROM patients ORDER BY fullName") fun observeAll(): Flow<List<PatientEntity>>; @Query("SELECT * FROM patients WHERE id=:id") suspend fun getById(id: Long): PatientEntity?; @Insert suspend fun insert(patient: PatientEntity): Long; @Update suspend fun update(patient: PatientEntity); @Delete suspend fun delete(patient: PatientEntity) }
@Dao interface DocumentDao {
 @Query("SELECT * FROM documents WHERE patientId=:patientId ORDER BY documentDate DESC") fun byPatient(patientId: Long): Flow<List<MedicalDocumentEntity>>
 @Query("SELECT * FROM documents ORDER BY documentDate DESC") fun all(): Flow<List<MedicalDocumentEntity>>
 @Query("SELECT * FROM documents WHERE id=:id") suspend fun getById(id:Long): MedicalDocumentEntity?
 @Insert suspend fun insert(document: MedicalDocumentEntity): Long
 @Update suspend fun update(document: MedicalDocumentEntity)
 @Delete suspend fun delete(document: MedicalDocumentEntity)
 @Query("SELECT * FROM documents WHERE (title LIKE :q OR doctorName LIKE :q OR clinic LIKE :q OR notes LIKE :q OR tags LIKE :q OR category LIKE :q OR EXISTS (SELECT 1 FROM patients p WHERE p.id=documents.patientId AND p.fullName LIKE :q)) AND (:patientId IS NULL OR patientId=:patientId) AND (:category IS NULL OR category=:category) AND (:fromDate='' OR documentDate>=:fromDate) AND (:toDate='' OR documentDate<=:toDate) ORDER BY documentDate DESC") suspend fun search(q:String, patientId:Long?, category: DocumentCategory?, fromDate:String, toDate:String): List<MedicalDocumentEntity>
}
@Dao interface VisitDao { @Query("SELECT * FROM visits WHERE patientId=:patientId ORDER BY visitDate DESC") fun byPatient(patientId: Long): Flow<List<VisitEntity>>; @Query("SELECT * FROM visits ORDER BY visitDate DESC") fun all(): Flow<List<VisitEntity>>; @Insert suspend fun insert(visit: VisitEntity): Long; @Update suspend fun update(visit: VisitEntity); @Delete suspend fun delete(visit: VisitEntity) }
@Dao interface VisitDocDao { @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun link(ref: VisitDocumentCrossRef); @Query("SELECT documentId FROM visit_documents WHERE visitId=:visitId") suspend fun getDocIds(visitId:Long): List<Long> }
