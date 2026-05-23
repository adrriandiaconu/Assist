package com.example.localmedicalvault.backup

import kotlinx.serialization.Serializable

@Serializable data class MetadataBackup(val patients: Int, val documents: Int, val visits: Int)
