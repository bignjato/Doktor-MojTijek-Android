package com.mojtijek.doktor.ui.kalendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.DogadjajEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KalendarScreen(vm: MojTijekViewModel) {
    val dogadjaji by vm.dogadjajiZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val sortirano = remember(dogadjaji) { dogadjaji.sortedBy { it.datum } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj pregled")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Pregledi i termini", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (sortirano.isEmpty()) {
                Text("Nema zakazanih pregleda.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortirano) { d -> DogadjajCard(d, onDone = { vm.azurirajDogadjaj(it.copy(status = "obavljeno")) }, onDelete = { vm.obrisiDogadjaj(it) }) }
                }
            }
        }
    }

    if (showDialog) {
        NoviDogadjajDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { d -> vm.dodajDogadjaj(d); showDialog = false }
        )
    }
}

@Composable
private fun DogadjajCard(d: DogadjajEntity, onDone: (DogadjajEntity) -> Unit, onDelete: (DogadjajEntity) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.naslov, fontWeight = FontWeight.SemiBold)
                    Text(
                        SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(d.datum)) +
                            (d.vrijeme?.let { " · $it" } ?: "") +
                            (d.lokacija?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(onClick = {}, label = { Text(d.status) })
            }
            if (d.uputnicaPotrebna) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (d.uputnicaIzdana) "Uputnica izdana" else "Uputnica potrebna",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (d.uputnicaIzdana) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Row {
                TextButton(onClick = { onDone(d) }) { Text("Obavljeno") }
                TextButton(onClick = { onDelete(d) }) { Text("Obriši") }
            }
        }
    }
}

@Composable
private fun NoviDogadjajDialog(clanId: String, onDismiss: () -> Unit, onSpremi: (DogadjajEntity) -> Unit) {
    var naslov by remember { mutableStateOf("") }
    var lokacija by remember { mutableStateOf("") }
    var vrijeme by remember { mutableStateOf("") }
    var uputnica by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi pregled/termin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naslov, { naslov = it }, label = { Text("Naslov (npr. Kardiolog)") }, singleLine = true)
                OutlinedTextField(lokacija, { lokacija = it }, label = { Text("Lokacija") }, singleLine = true)
                OutlinedTextField(vrijeme, { vrijeme = it }, label = { Text("Vrijeme (HH:mm)") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = uputnica, onCheckedChange = { uputnica = it })
                    Text("Potrebna uputnica")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naslov.isNotBlank()) {
                    onSpremi(
                        DogadjajEntity(
                            clanId = clanId,
                            naslov = naslov,
                            lokacija = lokacija.ifBlank { null },
                            vrijeme = vrijeme.ifBlank { null },
                            uputnicaPotrebna = uputnica
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
