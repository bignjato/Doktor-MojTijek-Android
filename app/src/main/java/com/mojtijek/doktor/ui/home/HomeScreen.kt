package com.mojtijek.doktor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.data.TerapijaEntity
import com.mojtijek.doktor.data.UzimanjeEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val terapije by vm.terapijeZaAktivnog().collectAsState(initial = emptyList())
    val uzimanja by vm.uzimanjaDanas().collectAsState(initial = emptyList())

    val danas = DoseSchedule.dayStart()
    val doze = remember(terapije) { DoseSchedule.entriesForDay(terapije, danas) }
    val uzetiSetovi = remember(uzimanja) { uzimanja.map { "${it.terapijaId}|${it.slot}" }.toSet() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Moj dan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        if (clanovi.isEmpty()) {
            EmptyState()
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            clanovi.forEach { clan ->
                FilterChip(
                    selected = clan.id == aktivniId,
                    onClick = { vm.odaberiClana(clan.id) },
                    label = { Text(clan.ime) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (doze.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Nema planiranih doza za danas", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val preostaleDoze = doze.count { "${it.terapija.id}|${it.slot}" !in uzetiSetovi }
            Text(
                "$preostaleDoze od ${doze.size} doza preostalo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(doze) { dose ->
                    val key = "${dose.terapija.id}|${dose.slot}"
                    val uzeto = key in uzetiSetovi
                    DoseCard(
                        terapija = dose.terapija,
                        slot = dose.slot,
                        uzeto = uzeto,
                        onPotvrdi = { vm.potvrdiDozu(dose.terapija, dose.slot) },
                        onPreskoci = { vm.preskociDozu(dose.terapija, dose.slot) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DoseCard(
    terapija: TerapijaEntity,
    slot: String,
    uzeto: Boolean,
    onPotvrdi: () -> Unit,
    onPreskoci: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(terapija.naziv, fontWeight = FontWeight.SemiBold)
                Text(
                    "$slot" + (terapija.jacina?.let { " · $it" } ?: "") + " · ${terapija.oblik}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uzeto) {
                AssistChip(onClick = {}, label = { Text("Uzeto") }, leadingIcon = {
                    Icon(Icons.Filled.Check, contentDescription = null)
                })
            } else {
                IconButton(onClick = onPreskoci) { Icon(Icons.Filled.Close, contentDescription = "Preskoči") }
                FilledIconButton(onClick = onPotvrdi) { Icon(Icons.Filled.Check, contentDescription = "Uzeto") }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dodaj prvog člana obitelji", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Otvori Profil da započneš",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
