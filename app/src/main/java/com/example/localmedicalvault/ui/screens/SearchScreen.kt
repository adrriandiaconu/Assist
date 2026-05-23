package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.localmedicalvault.ui.components.InfoCard
import com.example.localmedicalvault.ui.components.PrimaryAction

@Composable
fun SearchScreen(vm: VaultVM, onOpen: (Long) -> Unit) { val scope = rememberCoroutineScope(); var q by remember { mutableStateOf("") }; var results by remember { mutableStateOf(listOf<com.example.localmedicalvault.data.local.MedicalDocumentEntity>()) }; var from by remember { mutableStateOf("") }; var to by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Search", style = MaterialTheme.typography.headlineSmall); OutlinedTextField(q, { q = it }, label = { Text("Search documents") }); FilterChip(selected = false, onClick = {}, label = { Text("Patient") }); FilterChip(selected = false, onClick = {}, label = { Text("Category") }); FilterChip(selected = false, onClick = {}, label = { Text("Date") }); OutlinedTextField(from, { from = it }, label = { Text("From date") }); OutlinedTextField(to, { to = it }, label = { Text("To date") }); PrimaryAction("Search") { scope.launch { results = vm.search(q, null, null, from, to) } }
        if (results.isEmpty()) InfoCard("No documents found", "Try a different keyword or date range.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(results) { d -> InfoCard(d.title, "${d.category} • ${d.documentDate}\n${d.doctorName} • ${d.clinic}", trailing = d.fileType.uppercase()) { PrimaryAction("Open") { onOpen(d.id) } } } }
    }
}
