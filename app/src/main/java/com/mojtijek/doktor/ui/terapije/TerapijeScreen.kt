package com.mojtijek.doktor.ui.terapije

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val uzimanja by vm.svaUzimanja().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    
    // Calculate adherence (iOS: Adherencija - niz, 7d %)
    val aktivneTerapije = remember(terapije) { terapije.filter { it.aktivna } }
    val adherence7d = remember(uzimanja, aktivneTerapije) {
        if (aktivneTerapije.isEmpty()) return@remember 100.0
        val sedamDana = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val zadnjiTjedan = uzimanja.filter { it.ts >= sedamDana }
        if (zadnjiTjedan.isEmpty()) 100.0
        else (zadnjiTjedan.count { !it.preskoceno } / zadnjiTjedan.size.toDouble() * 100).coerceIn(0.0, 100.0)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj terapiju")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Terapije", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            
            // Adherence card (iOS: Adherencija niz, 7d %)
            if (aktivneTerapije.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Adherencija (7 dana)", style = MaterialTheme.typography.labelMedium)
                                    Text("${"%.0f".format(adherence7d)}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { (adherence7d / 100).toFloat() },
                                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${aktivneTerapije.size} aktivnih terapija",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (terapije.isEmpty()) {
                item {
                    Text(
                        "Nema unesenih terapija. Dodaj prvu pomoću + gumba.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(terapije) { t ->
                    TerapijaCard(t, onToggle = { vm.dodajTerapiju(it) }, onDelete = { vm.obrisiTerapiju(it) })
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
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dana = DoseSchedule.danaPreostalo(t)
    
    // Prescription expiry (iOS: Recept do)
    val receptIstice = remember(t.receptDo) {
        t.receptDo?.let { 
            val danaDoIsteka = ((it - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
            if (danaDoIsteka in 0..30) danaDoIsteka else null
        }
    }
    
    if (showEditDialog) {
        UrediTerapijuDialog(
            terapija = t,
            onDismiss = { showEditDialog = false },
            onSpremi = { onToggle(it); showEditDialog = false }
        )
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši terapiju?") },
            text = { Text("Jeste li sigurni da želite obrisati terapiju \"${t.naziv}\"? Ova radnja je nepovratna.") },
            confirmButton = {
                TextButton(onClick = { onDelete(t); showDeleteDialog = false }) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Odustani") } }
        )
    }
    
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
            
            // Stock level (iOS: Zaliha)
            if (dana != null) {
                Spacer(Modifier.height(6.dp))
                val boja = if (dana < 3) MaterialTheme.colorScheme.error else if (dana < 7) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                Text("Zaliha: ${"%.0f".format(dana)} dana", style = MaterialTheme.typography.labelMedium, color = boja)
            }
            
            // Prescription expiry (iOS: Recept)
            receptIstice?.let { dani ->
                Spacer(Modifier.height(4.dp))
                val boja = if (dani < 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                Text("Recept ističe za $dani dana", style = MaterialTheme.typography.labelSmall, color = boja)
            }
            
            // Ordered flag (iOS: Naručeno)
            if (t.narucenoTs != null) {
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = { onToggle(t.copy(narucenoTs = null)) },
                    label = { Text("Naručeno", style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            t.napomena?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(onClick = { showEditDialog = true }) { Text("Uredi") }
                if (dana != null && dana < 7 && t.narucenoTs == null) {
                    TextButton(onClick = { onToggle(t.copy(narucenoTs = System.currentTimeMillis())) }) {
                        Text("Označi naručenim")
                    }
                }
                TextButton(onClick = { showDeleteDialog = true }) { Text("Obriši") }
            }
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

@Composable
private fun UrediTerapijuDialog(terapija: TerapijaEntity, onDismiss: () -> Unit, onSpremi: (TerapijaEntity) -> Unit) {
    var naziv by remember { mutableStateOf(terapija.naziv) }
    var jacina by remember { mutableStateOf(terapija.jacina ?: "") }
    var oblik by remember { mutableStateOf(terapija.oblik) }
    var vremena by remember { mutableStateOf(terapija.vremena) }
    var dozaKom by remember { mutableStateOf(terapija.dozaKom.toString()) }
    var kolicina by remember { mutableStateOf(terapija.kolicina.toString()) }
    var napomena by remember { mutableStateOf(terapija.napomena ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uredi terapiju") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naziv, { naziv = it }, label = { Text("Naziv lijeka") }, singleLine = true)
                OutlinedTextField(jacina, { jacina = it }, label = { Text("Jačina (npr. 500mg)") }, singleLine = true)
                OutlinedTextField(oblik, { oblik = it }, label = { Text("Oblik (tableta, sirup...)") }, singleLine = true)
                OutlinedTextField(vremena, { vremena = it }, label = { Text("Vremena (08:00,20:00)") }, singleLine = true)
                OutlinedTextField(dozaKom, { dozaKom = it }, label = { Text("Doza po uzimanju") }, singleLine = true)
                OutlinedTextField(kolicina, { kolicina = it }, label = { Text("Trenutna zaliha (kom)") }, singleLine = true)
                OutlinedTextField(napomena, { napomena = it }, label = { Text("Napomena") }, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSpremi(
                        terapija.copy(
                            naziv = naziv,
                            jacina = jacina.ifBlank { null },
                            oblik = oblik,
                            vremena = vremena,
                            dozaKom = dozaKom.toDoubleOrNull() ?: terapija.dozaKom,
                            kolicina = kolicina.toDoubleOrNull() ?: terapija.kolicina,
                            napomena = napomena.ifBlank { null }
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
