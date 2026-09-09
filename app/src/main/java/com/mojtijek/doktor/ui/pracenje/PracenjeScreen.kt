package com.mojtijek.doktor.ui.pracenje

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.MjerenjeEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

private val TIPOVI = listOf("težina", "tlak", "puls", "šećer", "temperatura")

@Composable
fun PracenjeScreen(vm: MojTijekViewModel) {
    val mjerenja by vm.mjerenjaZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Novo mjerenje")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Praćenje", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (mjerenja.isEmpty()) {
                Text("Nema unesenih mjerenja.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mjerenja) { m -> MjerenjeCard(m, onDelete = { vm.obrisiMjerenje(it) }) }
                }
            }
        }
    }

    if (showDialog) {
        NovoMjerenjeDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { m -> vm.dodajMjerenje(m); showDialog = false }
        )
    }
}

@Composable
private fun MjerenjeCard(m: MjerenjeEntity, onDelete: (MjerenjeEntity) -> Unit) {
    Card {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                val vrijednostText = if (m.vrijednost2 != null) "${m.vrijednost.toInt()}/${m.vrijednost2!!.toInt()}" else "${m.vrijednost}"
                Text("${m.tip}: $vrijednostText ${m.jedinica ?: ""}", fontWeight = FontWeight.SemiBold)
                Text(
                    SimpleDateFormat("d. MMM yyyy. HH:mm", Locale("hr", "HR")).format(Date(m.ts)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onDelete(m) }) { Text("Obriši") }
        }
    }
}

@Composable
private fun NovoMjerenjeDialog(clanId: String, onDismiss: () -> Unit, onSpremi: (MjerenjeEntity) -> Unit) {
    var tip by remember { mutableStateOf(TIPOVI.first()) }
    var vrijednost by remember { mutableStateOf("") }
    var vrijednost2 by remember { mutableStateOf("") }
    var jedinica by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo mjerenje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = tip, onValueChange = {}, readOnly = true,
                        label = { Text("Tip mjerenja") },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TIPOVI.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { tip = t; expanded = false })
                        }
                    }
                }
                OutlinedTextField(vrijednost, { vrijednost = it }, label = { Text(if (tip == "tlak") "Sistolički" else "Vrijednost") }, singleLine = true)
                if (tip == "tlak") {
                    OutlinedTextField(vrijednost2, { vrijednost2 = it }, label = { Text("Dijastolički") }, singleLine = true)
                }
                OutlinedTextField(jedinica, { jedinica = it }, label = { Text("Jedinica (kg, mmHg...)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = vrijednost.toDoubleOrNull()
                if (v != null) {
                    onSpremi(
                        MjerenjeEntity(
                            clanId = clanId, tip = tip, vrijednost = v,
                            vrijednost2 = vrijednost2.toDoubleOrNull(),
                            jedinica = jedinica.ifBlank { null }
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
