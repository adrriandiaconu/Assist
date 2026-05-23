package com.example.localmedicalvault.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.ui.components.PrimaryAction
import com.example.localmedicalvault.ui.components.SecondaryAction
import java.io.File

@Composable
fun DocumentFormScreen(patientId: Long, vm: VaultVM, onDone: () -> Unit) { var title by remember { mutableStateOf("") }; var date by remember { mutableStateOf("") }; var doctor by remember { mutableStateOf("") }; var clinic by remember { mutableStateOf("") }; var tags by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var cat by remember { mutableStateOf(DocumentCategory.OTHER) }; var selected by remember { mutableStateOf<Uri?>(null) }; var name by remember { mutableStateOf<String?>(null) }; var err by remember{mutableStateOf("")}
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> selected = uri; name = uri?.lastPathSegment }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Add document", style = MaterialTheme.typography.headlineSmall); SecondaryAction("Select file") { picker.launch(arrayOf("image/*", "application/pdf", "*/*")) }; Text(name ?: "No file selected"); OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(date, { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(doctor, { doctor = it }, label = { Text("Doctor") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(clinic, { clinic = it }, label = { Text("Clinic") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(tags, { tags = it }, label = { Text("Tags") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()); if(err.isNotBlank()) Text(err,color=MaterialTheme.colorScheme.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryAction("Save") { if(title.isBlank()||selected==null) err="Title and file are required" else { vm.addDocument(patientId, title, cat, date, doctor, clinic, tags, notes, selected, name); onDone() } }; SecondaryAction("Cancel") { onDone() } }
    }
}

@Composable
fun DocumentDetailScreen(id: Long, vm: VaultVM, onBack: () -> Unit) { val docs by vm.allDocs.collectAsState(); val context = LocalContext.current; val d = docs.firstOrNull { it.id == id } ?: return
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(d.title, style = MaterialTheme.typography.headlineSmall); Text("${d.category} • ${d.documentDate}"); Text("Doctor: ${d.doctorName}"); Text("Clinic: ${d.clinic}"); Text("Notes: ${d.notes.ifBlank { "-" }}"); if (d.fileType == "image") Image(painter = rememberAsyncImagePainter(File(d.localPath)), contentDescription = null, modifier = Modifier.size(180.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrimaryAction("Open document") { val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.fromFile(File(d.localPath)), if (d.fileType == "pdf") "application/pdf" else "*/*"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; runCatching { context.startActivity(intent) } }; SecondaryAction("Delete") { vm.deleteDocument(d); onBack() } }; SecondaryAction("Back") { onBack() } }
}
