package com.example.localmedicalvault.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.data.local.MedicalDocumentEntity
import kotlinx.coroutines.launch

@Composable fun SearchScreen(padding: PaddingValues, vm: VaultVM, docs: List<MedicalDocumentEntity>, onOpen:(Long)->Unit){
    val scope= rememberCoroutineScope(); var q by remember{mutableStateOf("")}; var results by remember{mutableStateOf(docs)}
    Column(Modifier.padding(padding).padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)){ Text("Search", style=MaterialTheme.typography.headlineSmall); Text("Find documents fast", color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(q,{q=it; scope.launch{results=vm.search(q,null,null,"","")}}, modifier=Modifier.fillMaxWidth(), placeholder={Text("vitamin D, prescription...")}, leadingIcon={Icon(Icons.Default.Search,null)}, trailingIcon={ if(q.isNotBlank()) IconButton(onClick={q="";results=docs}){Icon(Icons.Default.Clear,null)} })
        if(results.isEmpty()) Text("No documents found.")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){ items(results){ DocumentRowCard(it,onOpen) } }
    }
}
