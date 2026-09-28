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
    val aktivniClan = remember(clanovi, aktivniId) { clanovi.find { it.id == aktivniId } }
    val terapije by vm.terapijeZaAktivnog().collectAsState(initial = emptyList())
    val uzimanja by vm.uzimanjaDanas().collectAsState(initial = emptyList())
    val dogadjaji by vm.dogadjajiZaAktivnog().collectAsState(initial = emptyList())

    val danas = DoseSchedule.dayStart()
    val doze = remember(terapije) { DoseSchedule.entriesForDay(terapije, danas) }
    val uzetiSetovi = remember(uzimanja) { uzimanja.map { "${it.terapijaId}|${it.slot}" }.toSet() }
    
    // Calculate adherence (iOS: streak, 7d %)
    val adherence7d = remember(uzimanja, doze) {
        if (doze.isEmpty()) 100.0
        else (uzimanja.count { !it.preskoceno } / doze.size.toDouble() * 100).coerceIn(0.0, 100.0)
    }
    
    // Find next appointment (iOS: Sljedeći pregled)
    val sljedeciPregled = remember(dogadjaji) {
        dogadjaji.filter { it.datum >= System.currentTimeMillis() && it.status != "obavljeno" }
            .minByOrNull { it.datum }
    }
    
    // Find low stock therapies (iOS: Lijekovi pri kraju)
    val niskiLijekovi = remember(terapije) {
        terapije.filter { it.aktivna }.mapNotNull { t ->
            val dana = DoseSchedule.danaPreostalo(t)
            if (dana != null && dana < 7) t to dana else null
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Početna", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (clanovi.isEmpty()) {
            item { EmptyState() }
            return@LazyColumn
        }

        // Family picker (iOS: FamilyPicker)
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                clanovi.forEach { clan ->
                    FilterChip(
                        selected = clan.id == aktivniId,
                        onClick = { vm.odaberiClana(clan.id) },
                        label = { Text(clan.ime) }
                    )
                }
            }
        }

        // Adherence indicator (iOS: Adherencija)
        if (doze.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
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
                }
            }
        }

        // Next appointment (iOS: Sljedeći pregled)
        sljedeciPregled?.let { pregled ->
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Sljedeći pregled", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(pregled.naslov, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            SimpleDateFormat("d. MMMM yyyy.", Locale("hr", "HR")).format(Date(pregled.datum)) +
                                (pregled.vrijeme?.let { " u $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (pregled.uputnicaPotrebna && !pregled.uputnicaIzdana) {
                            Spacer(Modifier.height(8.dp))
                            Text("⚠️ Uputnica potrebna", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Low stock alerts (iOS: Lijekovi pri kraju)
        if (niskiLijekovi.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Lijekovi pri kraju zalihe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(8.dp))
                        niskiLijekovi.forEach { (terapija, dana) ->
                            Text("• ${terapija.naziv}: ${"%.0f".format(dana)} dana", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // ICE Card (iOS: ICE kartica)
        aktivniClan?.let { clan ->
            if (!clan.hitniKontakt.isNullOrBlank() || !clan.hitniTelefon.isNullOrBlank()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("🚨 Hitni kontakt (ICE)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            clan.hitniKontakt?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            clan.hitniTelefon?.let { Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
                            clan.alergije?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text("Alergije: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Today's doses (iOS: Doze danas)
        item {
            Text("Doze danas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (doze.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Nema planiranih doza za danas", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            val preostaleDoze = doze.count { "${it.terapija.id}|${it.slot}" !in uzetiSetovi }
            item {
                Text(
                    "$preostaleDoze od ${doze.size} doza preostalo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
