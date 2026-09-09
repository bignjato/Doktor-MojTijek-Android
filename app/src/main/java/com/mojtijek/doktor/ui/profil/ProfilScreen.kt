package com.mojtijek.doktor.ui.profil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.ui.MojTijekViewModel

@Composable
fun ProfilScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj člana")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Obitelj", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (clanovi.isEmpty()) {
                Text("Nema dodanih članova. Dodaj prvog pomoću + gumba.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clanovi) { c ->
                        ClanCard(c, odabran = c.id == aktivniId, onOdaberi = { vm.odaberiClana(c.id) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("MojTijek Android · verzija sinkronizirana s iOS shared modelom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showDialog) {
        NoviClanDialog(onDismiss = { showDialog = false }, onSpremi = { ime -> vm.dodajClana(ime); showDialog = false })
    }
}

@Composable
private fun ClanCard(clan: ClanEntity, odabran: Boolean, onOdaberi: () -> Unit) {
    Card(onClick = onOdaberi) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(clan.ime, fontWeight = FontWeight.SemiBold)
                clan.lijecnik?.takeIf { it.isNotBlank() }?.let {
                    Text("Liječnik: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (odabran) AssistChip(onClick = {}, label = { Text("Aktivan") })
        }
    }
}

@Composable
private fun NoviClanDialog(onDismiss: () -> Unit, onSpremi: (String) -> Unit) {
    var ime by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi član obitelji") },
        text = { OutlinedTextField(ime, { ime = it }, label = { Text("Ime") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (ime.isNotBlank()) onSpremi(ime) }) { Text("Spremi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
