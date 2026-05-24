package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.ui.components.InfoCard
import com.example.localmedicalvault.ui.components.PrimaryAction
import com.example.localmedicalvault.ui.components.SecondaryAction

@Composable
fun VisitsScreen(patientId: Long, vm: VaultVM, onBack: () -> Unit, onAdd: () -> Unit) { var visits by remember { mutableStateOf(listOf<com.example.localmedicalvault.data.local.VisitEntity>()) }; LaunchedEffect(patientId) { vm.visitsByPatient(patientId).collect { visits = it } }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Visits", style = MaterialTheme.typography.headlineSmall); PrimaryAction("Add visit") { onAdd() }; if (visits.isEmpty()) InfoCard("No visits yet", "Add a first visit note for this patient.") ; LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(visits) { v -> InfoCard(v.visitDate, "${v.doctor} • ${v.clinic}\nReason: ${v.reason}", trailing = "0 docs") } }; SecondaryAction("Back") { onBack() } }
}

@Composable
fun VisitFormScreen(patientId: Long, vm: VaultVM, onDone: () -> Unit) { var date by remember { mutableStateOf("") }; var doctor by remember { mutableStateOf("") }; var clinic by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Add visit", style = MaterialTheme.typography.headlineSmall); OutlinedTextField(date, { date = it }, label = { Text("Date") }); OutlinedTextField(doctor, { doctor = it }, label = { Text("Doctor") }); OutlinedTextField(clinic, { clinic = it }, label = { Text("Clinic") }); OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryAction("Save") { vm.addVisit(patientId, date, doctor, clinic, reason, notes); onDone() }; SecondaryAction("Cancel") { onDone() } } }
}
