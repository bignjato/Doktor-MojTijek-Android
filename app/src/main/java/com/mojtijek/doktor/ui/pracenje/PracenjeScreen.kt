package com.mojtijek.doktor.ui.pracenje

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.MjerenjeEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.ChartDataPoint
import com.mojtijek.doktor.ui.components.SimpleLineChart
import java.text.SimpleDateFormat
import java.util.*

private val TIPOVI = listOf("koraci", "krvni tlak", "puls", "šećer", "težina", "temperatura")

enum class PracenjeTab { MJERENJA, TRENDOVI, LABORATORIJ }

@Composable
fun PracenjeScreen(vm: MojTijekViewModel) {
    val mjerenja by vm.mjerenjaZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(PracenjeTab.MJERENJA) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Novo mjerenje")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("Praćenje", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == PracenjeTab.MJERENJA,
                        onClick = { selectedTab = PracenjeTab.MJERENJA },
                        text = { Text("Mjerenja") }
                    )
                    Tab(
                        selected = selectedTab == PracenjeTab.TRENDOVI,
                        onClick = { selectedTab = PracenjeTab.TRENDOVI },
                        text = { Text("Trendovi") }
                    )
                    Tab(
                        selected = selectedTab == PracenjeTab.LABORATORIJ,
                        onClick = { selectedTab = PracenjeTab.LABORATORIJ },
                        text = { Text("Laboratorij") }
                    )
                }
            }
            
            when (selectedTab) {
                PracenjeTab.MJERENJA -> {
                    if (mjerenja.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("Nema unesenih mjerenja.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mjerenja) { m -> MjerenjeCard(m, onDelete = { vm.obrisiMjerenje(it) }) }
                        }
                    }
                }
                PracenjeTab.TRENDOVI -> {
                    TrendoviView(mjerenja)
                }
                PracenjeTab.LABORATORIJ -> {
                    LabNalaziView(vm)
                }
            }
        }
    }

    if (showDialog && selectedTab == PracenjeTab.MJERENJA) {
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

@Composable
private fun LabNalaziView(vm: MojTijekViewModel) {
    val lab by vm.labZaAktivnog().collectAsState(initial = emptyList())
    
    if (lab.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Nema laboratorijskih nalaza", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Laboratorijski nalazi se dodaju kroz dokumente u odjeljku Obitelj → Detalji člana → Dokumenti",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lab) { l -> LabNalazCard(l) }
        }
    }
}

@Composable
private fun TrendoviView(mjerenja: List<MjerenjeEntity>) {
    val tipoviSaPodacima = remember(mjerenja) {
        mjerenja.map { it.tip }.distinct().sorted()
    }
    
    if (tipoviSaPodacima.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Nema podataka za trendove.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(tipoviSaPodacima) { tip ->
                TrendCard(tip = tip, mjerenja = mjerenja.filter { it.tip == tip })
            }
        }
    }
}

@Composable
private fun TrendCard(tip: String, mjerenja: List<MjerenjeEntity>) {
    val podaci = remember(mjerenja) {
        mjerenja
            .sortedBy { it.ts }
            .takeLast(30)
            .map { ChartDataPoint(it.ts, it.vrijednost) }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                tip.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            if (mjerenja.isNotEmpty()) {
                val latest = mjerenja.maxByOrNull { it.ts }
                latest?.let {
                    val vrijednostText = if (it.vrijednost2 != null) {
                        "${it.vrijednost.toInt()}/${it.vrijednost2!!.toInt()}"
                    } else {
                        "%.1f".format(it.vrijednost)
                    }
                    Text(
                        "$vrijednostText ${it.jedinica ?: ""}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SimpleLineChart(
                data = podaci,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LabNalazCard(nalaz: com.mojtijek.doktor.data.LabNalazEntity) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(nalaz.naziv, fontWeight = FontWeight.SemiBold)
                nalaz.kratica?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                nalaz.datum?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                nalaz.vrijednost?.let { vrijednost ->
                    Text(
                        "${vrijednost} ${nalaz.jedinica ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (nalaz.refLo != null && nalaz.refHi != null) {
                    Text(
                        "Ref: ${nalaz.refLo}-${nalaz.refHi}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
