package com.mojtijek.doktor.ui.profil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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

enum class ClanTab { OSNOVNI, DOKUMENTI, CIJEPLJENJA, DNEVNIK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClanDetaljiScreen(
    vm: MojTijekViewModel,
    clanId: String,
    onNavigateBack: () -> Unit
) {
    val clanovi by vm.clanovi.collectAsState()
    val clan = remember(clanovi) { clanovi.find { it.id == clanId } }
    var selectedTab by remember { mutableStateOf(ClanTab.OSNOVNI) }

    if (clan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Član nije pronađen")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(clan.ime) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Natrag")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == ClanTab.OSNOVNI,
                    onClick = { selectedTab = ClanTab.OSNOVNI },
                    text = { Text("Osnovni podaci") }
                )
                Tab(
                    selected = selectedTab == ClanTab.DOKUMENTI,
                    onClick = { selectedTab = ClanTab.DOKUMENTI },
                    text = { Text("Dokumenti") }
                )
                Tab(
                    selected = selectedTab == ClanTab.CIJEPLJENJA,
                    onClick = { selectedTab = ClanTab.CIJEPLJENJA },
                    text = { Text("Cijepljenja") }
                )
                Tab(
                    selected = selectedTab == ClanTab.DNEVNIK,
                    onClick = { selectedTab = ClanTab.DNEVNIK },
                    text = { Text("Dnevnik") }
                )
            }

            when (selectedTab) {
                ClanTab.OSNOVNI -> OsnovniPodaciTab(clan, vm)
                ClanTab.DOKUMENTI -> DokumentiTab(clanId, vm)
                ClanTab.CIJEPLJENJA -> CijepljenjaTab(clanId, vm)
                ClanTab.DNEVNIK -> DnevnikTab(clanId, vm)
            }
        }
    }
}

@Composable
private fun OsnovniPodaciTab(clan: ClanEntity, vm: MojTijekViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Osnovne informacije", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("OIB", clan.oib ?: "Nije uneseno")
                    InfoRow("Spol", clan.spol ?: "Nije uneseno")
                    InfoRow("Krvna grupa", clan.krvnaGrupa ?: "Nije uneseno")
                    clan.datumRodjenja?.let {
                        InfoRow("Datum rođenja", SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(it)))
                    }
                    clan.visina?.let { InfoRow("Visina", "${it}cm") }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Zdravstveni podaci", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Alergije", clan.alergije ?: "Nema")
                    InfoRow("Kronične bolesti", clan.kronicneBolesti ?: "Nema")
                    InfoRow("MBO", clan.mbo ?: "Nije uneseno")
                    InfoRow("Liječnik", clan.lijecnik ?: "Nije uneseno")
                    clan.lijecnikEmail?.let { InfoRow("Email liječnika", it) }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Hitni kontakt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Kontakt osoba", clan.hitniKontakt ?: "Nije uneseno")
                    InfoRow("Telefon", clan.hitniTelefon ?: "Nije uneseno")
                    clan.hitnaNapomena?.let { InfoRow("Napomena", it) }
                }
            }
        }

        if (clan.spol == "žensko") {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ginekološki podaci", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Kontracepcija", clan.kontracepcija ?: "Nije uneseno")
                        clan.prosjecniCiklus?.let { InfoRow("Prosječni ciklus", "$it dana") }
                        if (clan.trudna) {
                            Text("Trudna", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            clan.terminPorod?.let {
                                InfoRow("Termin poroda", SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(it)))
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uredi podatke")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DokumentiTab(clanId: String, vm: MojTijekViewModel) {
    val dokumenti by vm.dokumentiZaAktivnog().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (dokumenti.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nema dodanih dokumenata", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showDialog = true }) {
                    Text("Dodaj dokument")
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dokumenti) { dok ->
                    DokumentCard(dok)
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, "Dodaj dokument")
        }
    }

    if (showDialog) {
        NoviDokumentDialog(
            clanId = clanId,
            onDismiss = { showDialog = false },
            onSpremi = { vm.dodajDokument(it); showDialog = false }
        )
    }
}

@Composable
private fun DokumentCard(dok: DokumentEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                dok.naziv ?: "Dokument bez naziva",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                dok.vrsta.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            dok.datum?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    SimpleDateFormat("d. MMMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            dok.ustanova?.let {
                Spacer(Modifier.height(4.dp))
                Text("Ustanova: $it", style = MaterialTheme.typography.bodySmall)
            }
            dok.napomena?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NoviDokumentDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (DokumentEntity) -> Unit
) {
    var naziv by remember { mutableStateOf("") }
    var vrsta by remember { mutableStateOf("nalaz") }
    var ustanova by remember { mutableStateOf("") }
    var napomena by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi dokument") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naziv, { naziv = it }, label = { Text("Naziv dokumenta") }, singleLine = true)
                OutlinedTextField(ustanova, { ustanova = it }, label = { Text("Ustanova/Liječnik") }, singleLine = true)
                OutlinedTextField(napomena, { napomena = it }, label = { Text("Napomena") }, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSpremi(
                        DokumentEntity(
                            clanId = clanId,
                            vrsta = vrsta,
                            naziv = naziv,
                            datum = System.currentTimeMillis(),
                            ustanova = ustanova.ifBlank { null },
                            napomena = napomena.ifBlank { null }
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
private fun CijepljenjaTab(clanId: String, vm: MojTijekViewModel) {
    val cijepljenja by vm.cijepljenjaZaAktivnog().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (cijepljenja.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nema evidencije o cijepljenjima", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showDialog = true }) {
                    Text("Dodaj cijepljenje")
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cijepljenja) { cij ->
                    CijepljenjeCard(cij)
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, "Dodaj cijepljenje")
        }
    }

    if (showDialog) {
        NovoCijepljenjeDialog(
            clanId = clanId,
            onDismiss = { showDialog = false },
            onSpremi = { vm.dodajCijepljenje(it); showDialog = false }
        )
    }
}

@Composable
private fun CijepljenjeCard(cij: CijepljenjeEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(cij.naziv, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            cij.datum?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Primljeno: " + SimpleDateFormat("d. MMMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            cij.sljedece?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sljedeće: " + SimpleDateFormat("d. MMMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            cij.napomena?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NovoCijepljenjeDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (CijepljenjeEntity) -> Unit
) {
    var naziv by remember { mutableStateOf("") }
    var napomena by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo cijepljenje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naziv, { naziv = it }, label = { Text("Naziv cjepiva") }, singleLine = true)
                OutlinedTextField(napomena, { napomena = it }, label = { Text("Napomena") }, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSpremi(
                        CijepljenjeEntity(
                            clanId = clanId,
                            naziv = naziv,
                            datum = System.currentTimeMillis(),
                            napomena = napomena.ifBlank { null }
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
private fun DnevnikTab(clanId: String, vm: MojTijekViewModel) {
    val unosi by vm.dnevnikZaAktivnog().collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (unosi.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nema zapisa u dnevniku", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showDialog = true }) {
                    Text("Dodaj zapis")
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unosi) { unos ->
                    DnevnikCard(unos, onDelete = { vm.obrisiDnevnikUnos(it) })
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, "Novi zapis")
        }
    }

    if (showDialog) {
        NoviDnevnikDialog(
            clanId = clanId,
            onDismiss = { showDialog = false },
            onSpremi = { vm.dodajDnevnikUnos(it); showDialog = false }
        )
    }
}

@Composable
private fun DnevnikCard(unos: DnevnikUnosEntity, onDelete: (DnevnikUnosEntity) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    unos.naslov?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                    }
                    unos.datum?.let {
                        Text(
                            SimpleDateFormat("d. MMMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                unos.raspolozenje?.let {
                    AssistChip(
                        onClick = {},
                        label = { Text(raspolozenjeText(it)) }
                    )
                }
            }
            unos.tekst?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { onDelete(unos) }) {
                Text("Obriši")
            }
        }
    }
}

private fun raspolozenjeText(value: Int): String = when (value) {
    1 -> "😢 Loše"
    2 -> "😕 Umjereno"
    3 -> "🙂 Dobro"
    4 -> "😊 Odlično"
    else -> "Neutralno"
}

@Composable
private fun NoviDnevnikDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (DnevnikUnosEntity) -> Unit
) {
    var naslov by remember { mutableStateOf("") }
    var tekst by remember { mutableStateOf("") }
    var raspolozenje by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi zapis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(naslov, { naslov = it }, label = { Text("Naslov") }, singleLine = true)
                OutlinedTextField(tekst, { tekst = it }, label = { Text("Tekst") }, maxLines = 5)
                Text("Raspoloženje (opcionalno):", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = raspolozenje == 1, onClick = { raspolozenje = 1 }, label = { Text("😢") })
                    FilterChip(selected = raspolozenje == 2, onClick = { raspolozenje = 2 }, label = { Text("😕") })
                    FilterChip(selected = raspolozenje == 3, onClick = { raspolozenje = 3 }, label = { Text("🙂") })
                    FilterChip(selected = raspolozenje == 4, onClick = { raspolozenje = 4 }, label = { Text("😊") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naslov.isNotBlank() || tekst.isNotBlank()) {
                    onSpremi(
                        DnevnikUnosEntity(
                            clanId = clanId,
                            vrsta = "biljeska",
                            naslov = naslov.ifBlank { null },
                            tekst = tekst.ifBlank { null },
                            raspolozenje = raspolozenje,
                            datum = System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}
