package com.example.localmedicalvault.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import com.example.localmedicalvault.data.local.PatientEntity
import java.io.File

@Composable
fun DocumentsListScreen(padding: PaddingValues, docs: List<MedicalDocumentEntity>, patients: List<PatientEntity>, onOpen:(Long)->Unit){
    var filter by remember{mutableStateOf("All")}
    val categoryFilters = listOf(
        DocumentCategory.BLOOD_TESTS,
        DocumentCategory.PRESCRIPTION,
        DocumentCategory.DIAGNOSIS,
        DocumentCategory.ALLERGY,
        DocumentCategory.OTHER
    )
    val filters = listOf("All") + patients.map { it.fullName }.take(3) + categoryFilters.map { it.displayLabel() }
    val filtered = docs.filter { d ->
        filter == "All" ||
            d.category.displayLabel() == filter ||
            patients.firstOrNull { p -> p.id == d.patientId }?.fullName == filter
    }
    Column(Modifier.padding(padding).padding(16.dp)) {
        Text("Documents", style=MaterialTheme.typography.headlineSmall)
        Text("Original files, easy to find", color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(columns=GridCells.Fixed(3), modifier=Modifier.height(80.dp)) {
            items(filters.size){ i ->
                FilterChip(selected=filter==filters[i], onClick={filter=filters[i]}, label={ Text(filters[i]) })
            }
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(filtered){ d ->
                DocumentRowCard(d, patients.firstOrNull { it.id == d.patientId }?.fullName ?: "Unknown member", onOpen)
            }
        }
    }
}

@Composable
fun AddDocumentWizard(padding: PaddingValues, vm: VaultVM, patients: List<PatientEntity>, onFinish:()->Unit){
    var step by remember{mutableStateOf(1)}
    var patient by remember{mutableStateOf<PatientEntity?>(null)}
    var cat by remember{mutableStateOf(DocumentCategory.OTHER)}
    var selected by remember{mutableStateOf<Uri?>(null)}
    var name by remember{mutableStateOf<String?>(null)}
    var title by remember{mutableStateOf("")}
    var clinic by remember{mutableStateOf("")}
    var doctor by remember{mutableStateOf("")}
    var date by remember{mutableStateOf("")}
    var notes by remember{mutableStateOf("")}
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ selected=it; name=it?.lastPathSegment }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ) {
        Text("Add document", style=MaterialTheme.typography.headlineSmall)
        Text("Save the original first", color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            (1..4).forEach { LinearProgressIndicator(progress={if(step>=it)1f else 0f}, modifier=Modifier.weight(1f)) }
        }
        when(step){
            1 -> { Text("Who is this for?"); patients.forEach{ p -> ElevatedCard(onClick={patient=p; step=2}){ Text(p.fullName, modifier=Modifier.padding(12.dp)) } } }
            2 -> { TextButton(onClick={step=1}){Text("← Back")}; Text("What type of document?"); DocumentCategory.entries.forEach{ c -> OutlinedButton(onClick={cat=c; step=3}){Text(c.displayLabel())} } }
            3 -> { TextButton(onClick={step=2}){Text("← Back")}; Text("Add original file"); Button(onClick={picker.launch(arrayOf("image/*","application/pdf","*/*"))}, modifier=Modifier.fillMaxWidth()){Text("Upload PDF / image")}; Button(onClick={step=4}, modifier=Modifier.fillMaxWidth()){Text("Continue")}; Text(name?:"No file selected") }
            4 -> {
                TextButton(onClick={step=3}){Text("← Back")}
                Text("Basic details")
                OutlinedTextField(title,{title=it},label={Text("Title")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(date,{date=it},label={Text("Date")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(notes,{notes=it},label={Text("Notes")}, modifier=Modifier.fillMaxWidth())
                Button(onClick={
                    if(patient!=null && selected!=null){
                        vm.addDocument(patient!!.id,title.ifBlank{"${cat.name} document"},cat,date,doctor,clinic,"",notes,selected,name)
                        onFinish()
                    }
                }, modifier=Modifier.fillMaxWidth()){Text("Save document")}
            }
        }
    }
}

@Composable
fun DocumentDetailScreen(id: Long, vm: VaultVM, onBack: () -> Unit) {
    val docs by vm.allDocs.collectAsState()
    val patients by vm.patients.collectAsState()
    val context = LocalContext.current
    val d = docs.firstOrNull { it.id == id } ?: return
    val memberName = patients.firstOrNull { it.id == d.patientId }?.fullName ?: "Unknown member"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)){
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Document", style=MaterialTheme.typography.headlineSmall)
        Text(d.title, style=MaterialTheme.typography.titleLarge)
        Text(friendlyFileLabel(d.fileType, d.originalFileName), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card{ Column(Modifier.padding(12.dp)){
            Text("Member: $memberName")
            Text("Type: ${d.category.displayLabel()}")
            Text("Date: ${d.documentDate}")
            Text("Clinic: ${d.clinic}")
            Text("Doctor: ${d.doctorName}")
            Text("Source: ${friendlyFileLabel(d.fileType, d.originalFileName)}")
        } }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={
                val mime = if(d.fileType=="pdf") "application/pdf" else "image/*"
                val intent=Intent(Intent.ACTION_VIEW).apply{ setDataAndType(Uri.fromFile(File(d.localPath)), mime); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                runCatching{ context.startActivity(intent) }.onFailure {
                    Toast.makeText(context, "Could not open this file on this device.", Toast.LENGTH_SHORT).show()
                }
            }, modifier=Modifier.weight(1f)){Text("Open")}
            OutlinedButton(onClick={showDeleteConfirm = true}, modifier=Modifier.weight(1f)){Text("Delete")}
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete document?") },
                text = { Text("This removes the document from your vault.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        vm.deleteDocument(d)
                        onBack()
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
            )
        }
    }
}
