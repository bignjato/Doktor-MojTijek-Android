package com.mojtijek.doktor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.*
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    
    if (clanovi.isEmpty()) {
        OnboardingScreen(onAddMember = { vm.dodajSeedData() })
    } else {
        PocetnaContent(vm)
    }
}

@Composable
private fun OnboardingScreen(onAddMember: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.FamilyRestroom,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "Dobrodošli u MojTijek",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Započnite praćenje zdravlja vaše obitelji",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
        FilledTonalButton(
            onClick = onAddMember,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dodaj prvog člana obitelji", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onAddMember,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Učitaj demo podatke")
        }
    }
}

@Composable
private fun PocetnaContent(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val aktivniClan = remember(clanovi, aktivniId) { clanovi.find { it.id == aktivniId } }
    val terapije by vm.terapijeZaAktivnog().collectAsState(initial = emptyList())
    val uzimanja by vm.uzimanjaDanas().collectAsState(initial = emptyList())
    val dogadjaji by vm.dogadjajiZaAktivnog().collectAsState(initial = emptyList())
    val mjerenja by vm.mjerenjaZaAktivnog().collectAsState(initial = emptyList())

    val danas = DoseSchedule.dayStart()
    val doze = remember(terapije) { DoseSchedule.entriesForDay(terapije, danas) }
    val uzetiSetovi = remember(uzimanja) { uzimanja.map { "${it.terapijaId}|${it.slot}" }.toSet() }
    
    // Adherence calculation
    val adherence7d = remember(uzimanja, doze) {
        if (doze.isEmpty()) 100.0
        else (uzimanja.count { !it.preskoceno } / doze.size.toDouble() * 100).coerceIn(0.0, 100.0)
    }
    
    // Next appointment
    val sljedeciPregled = remember(dogadjaji) {
        dogadjaji.filter { it.datum >= System.currentTimeMillis() && it.status != "obavljeno" }
            .minByOrNull { it.datum }
    }
    
    // Low stock therapies
    val niskiLijekovi = remember(terapije) {
        terapije.filter { it.aktivna }.mapNotNull { t ->
            val dana = DoseSchedule.danaPreostalo(t)
            if (dana != null && dana < 7) t to dana else null
        }
    }
    
    // Latest measurements
    val zadnjaMjerenja = remember(mjerenja) {
        mjerenja.groupBy { it.tip }.mapValues { it.value.maxByOrNull { m -> m.ts } }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text("Početna", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Family picker
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clanovi) { clan ->
                    FilterChip(
                        selected = clan.id == aktivniId,
                        onClick = { vm.odaberiClana(clan.id) },
                        label = { Text(clan.ime) },
                        leadingIcon = if (clan.id == aktivniId) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }

        // Health score / Adherence card
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Zdravstveno stanje", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${adherence7d.toInt()}",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    adherence7d >= 90 -> MaterialTheme.colorScheme.primary
                                    adherence7d >= 70 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                "/100",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            "Adherencija (7 dana)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CircularProgressIndicator(
                        progress = { (adherence7d / 100).toFloat() },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 8.dp
                    )
                }
            }
        }

        // Today's indicators (Danas pokazatelji)
        if (zadnjaMjerenja.isNotEmpty()) {
            item {
                Text("Danas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    zadnjaMjerenja.forEach { (tip, mjerenje) ->
                        item {
                            TodayIndicatorCard(tip, mjerenje)
                        }
                    }
                }
            }
        }

        // Next appointment
        sljedeciPregled?.let { pregled ->
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Sljedeći pregled",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                pregled.naslov,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                SimpleDateFormat("d. MMM", Locale("hr", "HR")).format(Date(pregled.datum)) +
                                    (pregled.vrijeme?.let { " u $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (pregled.uputnicaPotrebna && !pregled.uputnicaIzdana) {
                                Spacer(Modifier.height(4.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Uputnica potrebna", style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        // Low stock alerts
        if (niskiLijekovi.isNotEmpty()) {
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Lijekovi pri kraju",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        niskiLijekovi.take(3).forEach { (terapija, dana) ->
                            Text(
                                "• ${terapija.naziv}: ${"%.0f".format(dana)} dana",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // ICE Card
        aktivniClan?.let { clan ->
            if (!clan.hitniKontakt.isNullOrBlank() || !clan.hitniTelefon.isNullOrBlank()) {
                item {
                    ElevatedCard(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "🚨",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Hitni kontakt (ICE)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            clan.hitniKontakt?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }
                            clan.hitniTelefon?.let {
                                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            clan.alergije?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Alergije: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick actions (Brzi pristup)
        item {
            Text("Brzi pristup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Filled.Book,
                    label = "Dnevnik",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Folder,
                    label = "Kartoteka",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Kalendar",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Description,
                    label = "Uputnica",
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's doses
        item {
            Spacer(Modifier.height(8.dp))
            Text("Doze danas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        
        if (doze.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nema planiranih doza za danas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val preostaleDoze = doze.count { "${it.terapija.id}|${it.slot}" !in uzetiSetovi }
            item {
                LinearProgressIndicator(
                    progress = { (doze.size - preostaleDoze) / doze.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$preostaleDoze od ${doze.size} doza preostalo",
                    style = MaterialTheme.typography.labelMedium,
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
private fun TodayIndicatorCard(tip: String, mjerenje: MjerenjeEntity?) {
    if (mjerenje == null) return
    
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                tip.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            val vrijednostText = if (mjerenje.vrijednost2 != null) {
                "${mjerenje.vrijednost.toInt()}/${mjerenje.vrijednost2!!.toInt()}"
            } else {
                "${mjerenje.vrijednost.toInt()}"
            }
            Text(
                vrijednostText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                mjerenje.jedinica ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(terapija.naziv, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$slot" + (terapija.jacina?.let { " · $it" } ?: "") + " · ${terapija.oblik}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uzeto) {
                AssistChip(
                    onClick = {},
                    label = { Text("Uzeto") },
                    leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                Row {
                    IconButton(onClick = onPreskoci) {
                        Icon(Icons.Filled.Close, contentDescription = "Preskoči")
                    }
                    FilledTonalButton(onClick = onPotvrdi) {
                        Icon(Icons.Filled.Check, contentDescription = "Uzeto", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
