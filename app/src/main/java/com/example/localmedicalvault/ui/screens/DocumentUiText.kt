package com.example.localmedicalvault.ui.screens

import com.example.localmedicalvault.data.local.DocumentCategory

fun DocumentCategory.displayLabel(): String = when (this) {
    DocumentCategory.ALLERGY -> "Allergy"
    DocumentCategory.DIAGNOSIS -> "Diagnosis"
    DocumentCategory.PRESCRIPTION -> "Prescription"
    DocumentCategory.OTHER -> "Other"
    DocumentCategory.BLOOD_TESTS -> "Lab results"
    DocumentCategory.IMAGING -> "Consultation"
    DocumentCategory.DISCHARGE_LETTER -> "Consultation"
    DocumentCategory.VACCINATION -> "Consultation"
}

fun friendlyFileLabel(fileType: String, originalFileName: String): String {
    val type = fileType.lowercase()
    if (type == "pdf") return "Original PDF"
    if (type == "jpg" || type == "jpeg" || type == "png" || type == "webp" || type == "heic") return "Original image"
    if (originalFileName.isNotBlank()) return "Imported document"
    return "File attached"
}
