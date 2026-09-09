package com.mojtijek.doktor.ui.terapije

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
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.data.TerapijaEntity
import com.mojtijek.doktor.ui.MojTijekViewModel

@Composable
fun TerapijeScreen(vm: MojTijekViewModel) {
    val terapije by vm.sveTerapijeZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj terapiju")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Terapije", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (terapije.isEmpty()) {
                Text(
                    "Nema unesenih terapija. Dodaj prvu pomoću + gumba.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(terapije) { t -> TerapijaCard(t, onToggle = { vm.dodajTerapiju(it) }, onDelete = { vm.obrisiTerapiju(it) }) }
                }
            }
        }
    }

    if (showDialog) {
        NovaTerapijaDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { t -> vm.dodajTerapiju(t); showDialog = false }
        )
    }
}

@Composable
private fun TerapijaCard(t: TerapijaEntity, onToggle: (TerapijaEntity) -> Unit, onDelete: (TerapijaEntity) -> Unit) {
    val dana = DoseSchedule.danaPreostalo(t)
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.naziv, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(t.jacina, t.oblik, t.vremena.ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = t.aktivna, onCheckedChange = { onToggle(t.copy(aktivna = it)) })
            }
            if (dana != null) {
                Spacer(Modifier.height(6.dp))
                val boja = if (dana < 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text("Zaliha: ${"%.0f".format(dana)} dana", style = MaterialTheme.typography.labelMedium, color = boja)
            }
            t.napomena?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onDelete(t) }) { Text("Obriši") }
        }
    }
}

@Composable
private fun NovaTerapijaDialog(clanId: String, onDismiss: () -> Unit, onSpremi: (TerapijaEntity) -> Unit) {
    var naziv by remember { mutableStateOf("") }
    var jacina by remember { mutableStateOf("") }
    var oblik by remember { mutableStateOf("tableta") }
    var vremena by remember { mutableStateOf("08:00") }
    var dozaKom by remember { mutableStateOf("1") }
    var komPoKutiji by remember { mutableStateOf("30") }
    var kolicina by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova terapija") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naziv, { naziv = it }, label = { Text("Naziv lijeka") }, singleLine = true)
                OutlinedTextField(jacina, { jacina = it }, label = { Text("Jačina (npr. 500mg)") }, singleLine = true)
                OutlinedTextField(oblik, { oblik = it }, label = { Text("Oblik (tableta, sirup...)") }, singleLine = true)
                OutlinedTextField(vremena, { vremena = it }, label = { Text("Vremena (08:00,20:00)") }, singleLine = true)
                OutlinedTextField(dozaKom, { dozaKom = it }, label = { Text("Doza po uzimanju") }, singleLine = true)
                OutlinedTextField(kolicina, { kolicina = it }, label = { Text("Trenutna zaliha (kom)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSpremi(
                        TerapijaEntity(
                            clanId = clanId,
                            naziv = naziv,
                            jacina = jacina.ifBlank { null },
                            oblik = oblik.ifBlank { "tableta" },
                            vremena = vremena,
                            dozaKom = dozaKom.toDoubleOrNull() ?: 1.0,
                            komPoKutiji = komPoKutiji.toDoubleOrNull() ?: 30.0,
                            kolicina = kolicina.toDoubleOrNull() ?: 0.0
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
