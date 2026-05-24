package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import com.example.localmedicalvault.data.local.PatientEntity

@Composable
fun HomeScreen(
    padding: PaddingValues,
    patients: List<PatientEntity>,
    docs: List<MedicalDocumentEntity>,
    onAdd: () -> Unit,
    onAddMember: () -> Unit,
    onOpenDoc: (Long) -> Unit,
    onGoDocs: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Family Medical Vault", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Fast. Clear. Mobile. Simple.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Original documents are the truth", color = Color(0xFFCBD5E1))
                    Text(
                        "Your family medical archive, always in your pocket.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Button(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF111827),
                        ),
                    ) {
                        Text("+ Add document")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Family", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onAddMember, contentPadding = PaddingValues(0.dp)) {
                        Text("Add member")
                    }
                    Text("${patients.size} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            if (patients.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("No family members yet", style = MaterialTheme.typography.titleMedium)
                        Text("Add your first family member to start organizing documents.")
                        OutlinedButton(onClick = onAddMember) {
                            Text("Add first family member")
                        }
                    }
                }
            } else {
                val rows = (patients.size + 1) / 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height((rows * 100).dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(patients.size) { i ->
                        val p = patients[i]
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(p.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${p.relationType} · ${p.dateOfBirth.ifBlank { "Age not set" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Recent documents", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onGoDocs) { Text("See all") }
            }
        }

        items(docs.take(3).size) { index ->
            val d = docs[index]
            DocumentRowCard(d, patients.firstOrNull { it.id == d.patientId }?.fullName ?: "Unknown member", onOpenDoc)
        }
    }
}

@Composable
fun DocumentRowCard(d: MedicalDocumentEntity, memberName: String, onOpenDoc: (Long) -> Unit) {
    Card(onClick = { onOpenDoc(d.id) }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.titleSmall)
                Text("$memberName · ${d.category.displayLabel()}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${d.clinic} · ${d.documentDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                friendlyFileLabel(d.fileType, d.originalFileName),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(6.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
