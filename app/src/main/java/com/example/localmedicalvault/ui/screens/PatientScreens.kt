package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.ui.components.InfoCard
import com.example.localmedicalvault.ui.components.PrimaryAction
import com.example.localmedicalvault.ui.components.SecondaryAction

@Composable
fun PatientFormScreen(id: Long?, vm: VaultVM, onDone: () -> Unit, padding: PaddingValues = PaddingValues(0.dp)) { var n by remember { mutableStateOf("") }; var dob by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var rel by remember { mutableStateOf("Child") }; var err by remember{mutableStateOf("")}
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) { Text("Patient details", style = MaterialTheme.typography.headlineSmall); OutlinedTextField(n, { n = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(dob, { dob = it }, label = { Text("Date of birth") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(rel, { rel = it }, label = { Text("Relation: Child / Parent / Self / Other") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()); if(err.isNotBlank()) Text(err,color=MaterialTheme.colorScheme.error); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryAction("Save") { if(n.isBlank()) err="Name is required" else { vm.addOrUpdatePatient(id, n, dob, notes, rel); onDone() } }; SecondaryAction("Cancel", onClick = onDone) } }
}

@Composable
fun PatientDetailScreen(id: Long, vm: VaultVM, onBack: () -> Unit, onEdit: (Long) -> Unit, onDocs: (Long) -> Unit, onVisits: (Long) -> Unit, onOpenDoc: (Long) -> Unit) { val pList by vm.patients.collectAsState(); val docs by vm.allDocs.collectAsState(); val p = pList.firstOrNull { it.id == id }; var tab by remember{mutableStateOf(0)}
    if (p == null) return
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(p.fullName, "DOB: ${p.dateOfBirth} • ${p.relationType}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryAction("Add document") { onDocs(id) }; SecondaryAction("Add visit") { onVisits(id) } }
        TabRow(selectedTabIndex = tab) { listOf("Documents", "Visits", "Notes").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) } }
        when (tab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(docs.filter { it.patientId == id }) { d -> InfoCard(d.title, "${d.category} • ${d.documentDate}", trailing = d.fileType.uppercase()) { SecondaryAction("Open") { onOpenDoc(d.id) } } } }
            1 -> Text("Use Visits section to manage visit timeline.")
            else -> Text(if (p.notes.isBlank()) "No notes yet" else p.notes)
        }
        Spacer(Modifier.height(6.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { SecondaryAction("Edit") { onEdit(id) }; SecondaryAction("Back") { onBack() } }
    }
}
