package com.mojtijek.doktor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mojtijek.doktor.data.*
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    
    if (clanovi.isEmpty()) {
        OnboardingScreen(onLoadDemo = { vm.dodajSeedData() })
    } else {
        MojDanContent(vm)
    }
}

@Composable
private fun OnboardingScreen(onLoadDemo: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.FamilyRestroom,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Dobrodošli u MojTijek",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onLoadDemo,
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Učitaj demo podatke", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MojDanContent(vm: MojTijekViewModel) {
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
    
    val sljedeciPregled = remember(dogadjaji) {
        dogadjaji.filter { it.datum >= System.currentTimeMillis() && it.status != "obavljeno" }
            .minByOrNull { it.datum }
    }
    
    val zadnjaMjerenja = remember(mjerenja) {
        mjerenja.groupBy { it.tip }.mapValues { it.value.maxByOrNull { m -> m.ts } }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Moj dan",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj")
                }
            }
        }

        // Member selector
        aktivniClan?.let { clan ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Previous member */ }) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = "Prethodni",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Avatar with initials
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                clan.ime.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(
                                clan.ime,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                            Text(
                                SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        
                        IconButton(onClick = { /* Next member */ }) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "Sljedeći",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Therapies section
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Terapije koje treba uzeti",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val preostalo = doze.count { "${it.terapija.id}|${it.slot}" !in uzetiSetovi }
                Text(
                    "$preostalo/${doze.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        }

        // Dose carousel
        if (doze.isNotEmpty()) {
            items(doze) { dose ->
                val key = "${dose.terapija.id}|${dose.slot}"
                val uzeto = key in uzetiSetovi
                if (!uzeto) {
                    DoseCard(
                        terapija = dose.terapija,
                        slot = dose.slot,
                        onUzeto = { vm.potvrdiDozu(dose.terapija, dose.slot) },
                        onOdgodi = { vm.preskociDozu(dose.terapija, dose.slot) }
                    )
                }
            }
        } else {
            item {
                EmptyDoseCard()
            }
        }

        // Next appointment
        item {
            Text(
                "Sljedeći pregled",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        item {
            sljedeciPregled?.let { pregled ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                pregled.naslov,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                SimpleDateFormat("d. MMM u HH:mm", Locale("hr", "HR")).format(Date(pregled.datum)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } ?: Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    Modifier.padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nema nadolazećih pregleda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Indicators section
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Moji pokazatelji",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { }) {
                    Text("Uredi", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Metrics grid
        if (zadnjaMjerenja.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    zadnjaMjerenja.entries.take(2).forEach { (tip, mjerenje) ->
                        MetricCard(tip, mjerenje, Modifier.weight(1f))
                    }
                }
            }
            if (zadnjaMjerenja.size > 2) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        zadnjaMjerenja.entries.drop(2).take(2).forEach { (tip, mjerenje) ->
                            MetricCard(tip, mjerenje, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun DoseCard(
    terapija: TerapijaEntity,
    slot: String,
    onUzeto: () -> Unit,
    onOdgodi: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Sljedeća doza",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                slot,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text(
                terapija.naziv,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${terapija.jacina ?: ""} ${terapija.oblik}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
            
            terapija.napomena?.takeIf { it.isNotBlank() }?.let { napomena ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        napomena,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onUzeto,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Uzeto", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onOdgodi,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Odgodi", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyDoseCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            Modifier.padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sve doze za danas su uzete! 🎉",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun MetricCard(tip: String, mjerenje: MjerenjeEntity?, modifier: Modifier = Modifier) {
    if (mjerenje == null) return
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (tip.lowercase()) {
                    "koraci" -> "🏃"
                    "tlak", "krvni tlak" -> "❤️"
                    "šećer" -> "🍬"
                    "težina" -> "⚖️"
                    "temperatura" -> "🌡️"
                    else -> "📊"
                }
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    tip.replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            val vrijednostText = if (mjerenje.vrijednost2 != null) {
                "${mjerenje.vrijednost.toInt()}/${mjerenje.vrijednost2!!.toInt()}"
            } else {
                mjerenje.vrijednost.toInt().toString()
            }
            Text(
                vrijednostText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                mjerenje.jedinica ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
