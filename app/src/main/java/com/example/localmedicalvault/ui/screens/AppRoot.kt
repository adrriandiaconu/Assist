package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

sealed class Screen { data object Home:Screen(); data class PatientDetail(val id:Long):Screen(); data class PatientForm(val id:Long?):Screen(); data class DocForm(val patientId:Long?):Screen(); data class DocDetail(val id:Long):Screen(); data class VisitForm(val patientId:Long):Screen(); data class Visits(val patientId:Long):Screen(); data object Search:Screen(); data object Settings:Screen() }

class VaultVM(app: Application): AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addOrUpdatePatient(id:Long?,name:String,dob:String,notes:String,rel:String)=viewModelScope.launch { if(name.isBlank()) return@launch; if(id==null) repo.addPatient(PatientEntity(fullName=name.trim(),dateOfBirth=dob.trim(),notes=notes.trim(),relationType=rel.trim())) else repo.updatePatient(PatientEntity(id,name.trim(),dob.trim(),notes.trim(),rel.trim())) }
    fun deletePatient(p:PatientEntity)=viewModelScope.launch{ repo.deletePatient(p) }
    fun addDocument(patientId:Long?,title:String,cat:DocumentCategory,date:String,doctor:String,clinic:String,tags:String,notes:String,uri:Uri?,orig:String?,onResult:(Boolean,String)->Unit)=viewModelScope.launch { if(patientId == null) return@launch onResult(false, "Please select a family member"); if(uri==null) return@launch onResult(false, "Please select a file"); runCatching { storage.importToPrivateStorage(uri,orig) }.onFailure { onResult(false, "File import failed") }.onSuccess { (path,intName,type) -> repo.addDocument(MedicalDocumentEntity(patientId=patientId,title=title.ifBlank { orig ?: "Untitled" },category=cat,documentDate=date,doctorName=doctor,clinic=clinic,tags=tags,notes=notes,localPath=path,originalFileName=orig?:"",internalFileName=intName,fileType=type)); onResult(true, "Saved") } }
    fun deleteDocument(doc:MedicalDocumentEntity)=viewModelScope.launch{ storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    fun addVisit(patientId:Long,date:String,doctor:String,clinic:String,reason:String,notes:String)=viewModelScope.launch{ repo.addVisit(VisitEntity(patientId=patientId,visitDate=date,doctor=doctor,clinic=clinic,reason=reason,notes=notes)) }
    fun visitsByPatient(patientId:Long)=repo.visitsByPatient(patientId)
    fun docsByPatient(patientId:Long)=repo.documentsByPatient(patientId)
    fun exportBackup(uri:Uri,onDone:(Boolean)->Unit)=viewModelScope.launch{ onDone(BackupManager(getApplication(),db).export(uri)) }
    suspend fun search(q:String,pid:Long?,cat:DocumentCategory?,from:String,to:String)=repo.search(q,pid,cat,from,to)
}

private val bg = Color(0xFF0E131C)
private val card = Color(0xFF1A2230)

@Composable fun AppRoot(vm: VaultVM = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val patients by vm.patients.collectAsState()
    val docs by vm.allDocs.collectAsState()
    val content: @Composable () -> Unit = {
        when(val s=screen) {
            Screen.Home -> HomeScreen(patients, docs, onOpenPatient = { screen = Screen.PatientDetail(it) }, onOpenDoc = { screen = Screen.DocDetail(it) })
            is Screen.PatientForm -> PatientFormScreen(s.id, vm){screen=Screen.Home}
            is Screen.PatientDetail -> PatientDetailScreen(s.id, vm, onBack={screen=Screen.Home}, onEdit={screen=Screen.PatientForm(it)}, onDocs={screen=Screen.DocForm(it)}, onVisits={screen=Screen.Visits(it)}, onOpenDoc={screen=Screen.DocDetail(it)})
            is Screen.DocForm -> DocumentFormScreen(s.patientId, vm, patients){screen=Screen.Home}
            is Screen.DocDetail -> DocumentDetailScreen(s.id, vm, onBack={screen=Screen.Home})
            is Screen.Visits -> VisitsScreen(s.patientId, vm, onBack={screen=Screen.PatientDetail(s.patientId)}, onAdd={screen=Screen.VisitForm(s.patientId)})
            is Screen.VisitForm -> VisitFormScreen(s.patientId, vm){screen=Screen.Visits(s.patientId)}
            Screen.Search -> SearchScreen(vm, onBack={screen=Screen.Home}, onOpen={screen=Screen.DocDetail(it)})
            Screen.Settings -> SettingsScreen(vm, onBack={screen=Screen.Home})
        }
    }
    Scaffold(containerColor = bg, contentWindowInsets = WindowInsets.safeDrawing, floatingActionButton = {
        FloatingActionButton(onClick = { screen = Screen.DocForm(null) }, containerColor = Color.White, contentColor = Color.Black) { Icon(Icons.Default.Add, null) }
    }, floatingActionButtonPosition = FabPosition.Center, bottomBar = {
        NavigationBar(containerColor = card, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
            NavItem("Home", Icons.Default.Home) { screen = Screen.Home }
            NavItem("Docs", Icons.Default.Description) { screen = Screen.Home }
            NavigationBarItem(selected = false, onClick = { screen = Screen.DocForm(null) }, icon = { Icon(Icons.Default.Add, null) }, label = { Text("Add") })
            NavItem("Search", Icons.Default.Search) { screen = Screen.Search }
            NavItem("Settings", Icons.Default.Settings) { screen = Screen.Settings }
        }
    }) { p -> Box(Modifier.fillMaxSize().padding(p)) { content() } }
}

@Composable private fun NavItem(label:String, icon: androidx.compose.ui.graphics.vector.ImageVector, click:()->Unit) { NavigationBarItem(selected = false, onClick = click, icon = { Icon(icon, null) }, label = { Text(label) }) }

@Composable private fun HomeScreen(patients: List<PatientEntity>, docs: List<MedicalDocumentEntity>, onOpenPatient:(Long)->Unit, onOpenDoc:(Long)->Unit) {
    LazyColumn(Modifier.fillMaxSize().background(bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Family Medical Vault", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Fast. Clear. Mobile. Simple.", color = Color(0xFFB7C2D1)) }
        item { Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp)) { Text("Original documents are the truth", color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text("Your family medical archive, always in your pocket.", color = Color(0xFFD6DEEA)) } } }
        item { Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("+ Add document") } }
        item { Text("Family", color = Color.White, style = MaterialTheme.typography.titleLarge) }
        if (patients.isEmpty()) item { Text("No family members yet.", color = Color(0xFFB7C2D1)) }
        items(patients) { pat -> Card(colors = CardDefaults.cardColors(containerColor = card), modifier = Modifier.fillMaxWidth().clickable { onOpenPatient(pat.id) }) { Column(Modifier.padding(12.dp)) { Text(pat.fullName, color = Color.White); Text(pat.relationType.ifBlank { "Family" }, color = Color(0xFFB7C2D1)) } } }
        item { Text("Recent documents", color = Color.White, style = MaterialTheme.typography.titleLarge) }
        if (docs.isEmpty()) item { Text("No documents yet.", color = Color(0xFFB7C2D1)) }
        items(docs.take(5)) { d -> Text(d.title, color = Color.White, modifier = Modifier.fillMaxWidth().clickable { onOpenDoc(d.id) }.padding(8.dp)) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable private fun PatientFormScreen(id:Long?, vm:VaultVM, onDone:()->Unit){ var n by remember{mutableStateOf("")}; var dob by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var rel by remember{mutableStateOf("child")}; FormWrap("Family member") { OutlinedTextField(n,{n=it},label={Text("Full name")}); OutlinedTextField(dob,{dob=it},label={Text("Date of birth")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); OutlinedTextField(rel,{rel=it},label={Text("Relation")}); Button(onClick={ vm.addOrUpdatePatient(id,n,dob,notes,rel); onDone()}){Text("Save")}} }
@Composable private fun PatientDetailScreen(id:Long, vm:VaultVM, onBack:()->Unit,onEdit:(Long)->Unit,onDocs:(Long)->Unit,onVisits:(Long)->Unit,onOpenDoc:(Long)->Unit){ val pList by vm.patients.collectAsState(); val p=pList.firstOrNull{it.id==id}; var askDelete by remember{mutableStateOf(false)}; var docs by remember{mutableStateOf(listOf<MedicalDocumentEntity>())}; var visits by remember{mutableStateOf(listOf<VisitEntity>())}; LaunchedEffect(id){ vm.docsByPatient(id).collect{docs=it} }; LaunchedEffect(id){ vm.visitsByPatient(id).collect{visits=it} }
FormWrap(p?.fullName ?: "Family member") { Text("DOB: ${p?.dateOfBirth ?: "-"}", color = Color.White); Row { Button(onClick={onEdit(id)}){Text("Edit")}; Spacer(Modifier.width(8.dp)); Button(onClick={onDocs(id)}){Text("Add document")}; Spacer(Modifier.width(8.dp)); Button(onClick={onVisits(id)}){Text("Visits")}}; Text("Documents", color=Color.White); docs.forEach { Text(it.title, color=Color.White, modifier=Modifier.clickable { onOpenDoc(it.id) }) }; Text("Recent visits", color=Color.White); visits.take(5).forEach { Text("${it.visitDate} - ${it.doctor}", color=Color.White) }; Button(onClick={askDelete=true}){Text("Delete")}; TextButton(onClick=onBack){Text("Back")}}
 if(askDelete && p!=null){ AlertDialog(onDismissRequest={askDelete=false}, confirmButton={TextButton(onClick={vm.deletePatient(p); askDelete=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={askDelete=false}){Text("Cancel")}}, text={Text("Delete family member?")}) } }
@Composable private fun DocumentFormScreen(defaultPatientId:Long?, vm:VaultVM, patients:List<PatientEntity>, onDone:()->Unit){ val context=LocalContext.current; var patientId by remember{mutableStateOf(defaultPatientId)}; var title by remember{mutableStateOf("")}; var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var tags by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var cat by remember{mutableStateOf(DocumentCategory.OTHER)}; var selected by remember{mutableStateOf<Uri?>(null)}; var name by remember{mutableStateOf<String?>(null)}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri -> if(uri==null){Toast.makeText(context,"File selection cancelled",Toast.LENGTH_SHORT).show();return@rememberLauncherForActivityResult}; selected=uri; context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); name=uri.lastPathSegment }
FormWrap("Add document") { patients.forEach { p -> Text((if(patientId==p.id) "• " else "") + p.fullName, color = Color.White, modifier = Modifier.clickable { patientId = p.id }.padding(2.dp)) }; Button(onClick={picker.launch(arrayOf("image/*","application/pdf"))}){Text("Select file")}; Text(name?:"No file selected", color=Color.White); OutlinedTextField(title,{title=it},label={Text("Title")}); OutlinedTextField(date,{date=it},label={Text("Date")}); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}); OutlinedTextField(tags,{tags=it},label={Text("Category/Tags")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); Button(onClick={vm.addDocument(patientId,title,cat,date,doctor,clinic,tags,notes,selected,name){ok,msg -> Toast.makeText(context,msg,Toast.LENGTH_SHORT).show(); if(ok) onDone() }}){Text("Save")}} }
@Composable private fun DocumentDetailScreen(id:Long, vm:VaultVM, onBack:()->Unit){ val docs by vm.allDocs.collectAsState(); val context=LocalContext.current; val d=docs.firstOrNull{it.id==id}; var ask by remember{mutableStateOf(false)}; var err by remember{mutableStateOf<String?>(null)}; if(d==null){Text("Not found", color = Color.White); return}; FormWrap(d.title){ Text("${d.category} / ${d.documentDate}", color=Color.White); err?.let { Text(it, color = MaterialTheme.colorScheme.error) }; Button(onClick={ val f=File(d.localPath); if(!f.exists()){err="File missing from local storage"; return@Button}; val uri=FileProvider.getUriForFile(context, context.packageName+".fileprovider", f); val intent=Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, if(d.fileType=="pdf")"application/pdf" else "image/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; runCatching{ context.startActivity(intent) }.onFailure{err="No app available to open this file"} }){Text("Open document")}; Button(onClick={ask=true}){Text("Delete")}; TextButton(onClick=onBack){Text("Back")}}
 if(ask) AlertDialog(onDismissRequest={ask=false}, confirmButton={TextButton(onClick={vm.deleteDocument(d); ask=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={ask=false}){Text("Cancel")}}, text={Text("Delete document?")}) }
@Composable private fun VisitsScreen(patientId:Long, vm:VaultVM, onBack:()->Unit, onAdd:()->Unit){ var visits by remember{mutableStateOf(listOf<VisitEntity>())}; LaunchedEffect(patientId){ vm.visitsByPatient(patientId).collect{visits=it} }; FormWrap("Visits") { Button(onClick=onAdd){Text("Add visit")}; if(visits.isEmpty()) Text("No visits yet", color=Color.White) else LazyColumn{ items(visits){ Text("${it.visitDate} - ${it.doctor}", color=Color.White) } }; TextButton(onClick=onBack){Text("Back")}} }
@Composable private fun VisitFormScreen(patientId:Long, vm:VaultVM, onDone:()->Unit){ var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var reason by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; FormWrap("Add visit") { OutlinedTextField(date,{date=it},label={Text("Visit date")}); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}); OutlinedTextField(reason,{reason=it},label={Text("Reason")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); Button(onClick={vm.addVisit(patientId,date,doctor,clinic,reason,notes); onDone()}){Text("Save")}} }
@Composable private fun SearchScreen(vm:VaultVM, onBack:()->Unit, onOpen:(Long)->Unit){ val scope = rememberCoroutineScope(); var q by remember{mutableStateOf("")}; var results by remember{mutableStateOf(listOf<MedicalDocumentEntity>())}; var from by remember{mutableStateOf("")}; var to by remember{mutableStateOf("")}; FormWrap("Search documents") { OutlinedTextField(q,{q=it},label={Text("Search text")}); OutlinedTextField(from,{from=it},label={Text("From date")}); OutlinedTextField(to,{to=it},label={Text("To date")}); Button(onClick={ scope.launch { results = vm.search(q,null,null,from,to) } }){Text("Search")}; if(results.isEmpty()) Text("No search results", color=Color.White) else LazyColumn{ items(results){ Text(it.title, color=Color.White, modifier=Modifier.clickable{onOpen(it.id)}.padding(8.dp)) } }; TextButton(onClick=onBack){Text("Back")}} }
@Composable private fun SettingsScreen(vm:VaultVM, onBack:()->Unit){ var msg by remember{mutableStateOf("")}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){ uri -> if(uri!=null) vm.exportBackup(uri){ok-> msg= if(ok)"Backup exported" else "Backup failed. Check folder permissions and storage." } else msg="Export cancelled" }
 FormWrap("Settings") { Button(onClick={picker.launch(null)}){Text("Export backup")}; Text(msg, color=Color.White); TextButton(onClick=onBack){Text("Back")}} }

@Composable private fun FormWrap(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) { Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(10.dp)); Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}
