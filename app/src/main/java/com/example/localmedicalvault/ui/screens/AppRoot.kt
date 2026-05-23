package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

sealed class Screen { data object Home:Screen(); data object Documents:Screen(); data object AddWizard:Screen(); data object Search:Screen(); data object Settings:Screen(); data class PatientDetail(val id:Long):Screen(); data class PatientForm(val id:Long?):Screen(); data class DocDetail(val id:Long):Screen(); data class VisitForm(val patientId:Long):Screen(); data class Visits(val patientId:Long):Screen() }

class VaultVM(app: Application): AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addOrUpdatePatient(id:Long?,name:String,dob:String,notes:String,rel:String)=viewModelScope.launch { if(id==null) repo.addPatient(PatientEntity(fullName=name,dateOfBirth=dob,notes=notes,relationType=rel)) else repo.updatePatient(PatientEntity(id,name,dob,notes,rel)) }
    fun addDocument(patientId:Long,title:String,cat:DocumentCategory,date:String,doctor:String,clinic:String,tags:String,notes:String,uri:Uri?,orig:String?)=viewModelScope.launch { if(uri==null) return@launch; val (path,intName,type)=storage.importToPrivateStorage(uri,orig); repo.addDocument(MedicalDocumentEntity(patientId=patientId,title=title,category=cat,documentDate=date,doctorName=doctor,clinic=clinic,tags=tags,notes=notes,localPath=path,originalFileName=orig?:"",internalFileName=intName,fileType=type)) }
    fun deleteDocument(doc:MedicalDocumentEntity)=viewModelScope.launch{ storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    fun addVisit(patientId:Long,date:String,doctor:String,clinic:String,reason:String,notes:String)=viewModelScope.launch{ repo.addVisit(VisitEntity(patientId=patientId,visitDate=date,doctor=doctor,clinic=clinic,reason=reason,notes=notes)) }
    fun visitsByPatient(patientId:Long)=repo.visitsByPatient(patientId)
    suspend fun search(q:String,pid:Long?,cat:DocumentCategory?,from:String,to:String)=repo.search(q,pid,cat,from,to)
    fun exportBackup(uri:Uri,onDone:(Boolean)->Unit)=viewModelScope.launch{ onDone(BackupManager(getApplication(),db).export(uri)) }
}

@Composable fun AppRoot(vm: VaultVM = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val patients by vm.patients.collectAsState(); val docs by vm.allDocs.collectAsState()
    Scaffold(bottomBar={BottomNav(screen){screen=it}}) { p ->
        when(val s=screen){
            Screen.Home -> HomeScreen(p, patients, docs, onAdd={screen=Screen.AddWizard}, onOpenDoc={screen=Screen.DocDetail(it)}, onGoDocs={screen=Screen.Documents})
            Screen.Documents -> DocumentsListScreen(p, docs, patients, onOpen={screen=Screen.DocDetail(it)})
            Screen.AddWizard -> AddDocumentWizardScreen(p, vm, patients, onDone={screen=Screen.Documents})
            Screen.Search -> SearchScreen(p, vm, docs, onOpen={screen=Screen.DocDetail(it)})
            Screen.Settings -> SettingsScreen(p, vm)
            is Screen.DocDetail -> DocumentDetailScreen(s.id, vm, onBack={screen=Screen.Documents})
            is Screen.PatientDetail -> PatientDetailScreen(s.id, vm, onBack={screen=Screen.Home}, onEdit={screen=Screen.PatientForm(it)}, onDocs={}, onVisits={screen=Screen.Visits(it)}, onOpenDoc={screen=Screen.DocDetail(it)})
            is Screen.PatientForm -> PatientFormScreen(s.id, vm){screen=Screen.Home}
            is Screen.Visits -> VisitsScreen(s.patientId, vm, onBack={screen=Screen.Home}, onAdd={screen=Screen.VisitForm(s.patientId)})
            is Screen.VisitForm -> VisitFormScreen(s.patientId, vm){screen=Screen.Home}
        }
    }
}

@Composable private fun BottomNav(screen: Screen, onSelect:(Screen)->Unit){
    NavigationBar {
        NavigationBarItem(selected=screen is Screen.Home, onClick={onSelect(Screen.Home)}, icon={Icon(Icons.Default.Home,null)}, label={Text("Home")})
        NavigationBarItem(selected=screen is Screen.Documents, onClick={onSelect(Screen.Documents)}, icon={Icon(Icons.Default.Description,null)}, label={Text("Docs")})
        NavigationBarItem(selected=screen is Screen.AddWizard, onClick={onSelect(Screen.AddWizard)}, icon={Icon(Icons.Default.AddCircle,null)}, label={Text("Add")})
        NavigationBarItem(selected=screen is Screen.Search, onClick={onSelect(Screen.Search)}, icon={Icon(Icons.Default.Search,null)}, label={Text("Search")})
        NavigationBarItem(selected=screen is Screen.Settings, onClick={onSelect(Screen.Settings)}, icon={Icon(Icons.Default.Settings,null)}, label={Text("Settings")})
    }
}
