package com.example.localmedicalvault.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

class VaultVM(app: Application): AndroidViewModel(app) {
    private val repo = VaultRepository(app)
    private val storage = DocumentStorage(app)
    private val db = AppDatabase.get(app)
    val patients = repo.observePatients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDocs = repo.allDocuments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addDocument(patientId:Long,title:String,cat:DocumentCategory,date:String,doctor:String,clinic:String,tags:String,notes:String,uri:Uri?,orig:String?)=viewModelScope.launch { if(uri==null) return@launch; val (path,intName,type)=storage.importToPrivateStorage(uri,orig); repo.addDocument(MedicalDocumentEntity(patientId=patientId,title=title,category=cat,documentDate=date,doctorName=doctor,clinic=clinic,tags=tags,notes=notes,localPath=path,originalFileName=orig?:"",internalFileName=intName,fileType=type)) }
    fun deleteDocument(doc:MedicalDocumentEntity)=viewModelScope.launch{ storage.deleteLocalFile(doc.localPath); repo.deleteDocument(doc) }
    suspend fun search(q:String,pid:Long?,cat:DocumentCategory?,from:String,to:String)=repo.search(q,pid,cat,from,to)
    fun exportBackup(uri:Uri,onDone:(Boolean)->Unit)=viewModelScope.launch{ onDone(BackupManager(getApplication(),db).export(uri)) }
}

@Composable
fun AppRoot(vm: VaultVM = viewModel()) {
    var currentTab by remember { mutableStateOf("home") }
    var selectedDocId by remember { mutableStateOf<Long?>(null) }
    val patients by vm.patients.collectAsState()
    val docs by vm.allDocs.collectAsState()

    Scaffold(bottomBar = { if (selectedDocId == null) BottomNavBar(currentTab) { currentTab = it } }, containerColor = Color(0xFFF8FAFC)) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (selectedDocId != null) {
                DocumentDetailScreen(id = selectedDocId!!, vm = vm, onBack = { selectedDocId = null })
            } else {
                when (currentTab) {
                    "home" -> HomeScreen(PaddingValues(0.dp), patients, docs, onAdd = { currentTab = "add" }, onOpenDoc = { selectedDocId = it }, onGoDocs = { currentTab = "documents" })
                    "documents" -> DocumentsListScreen(PaddingValues(0.dp), docs, patients, onOpen = { selectedDocId = it })
                    "add" -> AddDocumentWizard(PaddingValues(0.dp), vm, patients, onFinish = { currentTab = "documents" })
                    "search" -> SearchScreen(PaddingValues(0.dp), vm, docs, onOpen = { selectedDocId = it })
                    "settings" -> SettingsScreen(PaddingValues(0.dp), vm)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentTab: String, onTabSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        NavItem("Home", Icons.Rounded.Home, currentTab == "home") { onTabSelected("home") }
        NavItem("Docs", Icons.Rounded.Description, currentTab == "documents") { onTabSelected("documents") }
        Box(modifier = Modifier.size(56.dp).offset(y = (-12).dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F172A)).clickable { onTabSelected("add") }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(32.dp)) }
        NavItem("Search", Icons.Rounded.Search, currentTab == "search") { onTabSelected("search") }
        NavItem("Settings", Icons.Rounded.Settings, currentTab == "settings") { onTabSelected("settings") }
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 11.sp, color = color)
    }
}
