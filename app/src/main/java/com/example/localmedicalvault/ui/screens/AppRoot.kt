package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.localmedicalvault.backup.BackupManager
import com.example.localmedicalvault.data.local.AppDatabase
import com.example.localmedicalvault.data.local.DocumentCategory
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import com.example.localmedicalvault.data.local.PatientEntity
import com.example.localmedicalvault.data.local.VisitEntity
import com.example.localmedicalvault.data.repo.VaultRepository
import com.example.localmedicalvault.storage.DocumentStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data class PatientDetail(val id: Long) : Screen()
    data class PatientForm(val id: Long?) : Screen()
    data class DocForm(val patientId: Long, val docId: Long?) : Screen()
    data class DocDetail(val id: Long) : Screen()
    data class VisitForm(val patientId: Long) : Screen()
    data class Visits(val patientId: Long) : Screen()
    data object Search : Screen()
    data object Settings : Screen()
}

class VaultVM(app: Application) : AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addOrUpdatePatient(id: Long?, name: String, dob: String, notes: String, rel: String) = viewModelScope.launch {
        if (id == null) repo.addPatient(PatientEntity(fullName = name, dateOfBirth = dob, notes = notes, relationType = rel))
        else repo.updatePatient(PatientEntity(id, name, dob, notes, rel))
    }
    fun deletePatient(p: PatientEntity) = viewModelScope.launch { repo.deletePatient(p) }
    fun addDocument(patientId: Long, title: String, cat: DocumentCategory, date: String, doctor: String, clinic: String, tags: String, notes: String, uri: Uri?, orig: String?) = viewModelScope.launch {
        if (uri == null) return@launch
        val (path, intName, type) = storage.importToPrivateStorage(uri, orig)
        repo.addDocument(MedicalDocumentEntity(patientId = patientId, title = title, category = cat, documentDate = date, doctorName = doctor, clinic = clinic, tags = tags, notes = notes, localPath = path, originalFileName = orig ?: "", internalFileName = intName, fileType = type))
    }
    fun deleteDocument(doc: MedicalDocumentEntity) = viewModelScope.launch { storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    fun addVisit(patientId: Long, date: String, doctor: String, clinic: String, reason: String, notes: String) = viewModelScope.launch { repo.addVisit(VisitEntity(patientId = patientId, visitDate = date, doctor = doctor, clinic = clinic, reason = reason, notes = notes)) }
    fun visitsByPatient(patientId: Long) = repo.visitsByPatient(patientId)
    fun docsByPatient(patientId: Long) = repo.documentsByPatient(patientId)
    suspend fun search(q: String, pid: Long?, cat: DocumentCategory?, from: String, to: String) = repo.search(q, pid, cat, from, to)
    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch { onDone(BackupManager(getApplication(), db).export(uri)) }
}

@Composable
fun AppRoot(vm: VaultVM = viewModel()) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val patients by vm.patients.collectAsState()
    val docs by vm.allDocs.collectAsState()

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = screen is Screen.Home, onClick = { screen = Screen.Home }, icon = { Icon(Icons.Default.Home, null) }, label = { androidx.compose.material3.Text("Patients") })
            NavigationBarItem(selected = false, onClick = { screen = Screen.Home }, icon = { Icon(Icons.Default.Description, null) }, label = { androidx.compose.material3.Text("Documents") })
            NavigationBarItem(selected = screen is Screen.Visits || screen is Screen.VisitForm, onClick = { screen = patients.firstOrNull()?.let { Screen.Visits(it.id) } ?: Screen.Home }, icon = { Icon(Icons.Default.Today, null) }, label = { androidx.compose.material3.Text("Visits") })
            NavigationBarItem(selected = screen is Screen.Search, onClick = { screen = Screen.Search }, icon = { Icon(Icons.Default.Search, null) }, label = { androidx.compose.material3.Text("Search") })
            NavigationBarItem(selected = screen is Screen.Settings, onClick = { screen = Screen.Settings }, icon = { Icon(Icons.Default.Settings, null) }, label = { androidx.compose.material3.Text("Settings") })
        }
    }) { p ->
        when (val s = screen) {
            Screen.Home -> HomeScreen(p, patients, onAddPatient = { screen = Screen.PatientForm(null) }, onAddDocument = { pid -> screen = Screen.DocForm(pid, null) }, onOpenPatient = { screen = Screen.PatientDetail(it) }, docCountByPatient = { id -> docs.count { it.patientId == id } })
            is Screen.PatientForm -> PatientFormScreen(s.id, vm) { screen = Screen.Home }
            is Screen.PatientDetail -> PatientDetailScreen(s.id, vm, onBack = { screen = Screen.Home }, onEdit = { screen = Screen.PatientForm(it) }, onDocs = { screen = Screen.DocForm(it, null) }, onVisits = { screen = Screen.Visits(it) }, onOpenDoc = { screen = Screen.DocDetail(it) })
            is Screen.DocForm -> DocumentFormScreen(s.patientId, vm) { screen = Screen.PatientDetail(s.patientId) }
            is Screen.DocDetail -> DocumentDetailScreen(s.id, vm, onBack = { screen = Screen.Home })
            is Screen.Visits -> VisitsScreen(s.patientId, vm, onBack = { screen = Screen.Home }, onAdd = { screen = Screen.VisitForm(s.patientId) })
            is Screen.VisitForm -> VisitFormScreen(s.patientId, vm) { screen = Screen.Visits(s.patientId) }
            Screen.Search -> SearchScreen(vm, onOpen = { screen = Screen.DocDetail(it) })
            Screen.Settings -> SettingsScreen(vm)
        }
    }
}
