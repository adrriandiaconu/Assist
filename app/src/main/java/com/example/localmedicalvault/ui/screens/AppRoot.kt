package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localmedicalvault.backup.BackupManager
import com.example.localmedicalvault.data.local.*
import com.example.localmedicalvault.data.repo.VaultRepository
import com.example.localmedicalvault.storage.DocumentStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

private val ScreenBg = Color(0xFFF3F5F9)
private val HeroBg = Color(0xFF1F2937)
private val PrimaryBtn = Color(0xFF334155)
private val CardShape = RoundedCornerShape(18.dp)

sealed class Screen { data object Home:Screen(); data class PatientDetail(val id:Long):Screen(); data class PatientForm(val id:Long?):Screen(); data class DocForm(val patientId:Long?):Screen(); data class DocDetail(val id:Long):Screen(); data class VisitForm(val patientId:Long):Screen(); data class Visits(val patientId:Long):Screen(); data object Search:Screen(); data object Settings:Screen() }

class VaultVM(app: Application): AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addOrUpdatePatient(id:Long?,name:String,dob:String,notes:String,rel:String)=viewModelScope.launch {
        if(name.isBlank()) return@launch
        if(id==null) repo.addPatient(PatientEntity(fullName=name.trim(),dateOfBirth=dob.trim(),notes=notes.trim(),relationType=rel.trim())) else repo.updatePatient(PatientEntity(id,name.trim(),dob.trim(),notes.trim(),rel.trim()))
    }
    fun deletePatient(p:PatientEntity)=viewModelScope.launch{ repo.deletePatient(p) }
    fun addDocument(patientId:Long?,title:String,cat:DocumentCategory,date:String,doctor:String,clinic:String,tags:String,notes:String,uri:Uri?,orig:String?,onResult:(Boolean,String)->Unit)=viewModelScope.launch {
        if(patientId == null) return@launch onResult(false, "Please select a patient")
        if(uri==null) return@launch onResult(false, "Please select a file")
        val result = runCatching { storage.importToPrivateStorage(uri,orig) }
        result.onFailure { onResult(false, "File import failed") }
        result.onSuccess { (path,intName,type) ->
            repo.addDocument(MedicalDocumentEntity(patientId=patientId,title=title.ifBlank { orig ?: "Untitled" },category=cat,documentDate=date,doctorName=doctor,clinic=clinic,tags=tags,notes=notes,localPath=path,originalFileName=orig?:"",internalFileName=intName,fileType=type))
            onResult(true, "Saved")
        }
    }
    fun deleteDocument(doc:MedicalDocumentEntity)=viewModelScope.launch{ storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    fun addVisit(patientId:Long,date:String,doctor:String,clinic:String,reason:String,notes:String)=viewModelScope.launch{ repo.addVisit(VisitEntity(patientId=patientId,visitDate=date,doctor=doctor,clinic=clinic,reason=reason,notes=notes)) }
    fun visitsByPatient(patientId:Long)=repo.visitsByPatient(patientId)
    fun docsByPatient(patientId:Long)=repo.documentsByPatient(patientId)
    fun exportBackup(uri:Uri,onDone:(Boolean)->Unit)=viewModelScope.launch{ onDone(BackupManager(getApplication(),db).export(uri)) }
    suspend fun search(q:String,pid:Long?,cat:DocumentCategory?,from:String,to:String)=repo.search(q,pid,cat,from,to)
}

@Composable fun AppRoot(vm: VaultVM = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val patients by vm.patients.collectAsState()
    Scaffold(
        containerColor = ScreenBg,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = screen is Screen.Home,
                    onClick = { screen = Screen.Home },
                    icon = { Text("🏠") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = screen is Screen.DocForm || screen is Screen.DocDetail,
                    onClick = { screen = Screen.DocForm(null) },
                    icon = { Text("📄") },
                    label = { Text("Docs") }
                )
                Box(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { screen = Screen.DocForm(null) },
                        shape = CircleShape,
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White
                    ) { Text("+") }
                }
                NavigationBarItem(
                    selected = screen is Screen.Search,
                    onClick = { screen = Screen.Search },
                    icon = { Text("🔎") },
                    label = { Text("Search") }
                )
                NavigationBarItem(
                    selected = screen is Screen.Settings,
                    onClick = { screen = Screen.Settings },
                    icon = { Text("⚙️") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { p ->
        Box(Modifier.fillMaxSize().padding(p)) {
            when(val s=screen) {
                Screen.Home -> HomeScreen(patients, onOpen = { screen = Screen.PatientDetail(it) }, onAddMember = { screen = Screen.PatientForm(null) }, onDocs = { screen = Screen.DocForm(null) }, onSearch = { screen = Screen.Search }, onSettings = { screen = Screen.Settings })
                is Screen.PatientForm -> PatientFormScreen(s.id, vm){screen=Screen.Home}
                is Screen.PatientDetail -> PatientDetailScreen(s.id, vm, onBack={screen=Screen.Home}, onEdit={screen=Screen.PatientForm(it)}, onDocs={screen=Screen.DocForm(it)}, onVisits={screen=Screen.Visits(it)}, onOpenDoc={screen=Screen.DocDetail(it)})
                is Screen.DocForm -> DocumentFormScreen(s.patientId, vm, patients){screen=Screen.Home}
                is Screen.DocDetail -> DocumentDetailScreen(s.id, vm, patients, onBack={screen=Screen.Home})
                is Screen.Visits -> VisitsScreen(s.patientId, vm, onBack={screen=Screen.PatientDetail(s.patientId)}, onAdd={screen=Screen.VisitForm(s.patientId)})
                is Screen.VisitForm -> VisitFormScreen(s.patientId, vm){screen=Screen.Visits(s.patientId)}
                Screen.Search -> SearchScreen(vm, patients, onBack={screen=Screen.Home}, onOpen={screen=Screen.DocDetail(it)})
                Screen.Settings -> SettingsScreen(vm, onBack={screen=Screen.Home})
            }
        }
    }
}

@Composable private fun HomeScreen(patients: List<PatientEntity>, onOpen:(Long)->Unit, onAddMember:()->Unit, onDocs:()->Unit, onSearch:()->Unit, onSettings:()->Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = HeroBg)) {
            Column(Modifier.padding(18.dp)) {
                Text("Family Medical Vault", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Fast. Clear. Mobile. Simple.", color = Color(0xFFCBD5E1))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddMember, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)) { Text("Add") }
            Button(onClick = onDocs, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)) { Text("Docs") }
            Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)) { Text("Search") }
            Button(onClick = onSettings, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)) { Text("Settings") }
        }
        Text("Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (patients.isEmpty()) Text("No members yet", color = Color.Gray) else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(patients){ pat ->
                Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable { onOpen(pat.id) }) {
                    Column(Modifier.padding(14.dp)) { Text(pat.fullName, fontWeight = FontWeight.Bold); Text(pat.relationType, color = Color.Gray) }
                }
            }
        }
    }
}

@Composable private fun PatientFormScreen(id:Long?, vm:VaultVM, onDone:()->Unit){ var n by remember{mutableStateOf("")}; var dob by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var rel by remember{mutableStateOf("child")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){
        Text("Add Member", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(n,{n=it},label={Text("Full name")}, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(dob,{dob=it},label={Text("Date of birth")}, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(rel,{rel=it},label={Text("Relation")}, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes,{notes=it},label={Text("Notes")}, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick={ vm.addOrUpdatePatient(id,n,dob,notes,rel); onDone()}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Save")}
            OutlinedButton(onClick=onDone){Text("Cancel")}
        }
    }
}

@Composable private fun PatientDetailScreen(id:Long, vm:VaultVM, onBack:()->Unit,onEdit:(Long)->Unit,onDocs:(Long)->Unit,onVisits:(Long)->Unit,onOpenDoc:(Long)->Unit){ val pList by vm.patients.collectAsState(); val p=pList.firstOrNull{it.id==id}; var askDelete by remember{mutableStateOf(false)}; var docs by remember{mutableStateOf(listOf<MedicalDocumentEntity>())}; var visits by remember{mutableStateOf(listOf<VisitEntity>())}; LaunchedEffect(id){ vm.docsByPatient(id).collect{docs=it} }; LaunchedEffect(id){ vm.visitsByPatient(id).collect{visits=it} }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){ Text(p?.fullName?:"Member", style = MaterialTheme.typography.headlineSmall); Text("DOB: ${p?.dateOfBirth.orEmpty()}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){ Button(onClick={onEdit(id)}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Edit")}; Button(onClick={onDocs(id)}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Add Document")}; Button(onClick={onVisits(id)}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Visits")}}
        Text("Documents", fontWeight = FontWeight.Bold); if(docs.isEmpty()) Text("No documents yet") else docs.forEach { Text(it.title, Modifier.clickable { onOpenDoc(it.id) }) }
        Text("Recent visits", fontWeight = FontWeight.Bold); if(visits.isEmpty()) Text("No visits yet") else visits.take(5).forEach { Text("${it.visitDate} - ${it.doctor}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={askDelete=true}){Text("Delete")}; OutlinedButton(onClick=onBack){Text("Back")}}
    }
    if(askDelete && p!=null){ AlertDialog(onDismissRequest={askDelete=false}, confirmButton={TextButton(onClick={vm.deletePatient(p); askDelete=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={askDelete=false}){Text("Cancel")}}, text={Text("Delete patient?")}) }
}

@Composable private fun DocumentFormScreen(defaultPatientId:Long?, vm:VaultVM, patients:List<PatientEntity>, onDone:()->Unit){ val context=LocalContext.current; var patientId by remember{mutableStateOf(defaultPatientId)}; var title by remember{mutableStateOf("")}; var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var tags by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var cat by remember{mutableStateOf(DocumentCategory.OTHER)}; var selected by remember{mutableStateOf<Uri?>(null)}; var name by remember{mutableStateOf<String?>(null)}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri -> if(uri==null){Toast.makeText(context,"File selection cancelled",Toast.LENGTH_SHORT).show();return@rememberLauncherForActivityResult}; selected=uri; context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); name=uri.lastPathSegment }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){
        Card(colors = CardDefaults.cardColors(containerColor = HeroBg), shape = CardShape) { Text("Add Document", color = Color.White, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.titleLarge) }
        Text("Step 1: Choose member"); if(patients.isEmpty()) Text("No patients yet") else patients.forEach { p -> FilterChip(selected = patientId==p.id, onClick = { patientId = p.id }, label = { Text(p.fullName) }) }
        Text("Step 2: Select file"); Button(onClick={picker.launch(arrayOf("image/*","application/pdf"))}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Select JPG/PDF")}; Text(name?:"No file selected")
        Text("Step 3: Metadata"); OutlinedTextField(title,{title=it},label={Text("Title")}, modifier = Modifier.fillMaxWidth()); OutlinedTextField(date,{date=it},label={Text("Date")}, modifier = Modifier.fillMaxWidth()); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}, modifier = Modifier.fillMaxWidth()); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}, modifier = Modifier.fillMaxWidth());
        Text("Step 4: Category and notes"); OutlinedTextField(tags,{tags=it},label={Text("Category/Tags")}, modifier = Modifier.fillMaxWidth()); OutlinedTextField(notes,{notes=it},label={Text("Notes")}, modifier = Modifier.fillMaxWidth())
        Button(onClick={vm.addDocument(patientId,title,cat,date,doctor,clinic,tags,notes,selected,name){ok,msg -> Toast.makeText(context,msg,Toast.LENGTH_SHORT).show(); if(ok) onDone() }}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827))){Text("Save Document")}
    }
}

@Composable private fun DocumentDetailScreen(id:Long, vm:VaultVM, patients: List<PatientEntity>, onBack:()->Unit){ val docs by vm.allDocs.collectAsState(); val context=LocalContext.current; val d=docs.firstOrNull{it.id==id}; var ask by remember{mutableStateOf(false)}; var err by remember{mutableStateOf<String?>(null)}; if(d==null){Text("Not found"); return}
    val memberName = patients.firstOrNull { it.id == d.patientId }?.fullName ?: "Unknown member"
    Card(shape = CardShape, modifier = Modifier.padding(16.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Document Detail", style = MaterialTheme.typography.titleLarge)
        Text(d.title, fontWeight = FontWeight.Bold); Text("Member: $memberName"); Text("Category: ${d.category}"); Text("Clinic/Date: ${d.clinic} ${d.documentDate}"); Text("File: ${d.originalFileName.ifBlank { d.internalFileName }}")
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick={ val f=File(d.localPath); if(!f.exists()){err="File missing from local storage"; return@Button}; val uri=FileProvider.getUriForFile(context, context.packageName+".fileprovider", f); val intent=Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, if(d.fileType=="pdf")"application/pdf" else "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; runCatching{ context.startActivity(intent) }.onFailure{err="No app available to open this file"} }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Open")}
            OutlinedButton(onClick={ask=true}){Text("Delete")}; OutlinedButton(onClick=onBack){Text("Back")}
        }
        Text("Edit: planned for V2", color = Color.Gray)
    } }
    if(ask) AlertDialog(onDismissRequest={ask=false}, confirmButton={TextButton(onClick={vm.deleteDocument(d); ask=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={ask=false}){Text("Cancel")}}, text={Text("Delete document?")})
}

@Composable private fun VisitsScreen(patientId:Long, vm:VaultVM, onBack:()->Unit, onAdd:()->Unit){ var visits by remember{mutableStateOf(listOf<VisitEntity>())}; LaunchedEffect(patientId){ vm.visitsByPatient(patientId).collect{visits=it} }; Column(Modifier.padding(16.dp)){ Text("Visits"); Button(onClick=onAdd, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Add visit")}; if(visits.isEmpty()) Text("No visits yet") else LazyColumn{ items(visits){ Text("${it.visitDate} - ${it.doctor}") } }; OutlinedButton(onClick=onBack){Text("Back")}}
}
@Composable private fun VisitFormScreen(patientId:Long, vm:VaultVM, onDone:()->Unit){ var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var reason by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)){ Text("Add visit"); OutlinedTextField(date,{date=it},label={Text("Visit date")}); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}); OutlinedTextField(reason,{reason=it},label={Text("Reason")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); Button(onClick={vm.addVisit(patientId,date,doctor,clinic,reason,notes); onDone()}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Save")}}
}
@Composable private fun SearchScreen(vm:VaultVM, patients:List<PatientEntity>, onBack:()->Unit, onOpen:(Long)->Unit){ val scope = rememberCoroutineScope(); var q by remember{mutableStateOf("")}; var results by remember{mutableStateOf(listOf<MedicalDocumentEntity>())}; var from by remember{mutableStateOf("")}; var to by remember{mutableStateOf("")}; Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){ Text("Search", style = MaterialTheme.typography.headlineSmall); OutlinedTextField(q,{q=it},label={Text("Search text")}, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){ OutlinedTextField(from,{from=it},label={Text("From")}, modifier = Modifier.weight(1f)); OutlinedTextField(to,{to=it},label={Text("To")}, modifier = Modifier.weight(1f)) }; Button(onClick={ scope.launch { results = vm.search(q,null,null,from,to) } }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Search")}; if(results.isEmpty()) Text("No results yet. Try title, clinic, tags, or date range.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)){ items(results){ d ->
        val member = patients.firstOrNull { it.id == d.patientId }?.fullName ?: "Unknown member"
        Card(shape = CardShape, modifier = Modifier.fillMaxWidth().clickable{onOpen(d.id)}) { Column(Modifier.padding(12.dp)) { Text(d.title, fontWeight = FontWeight.Bold); Text(member, color = Color.Gray); Text("${d.category} • ${d.documentDate}") } }
    } }; OutlinedButton(onClick=onBack){Text("Back")}}
}
@Composable private fun SettingsScreen(vm:VaultVM, onBack:()->Unit){ var msg by remember{mutableStateOf("")}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){ uri -> if(uri!=null) vm.exportBackup(uri){ok-> msg= if(ok)"Backup exported" else "Backup failed. Check folder permissions and storage." } else msg="Export cancelled" }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)){ Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Card(shape = CardShape){ Column(Modifier.padding(12.dp)) { Text("Backup", fontWeight = FontWeight.Bold); Text("Export an encrypted local backup."); Button(onClick={picker.launch(null)}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)){Text("Export backup")}} }
        Card(shape = CardShape){ Column(Modifier.padding(12.dp)) { Text("Security", fontWeight = FontWeight.Bold); Text("All data stays on this device.") } }
        Card(shape = CardShape){ Column(Modifier.padding(12.dp)) { Text("About", fontWeight = FontWeight.Bold); Text("Medical Vault V1") } }
        if(msg.isNotBlank()) Text(msg)
        OutlinedButton(onClick=onBack){Text("Back")}
    }
}
