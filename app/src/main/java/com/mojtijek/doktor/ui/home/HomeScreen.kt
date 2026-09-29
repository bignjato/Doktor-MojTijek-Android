package com.mojtijek.doktor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mojtijek.doktor.data.*
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

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
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Dobrodošli u MojTijek",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Vaš osobni zdravstveni asistent",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onLoadDemo,
                modifier = Modifier.fillMaxWidth(0.7f).height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text("Učitaj demo podatke", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    val neuzetaDoze = remember(doze, uzetiSetovi) {
        doze.filter { "${it.terapija.id}|${it.slot}" !in uzetiSetovi }
    }
    
    val sljedeciPregled = remember(dogadjaji) {
        dogadjaji.filter { it.datum >= System.currentTimeMillis() && it.status != "obavljeno" }
            .minByOrNull { it.datum }
    }
    
    val zadnjaMjerenja = remember(mjerenja) {
        mjerenja.groupBy { it.tip }.mapValues { it.value.maxByOrNull { m -> m.ts } }
    }
    
    // Calculate health score
    val healthScore = remember(uzimanja, mjerenja, doze) {
        calculateHealthScore(uzimanja, mjerenja, doze)
    }
    
    // Calculate adherence
    val adherencePercent = remember(doze, uzetiSetovi) {
        if (doze.isEmpty()) 100 else ((doze.size - neuzetaDoze.size) * 100 / doze.size)
    }
    
    // Low stock warnings
    val lowStockTherapies = remember(terapije) {
        terapije.filter { 
            it.aktivna && it.kolicina != null && it.pragDana != null &&
            (it.kolicina!! / (it.dozaKom * it.putaDnevno)) <= it.pragDana!!
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 100.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
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
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier.size(50.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Dodaj", modifier = Modifier.size(26.dp))
                }
            }
        }

        // Member selector
        aktivniClan?.let { clan ->
            item {
                MemberSelectorCard(
                    clan = clan,
                    onPrevious = {
                        val idx = clanovi.indexOf(clan)
                        if (idx > 0) vm.odaberiClana(clanovi[idx - 1].id)
                    },
                    onNext = {
                        val idx = clanovi.indexOf(clan)
                        if (idx < clanovi.size - 1) vm.odaberiClana(clanovi[idx + 1].id)
                    }
                )
            }
        }

        // Health Score
        item {
            HealthScoreCard(score = healthScore, adherencePercent = adherencePercent)
        }
        
        // Quick Actions
        item {
            QuickActionsGrid()
        }

        // Low stock warning
        if (lowStockTherapies.isNotEmpty()) {
            item {
                LowStockWarning(therapies = lowStockTherapies)
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${doze.size - neuzetaDoze.size}/${doze.size}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Dose Carousel
        item {
            if (neuzetaDoze.isNotEmpty()) {
                DoseCarousel(
                    doses = neuzetaDoze,
                    onUzeto = { dose -> vm.potvrdiDozu(dose.terapija, dose.slot) },
                    onOdgodi = { dose -> vm.preskociDozu(dose.terapija, dose.slot) }
                )
            } else {
                EmptyDoseCard()
            }
        }

        // Next appointment
        item {
            Text(
                "Sljedeći pregled",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }
        
        item {
            sljedeciPregled?.let { pregled ->
                AppointmentCard(pregled)
            } ?: EmptyAppointmentCard()
        }

        // ICE Card
        aktivniClan?.let { clan ->
            if (!clan.hitniKontakt.isNullOrBlank()) {
                item {
                    Text(
                        "Hitni kontakt (ICE)",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                }
                item {
                    ICECard(clan)
                }
            }
        }

        // Indicators section
        if (zadnjaMjerenja.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Moji pokazatelji",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                    TextButton(onClick = { }) {
                        Text(
                            "Uredi",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Metrics grid
            item {
                MetricsGrid(zadnjaMjerenja)
            }
        }
    }
}

@Composable
private fun MemberSelectorCard(
    clan: ClanEntity,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Prethodni",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Avatar with initials
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    clan.ime.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    clan.ime,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Sljedeći",
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthScoreCard(score: Int, adherencePercent: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Zdravstveni rezultat",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$score",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        "/100",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Adherencija: $adherencePercent%",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Score ring visualization
            Box(
                Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(80.dp),
                    color = when {
                        score >= 75 -> MaterialTheme.colorScheme.primary
                        score >= 50 -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    },
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid() {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuickActionButton(
                icon = Icons.Filled.Edit,
                label = "Dnevnik",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Filled.Folder,
                label = "Kartoteka",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuickActionButton(
                icon = Icons.Filled.CalendarMonth,
                label = "Kalendar",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Filled.Receipt,
                label = "Uputnica",
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LowStockWarning(therapies: List<TerapijaEntity>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFFFA726).copy(alpha = 0.15f),
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFFFA726),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Lijekovi pri kraju",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    therapies.joinToString(", ") { it.naziv },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoseCarousel(
    doses: List<DoseSchedule.ScheduledDose>,
    onUzeto: (DoseSchedule.ScheduledDose) -> Unit,
    onOdgodi: (DoseSchedule.ScheduledDose) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { doses.size })
    
    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val dose = doses[page]
            DoseCard(
                terapija = dose.terapija,
                slot = dose.slot,
                onUzeto = { onUzeto(dose) },
                onOdgodi = { onOdgodi(dose) }
            )
        }
        
        if (doses.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(doses.size) { index ->
                    Box(
                        Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    if (index < doses.size - 1) Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DoseCard(
    terapija: TerapijaEntity,
    slot: String,
    onUzeto: () -> Unit,
    onOdgodi: () -> Unit
) {
    // Hero dose card - large, soft, prominent
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sljedeća doza",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                slot,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                terapija.naziv,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
            
            Text(
                "${terapija.jacina ?: ""} ${terapija.oblik}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
            
            terapija.napomena?.takeIf { it.isNotBlank() }?.let { napomena ->
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Kratko o lijeku",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            napomena,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = onUzeto,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(27.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Text(
                        "Uzeto",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedButton(
                    onClick = onOdgodi,
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(27.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp
                    )
                ) {
                    Text(
                        "Odgodi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDoseCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            Modifier.padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Sve doze za danas su uzete!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppointmentCard(pregled: DogadjajEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pregled.naslov,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    SimpleDateFormat("d. MMM u HH:mm", Locale("hr", "HR")).format(Date(pregled.datum)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                if (pregled.uputnicaIzdana) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Uputnica",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyAppointmentCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Box(
            Modifier.padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Nema nadolazećih pregleda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ICECard(clan: ClanEntity) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFEF5350).copy(alpha = 0.15f),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF5350).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalHospital,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFEF5350)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    clan.hitniKontakt ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    clan.hitniTelefon ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                clan.alergije?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Alergije: $it",
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Icon(
                Icons.Filled.Phone,
                contentDescription = null,
                tint = Color(0xFFEF5350)
            )
        }
    }
}

@Composable
private fun MetricsGrid(zadnjaMjerenja: Map<String, MjerenjeEntity?>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        zadnjaMjerenja.entries.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { (tip, mjerenje) ->
                    MetricCard(tip, mjerenje, Modifier.weight(1f))
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(tip: String, mjerenje: MjerenjeEntity?, modifier: Modifier = Modifier) {
    if (mjerenje == null) return
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val icon = when (tip.lowercase()) {
                    "koraci" -> "🏃"
                    "tlak", "krvni tlak" -> "❤️"
                    "šećer" -> "🍬"
                    "težina" -> "⚖️"
                    "temperatura" -> "🌡️"
                    "puls" -> "💓"
                    else -> "📊"
                }
                Text(icon, fontSize = 24.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                tip.replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            val vrijednostText = if (mjerenje.vrijednost2 != null) {
                "${mjerenje.vrijednost.toInt()}/${mjerenje.vrijednost2!!.toInt()}"
            } else {
                mjerenje.vrijednost.toInt().toString()
            }
            Text(
                vrijednostText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                mjerenje.jedinica ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

private fun calculateHealthScore(
    uzimanja: List<UzimanjeEntity>,
    mjerenja: List<MjerenjeEntity>,
    doze: List<DoseSchedule.ScheduledDose>
): Int {
    // Adherence component (40%)
    val adherence = if (doze.isEmpty()) 40 else {
        val uzeto = uzimanja.size
        val total = doze.size
        min(40, (uzeto * 40) / total)
    }
    
    // Metrics component (60%) - simple presence check
    val metricsScore = min(60, mjerenja.distinctBy { it.tip }.size * 10)
    
    return adherence + metricsScore
}
