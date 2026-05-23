package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.example.localmedicalvault.backup.BackupManager
import com.example.localmedicalvault.data.local.*
import com.example.localmedicalvault.data.repo.VaultRepository
import com.example.localmedicalvault.storage.DocumentStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen { data object Home:Screen(); data class PatientDetail(val id:Long):Screen(); data class PatientForm(val id:Long?):Screen(); data class DocForm(val patientId:Long,val docId:Long?):Screen(); data class DocDetail(val id:Long):Screen(); data class VisitForm(val patientId:Long):Screen(); data class Visits(val patientId:Long):Screen(); data object Search:Screen(); data object Settings:Screen() }

class VaultVM(app: Application): AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addOrUpdatePatient(id:Long?,name:String,dob:String,notes:String,rel:String)=viewModelScope.launch { if(id==null) repo.addPatient(PatientEntity(fullName=name,dateOfBirth=dob,notes=notes,relationType=rel)) else repo.updatePatient(PatientEntity(id,name,dob,notes,rel)) }
    fun deletePatient(p:PatientEntity)=viewModelScope.launch{ repo.deletePatient(p) }
    fun addDocument(patientId:Long,title:String,cat:DocumentCategory,date:String,doctor:String,clinic:String,tags:String,notes:String,uri:Uri?,orig:String?)=viewModelScope.launch {
        if(uri==null) return@launch
        val (path,intName,type)=storage.importToPrivateStorage(uri,orig)
        repo.addDocument(MedicalDocumentEntity(patientId=patientId,title=title,category=cat,documentDate=date,doctorName=doctor,clinic=clinic,tags=tags,notes=notes,localPath=path,originalFileName=orig?:"",internalFileName=intName,fileType=type))
    }
    fun deleteDocument(doc:MedicalDocumentEntity)=viewModelScope.launch{ storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    fun addVisit(patientId:Long,date:String,doctor:String,clinic:String,reason:String,notes:String)=viewModelScope.launch{ repo.addVisit(VisitEntity(patientId=patientId,visitDate=date,doctor=doctor,clinic=clinic,reason=reason,notes=notes)) }
    fun visitsByPatient(patientId:Long)=repo.visitsByPatient(patientId)
    fun docsByPatient(patientId:Long)=repo.documentsByPatient(patientId)
    suspend fun patient(id:Long)=repo.getPatient(id)
    suspend fun doc(id:Long)=db.documentDao().getById(id)
    fun exportBackup(uri:Uri,onDone:(Boolean)->Unit)=viewModelScope.launch{ onDone(BackupManager(getApplication(),db).export(uri)) }
    suspend fun search(q:String,pid:Long?,cat:DocumentCategory?,from:String,to:String)=repo.search(q,pid,cat,from,to)
}

@Composable fun AppRoot(vm: VaultVM = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val patients by vm.patients.collectAsState()
    when(val s=screen) {
        Screen.Home -> Scaffold(floatingActionButton = { FloatingActionButton(onClick={screen=Screen.PatientForm(null)}){Text("+")}}) { p->
            Column(Modifier.padding(p).padding(16.dp)) { Text("Family Medical Vault", style=MaterialTheme.typography.headlineSmall); Button(onClick={screen=Screen.Search}){Text("Search")}; Button(onClick={screen=Screen.Settings}){Text("Settings")}
                LazyColumn { items(patients){ pat -> Text("${pat.fullName} (${pat.relationType})", modifier=Modifier.fillMaxWidth().clickable { screen=Screen.PatientDetail(pat.id) }.padding(10.dp)) } }
            }
        }
        is Screen.PatientForm -> PatientFormScreen(s.id, vm){screen=Screen.Home}
        is Screen.PatientDetail -> PatientDetailScreen(s.id, vm, onBack={screen=Screen.Home}, onEdit={screen=Screen.PatientForm(it)}, onDocs={screen=Screen.DocForm(it,null)}, onVisits={screen=Screen.Visits(it)})
        is Screen.DocForm -> DocumentFormScreen(s.patientId, vm){screen=Screen.Home}
        is Screen.DocDetail -> DocumentDetailScreen(s.id, vm, onBack={screen=Screen.Home})
        is Screen.Visits -> VisitsScreen(s.patientId, vm, onBack={screen=Screen.Home}, onAdd={screen=Screen.VisitForm(s.patientId)})
        is Screen.VisitForm -> VisitFormScreen(s.patientId, vm){screen=Screen.Visits(s.patientId)}
        Screen.Search -> SearchScreen(vm, onBack={screen=Screen.Home}, onOpen={screen=Screen.DocDetail(it)})
        Screen.Settings -> SettingsScreen(vm, onBack={screen=Screen.Home})
    }
}

@Composable private fun PatientFormScreen(id:Long?, vm:VaultVM, onDone:()->Unit){ var n by remember{mutableStateOf("")}; var dob by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var rel by remember{mutableStateOf("child")}; Column(Modifier.padding(16.dp)){ Text("Patient"); OutlinedTextField(n,{n=it},label={Text("Full name")}); OutlinedTextField(dob,{dob=it},label={Text("Date of birth")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); OutlinedTextField(rel,{rel=it},label={Text("Relation")}); Button(onClick={ vm.addOrUpdatePatient(id,n,dob,notes,rel); onDone()}){Text("Save")}}
}
@Composable private fun PatientDetailScreen(id:Long, vm:VaultVM, onBack:()->Unit,onEdit:(Long)->Unit,onDocs:(Long)->Unit,onVisits:(Long)->Unit){ val pList by vm.patients.collectAsState(); val p=pList.firstOrNull{it.id==id}; var askDelete by remember{mutableStateOf(false)}; Column(Modifier.padding(16.dp)){ Text(p?.fullName?:"Patient"); Text("DOB: ${p?.dateOfBirth}"); Text("Notes: ${p?.notes}"); Row{ Button(onClick={onEdit(id)}){Text("Edit")}; Spacer(Modifier.width(8.dp)); Button(onClick={onDocs(id)}){Text("Add Document")}; Spacer(Modifier.width(8.dp)); Button(onClick={onVisits(id)}){Text("Visits")}}; Button(onClick={askDelete=true}){Text("Delete")}; Button(onClick=onBack){Text("Back")}}
 if(askDelete && p!=null){ AlertDialog(onDismissRequest={askDelete=false}, confirmButton={TextButton(onClick={vm.deletePatient(p); askDelete=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={askDelete=false}){Text("Cancel")}}, text={Text("Delete patient?")}) }
}
@Composable private fun DocumentFormScreen(patientId:Long, vm:VaultVM, onDone:()->Unit){ var title by remember{mutableStateOf("")}; var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var tags by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; var cat by remember{mutableStateOf(DocumentCategory.OTHER)}; var selected by remember{mutableStateOf<Uri?>(null)}; var name by remember{mutableStateOf<String?>(null)}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri -> selected=uri; name=uri?.lastPathSegment }
 Column(Modifier.padding(16.dp)){ Text("Add document"); Button(onClick={picker.launch(arrayOf("image/*","application/pdf","*/*"))}){Text("Select file")}; Text(name?:"No file selected"); OutlinedTextField(title,{title=it},label={Text("Title")}); OutlinedTextField(date,{date=it},label={Text("Date")}); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}); OutlinedTextField(tags,{tags=it},label={Text("Tags")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); Button(onClick={vm.addDocument(patientId,title,cat,date,doctor,clinic,tags,notes,selected,name); onDone()}){Text("Save")}}
}
@Composable private fun DocumentDetailScreen(id:Long, vm:VaultVM, onBack:()->Unit){ val docs by vm.allDocs.collectAsState(); val context=LocalContext.current; val d=docs.firstOrNull{it.id==id}; var ask by remember{mutableStateOf(false)}; if(d==null){Text("Not found"); return}; Column(Modifier.padding(16.dp)){ Text(d.title); Text("${d.category} / ${d.documentDate}"); if(d.fileType=="image") Image(painter=rememberAsyncImagePainter(File(d.localPath)), contentDescription=null, modifier=Modifier.size(180.dp)); Button(onClick={ val intent=Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.fromFile(File(d.localPath)), if(d.fileType=="pdf")"application/pdf" else "*/*"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }; runCatching{ context.startActivity(intent)} }){Text("Open Document")}; Button(onClick={ask=true}){Text("Delete")}; Button(onClick=onBack){Text("Back")}}
 if(ask) AlertDialog(onDismissRequest={ask=false}, confirmButton={TextButton(onClick={vm.deleteDocument(d); ask=false; onBack()}){Text("Delete")}}, dismissButton={TextButton(onClick={ask=false}){Text("Cancel")}}, text={Text("Delete document?")})
}
@Composable private fun VisitsScreen(patientId:Long, vm:VaultVM, onBack:()->Unit, onAdd:()->Unit){ var visits by remember{mutableStateOf(listOf<VisitEntity>())}; LaunchedEffect(patientId){ vm.visitsByPatient(patientId).collect{visits=it} }; Column(Modifier.padding(16.dp)){ Text("Visits"); Button(onClick=onAdd){Text("Add visit")}; LazyColumn{ items(visits){ Text("${it.visitDate} - ${it.doctor}") } }; Button(onClick=onBack){Text("Back")}}
}
@Composable private fun VisitFormScreen(patientId:Long, vm:VaultVM, onDone:()->Unit){ var date by remember{mutableStateOf("")}; var doctor by remember{mutableStateOf("")}; var clinic by remember{mutableStateOf("")}; var reason by remember{mutableStateOf("")}; var notes by remember{mutableStateOf("")}; Column(Modifier.padding(16.dp)){ Text("Add visit"); OutlinedTextField(date,{date=it},label={Text("Visit date")}); OutlinedTextField(doctor,{doctor=it},label={Text("Doctor")}); OutlinedTextField(clinic,{clinic=it},label={Text("Clinic")}); OutlinedTextField(reason,{reason=it},label={Text("Reason")}); OutlinedTextField(notes,{notes=it},label={Text("Notes")}); Button(onClick={vm.addVisit(patientId,date,doctor,clinic,reason,notes); onDone()}){Text("Save")}}
}
@Composable private fun SearchScreen(vm:VaultVM, onBack:()->Unit, onOpen:(Long)->Unit){ val scope = rememberCoroutineScope(); var q by remember{mutableStateOf("")}; var results by remember{mutableStateOf(listOf<MedicalDocumentEntity>())}; var from by remember{mutableStateOf("")}; var to by remember{mutableStateOf("")}; Column(Modifier.padding(16.dp)){ Text("Search documents"); OutlinedTextField(q,{q=it},label={Text("Search text")}); OutlinedTextField(from,{from=it},label={Text("From date")}); OutlinedTextField(to,{to=it},label={Text("To date")}); Button(onClick={ scope.launch { results = vm.search(q,null,null,from,to) } }){Text("Search")}; LazyColumn{ items(results){ Text(it.title, modifier=Modifier.clickable{onOpen(it.id)}.padding(8.dp)) } }; Button(onClick=onBack){Text("Back")}}
}
@Composable private fun SettingsScreen(vm:VaultVM, onBack:()->Unit){ var msg by remember{mutableStateOf("")}; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){ uri -> if(uri!=null) vm.exportBackup(uri){ok-> msg= if(ok)"Backup exported" else "Backup failed" } }
 Column(Modifier.padding(16.dp)){ Text("Settings"); Text("App lock: coming in V2"); Text("Restore: placeholder for V2"); Button(onClick={picker.launch(null)}){Text("Export backup")}; Text(msg); Button(onClick=onBack){Text("Back")}}
}
