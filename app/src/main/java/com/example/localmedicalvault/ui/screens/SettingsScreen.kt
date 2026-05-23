package com.example.localmedicalvault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun SettingsScreen(padding: PaddingValues, vm: VaultVM) { var msg by remember { mutableStateOf("") }; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.exportBackup(uri) { ok -> msg = if (ok) "Backup exported" else "Backup failed" } }
    Column(Modifier.padding(padding).padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("Settings", style=MaterialTheme.typography.headlineSmall); Text("Simple MVP settings", color=MaterialTheme.colorScheme.onSurfaceVariant)
        ElevatedCard{Column(Modifier.padding(14.dp)){Text("Privacy", style=MaterialTheme.typography.titleMedium); Text("All data stays on this device.")}}
        ElevatedCard{Column(Modifier.padding(14.dp)){Text("Backup", style=MaterialTheme.typography.titleMedium); Text("Export local backup"); TextButton(onClick={picker.launch(null)}){Text("Export")}}}
        ElevatedCard{Column(Modifier.padding(14.dp)){Text("Security", style=MaterialTheme.typography.titleMedium); Text("App lock — planned for V2")}}
        if(msg.isNotBlank()) Text(msg)
    }
}
