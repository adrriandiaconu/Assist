package com.example.localmedicalvault.domain

import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.data.local.MedicalDocumentEntity

fun filterDocuments(input: List<MedicalDocumentEntity>, query: String, patientId: Long?, category: DocumentCategory?): List<MedicalDocumentEntity> =
    input.filter { (query.isBlank() || listOf(it.title,it.doctorName,it.clinic,it.notes,it.tags).any { v -> v.contains(query, true) }) && (patientId==null || it.patientId==patientId) && (category==null || it.category==category) }
