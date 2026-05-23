package com.example.localmedicalvault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localmedicalvault.ui.components.InfoCard
import com.example.localmedicalvault.ui.components.SecondaryAction

@Composable
fun SettingsScreen(vm: VaultVM) { var msg by remember { mutableStateOf("") }; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.exportBackup(uri) { ok -> msg = if (ok) "Backup exported" else "Backup failed" } }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Settings", style = MaterialTheme.typography.headlineSmall); InfoCard("Privacy", "All data stays on this device"); InfoCard("Backup", "Export local backup") { SecondaryAction("Export backup") { picker.launch(null) } }; InfoCard("Security", "App lock — planned for V2"); InfoCard("About", "This app helps organize medical documents and does not replace medical advice."); if (msg.isNotBlank()) Text(msg) }
}
