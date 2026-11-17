package com.mojtijek.doktor.ui.profil

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.data.Kopija
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mojtijek.doktor.ui.MojTijekViewModel

@Composable
fun ProfilScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var odabraniClanId by remember { mutableStateOf<String?>(null) }

    if (odabraniClanId != null) {
        ClanDetaljiScreen(
            vm = vm,
            clanId = odabraniClanId!!,
            onNavigateBack = { odabraniClanId = null }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj člana")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Obitelj", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (clanovi.isEmpty()) {
                Text("Nema dodanih članova. Dodaj prvog pomoću + gumba.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(clanovi) { c ->
                        ClanCard(
                            c,
                            odabran = c.id == aktivniId,
                            onOdaberi = { vm.odaberiClana(c.id) },
                            onPrikaziDetalje = { odabraniClanId = c.id }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            KopijaCard(vm)
            Spacer(Modifier.height(16.dp))
            Text("MojTijek Android · verzija sinkronizirana s iOS shared modelom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showDialog) {
        NoviClanDialog(onDismiss = { showDialog = false }, onSpremi = { ime -> vm.dodajClana(ime); showDialog = false })
    }
}

@Composable
private fun ClanCard(clan: ClanEntity, odabran: Boolean, onOdaberi: () -> Unit, onPrikaziDetalje: () -> Unit) {
    Card(onClick = onPrikaziDetalje) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(clan.ime, fontWeight = FontWeight.SemiBold)
                clan.lijecnik?.takeIf { it.isNotBlank() }?.let {
                    Text("Liječnik: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (odabran) {
                AssistChip(onClick = {}, label = { Text("Aktivan") })
            } else {
                TextButton(onClick = onOdaberi) { Text("Postavi aktivnim") }
            }
        }
    }
}

@Composable
private fun NoviClanDialog(onDismiss: () -> Unit, onSpremi: (String) -> Unit) {
    var ime by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi član obitelji") },
        text = { OutlinedTextField(ime, { ime = it }, label = { Text("Ime") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (ime.isNotBlank()) onSpremi(ime) }) { Text("Spremi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

/** Sigurnosna kopija: izvoz u datoteku koju korisnik odabere i vraćanje iz nje (isti format kao iOS). */
@Composable
private fun KopijaCard(vm: MojTijekViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sDokumentima by remember { mutableStateOf(true) }
    var poruka by remember { mutableStateOf<String?>(null) }
    var zaUvoz by remember { mutableStateOf<Pair<String, Kopija.Sazetak>?>(null) }
    val spremljeno = stringResource(R.string.kopija_spremljena)
    val vraceno = stringResource(R.string.kopija_vracena)
    val nevaljana = stringResource(R.string.kopija_nevaljana)
    val neuspjelo = stringResource(R.string.kopija_neuspjelo)

    val izvoz = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            poruka = runCatching {
                withContext(Dispatchers.IO) {
                    val json = vm.izveziKopiju(sDokumentima)
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.fold({ spremljeno }, { neuspjelo })
        }
    }
    val uvoz = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val tekst = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    tekst to Kopija.provjeri(tekst).second
                }
            }.fold({ zaUvoz = it }, { poruka = if (it is Kopija.Nevaljana) "$nevaljana (${it.detalj})" else neuspjelo })
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.kopija_naslov), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.kopija_opis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(sDokumentima, { sDokumentima = it })
                Text(stringResource(R.string.kopija_s_dokumentima), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { izvoz.launch("MojTijek-kopija-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json") },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.kopija_izvezi)) }
                OutlinedButton(onClick = { uvoz.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                               modifier = Modifier.weight(1f)) { Text(stringResource(R.string.kopija_vrati)) }
            }
            poruka?.let { Spacer(Modifier.height(8.dp)); Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    zaUvoz?.let { (tekst, s) ->
        AlertDialog(
            onDismissRequest = { zaUvoz = null },
            title = { Text(stringResource(R.string.kopija_vrati_naslov)) },
            text = { Text(stringResource(R.string.kopija_vrati_upozorenje, s.clanova, s.terapija, s.dokumenata, s.privitaka)) },
            confirmButton = {
                TextButton(onClick = {
                    zaUvoz = null
                    scope.launch { poruka = runCatching { vm.uveziKopiju(tekst) }.fold({ vraceno }, { neuspjelo }) }
                }) { Text(stringResource(R.string.kopija_zamijeni)) }
            },
            dismissButton = { TextButton(onClick = { zaUvoz = null }) { Text(stringResource(R.string.odustani)) } }
        )
    }
}
