package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.data.local.PatientEntity
import com.example.localmedicalvault.ui.components.InfoCard
import com.example.localmedicalvault.ui.components.PrimaryAction
import com.example.localmedicalvault.ui.components.SecondaryAction

@Composable
fun HomeScreen(padding: PaddingValues, patients: List<PatientEntity>, onAddPatient: () -> Unit, onAddDocument: (Long) -> Unit, onOpenPatient: (Long) -> Unit, docCountByPatient: (Long) -> Int) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Medical Vault", style = MaterialTheme.typography.headlineMedium)
            Text("Family medical documents, stored locally", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            PrimaryAction("Add document", Modifier.fillMaxWidth(), onClick = { patients.firstOrNull()?.let { onAddDocument(it.id) } })
            SecondaryAction("Add patient", Modifier.fillMaxWidth(), onClick = onAddPatient)
        }
        if (patients.isEmpty()) item { InfoCard("No patients yet", "Add your first family member to start organizing documents.") }
        items(patients) { p ->
            InfoCard(title = p.fullName, subtitle = "DOB: ${p.dateOfBirth.ifBlank { "Not set" }} • ${p.relationType}", trailing = "${docCountByPatient(p.id)} docs") {
                SecondaryAction("Open folder") { onOpenPatient(p.id) }
            }
        }
    }
}
