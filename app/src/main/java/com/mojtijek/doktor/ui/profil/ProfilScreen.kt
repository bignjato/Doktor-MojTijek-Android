package com.mojtijek.doktor.ui.profil

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.Kopija
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.MemberAvatar
import com.mojtijek.doktor.ui.components.ObiteljSheet
import com.mojtijek.doktor.ui.theme.Tema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Ekran „Profil” — isti redoslijed odjeljaka kao iOS ProfileView:
// član → Obitelj → Izgled → Povezivanje i podaci → Aplikacija → Demo.
// Redak iza kojeg na Androidu još nema ekrana se ne prikazuje (Premium, Izvještaji, ICE, sažetak za liječnika,
// zaključavanje, privole, povijest promjena, podsjetnici, vodič, uklanjanje demo podataka).

private fun verzija(context: Context): String = try {
    val p = context.packageManager.getPackageInfo(context.packageName, 0)
    val build = if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else @Suppress("DEPRECATION") p.versionCode.toLong()
    "${p.versionName} ($build)"
} catch (e: Exception) { "?" }

/** Jezik aplikacije mijenja se u postavkama sustava (kao na iOS-u); prije Androida 13 otvara se stranica aplikacije. */
private fun otvoriJezik(context: Context) {
    val paket = Uri.fromParts("package", context.packageName, null)
    val jezik = if (Build.VERSION.SDK_INT >= 33) Intent(Settings.ACTION_APP_LOCALE_SETTINGS, paket) else null
    try {
        context.startActivity(jezik ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paket))
    } catch (e: Exception) {
        try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paket)) } catch (e2: Exception) { }
    }
}

@Composable
fun ProfilScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val clan = clanovi.firstOrNull { it.id == aktivniId } ?: clanovi.firstOrNull()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var urediId by remember { mutableStateOf<String?>(null) }
    var obitelj by remember { mutableStateOf(false) }
    var noviClan by remember { mutableStateOf(false) }
    var biracTeme by remember { mutableStateOf(false) }
    var pitajKopiju by remember { mutableStateOf(false) }
    var sDokumentima by remember { mutableStateOf(true) }
    var radi by remember { mutableStateOf(false) }
    var poruka by remember { mutableStateOf<String?>(null) }
    var zaUvoz by remember { mutableStateOf<Pair<String, Kopija.Sazetak>?>(null) }

    val uredi = urediId
    if (uredi != null) {
        ClanDetaljiScreen(vm = vm, clanId = uredi, onNavigateBack = { urediId = null })
        return
    }

    val spremljeno = stringResource(R.string.kopija_spremljena)
    val vraceno = stringResource(R.string.kopija_vracena)
    val nevaljana = stringResource(R.string.kopija_nevaljana)
    val neuspjelo = stringResource(R.string.kopija_neuspjelo)

    val izvoz = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            radi = true
            poruka = runCatching {
                withContext(Dispatchers.IO) {
                    val json = vm.izveziKopiju(sDokumentima)
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) }
                }
            }.fold({ spremljeno }, { neuspjelo })
            radi = false
        }
    }
    val uvoz = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            radi = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val tekst = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    tekst to Kopija.provjeri(tekst).second
                }
            }.fold({ zaUvoz = it }, { poruka = if (it is Kopija.Nevaljana) "$nevaljana (${it.detalj})" else neuspjelo })
            radi = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.tab_profil_naslov), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
             modifier = Modifier.padding(bottom = 8.dp))

        if (clan != null) {
            Odjeljak(null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MemberAvatar(clan, 58.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(clan.ime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.obiteljski_profil), style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { urediId = clan.id }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.uredi_podatke_clana), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Crta()
                Redak(Icons.Filled.Edit, stringResource(R.string.uredi_podatke_clana), strelica = true) { urediId = clan.id }
            }
        }

        Odjeljak(stringResource(R.string.obitelj)) {
            if (clan != null) {
                Redak(Icons.Filled.Group, stringResource(R.string.clanovi_obitelji), strelica = true) { obitelj = true }
                Crta()
            }
            Redak(Icons.Filled.PersonAdd, stringResource(R.string.dodaj_clana)) { noviClan = true }
        }

        Odjeljak(stringResource(R.string.izgled)) {
            Box {
                val teme = listOf("system" to R.string.tema_sustav, "light" to R.string.tema_svijetlo, "dark" to R.string.tema_tamno)
                Redak(Icons.Filled.Contrast, stringResource(R.string.tema),
                      vrijednost = stringResource(teme.first { it.first == Tema.izbor }.second)) { biracTeme = true }
                DropdownMenu(expanded = biracTeme, onDismissRequest = { biracTeme = false }) {
                    teme.forEach { (kljuc, naziv) ->
                        DropdownMenuItem(text = { Text(stringResource(naziv)) }, onClick = { Tema.postavi(context, kljuc); biracTeme = false })
                    }
                }
            }
            Crta()
            Redak(Icons.Filled.Language, stringResource(R.string.jezik_aplikacije), strelica = true) { otvoriJezik(context) }
        }

        Odjeljak(stringResource(R.string.povezivanje_i_podaci)) {
            Redak(Icons.Filled.FileDownload, stringResource(R.string.uvezi_kopiju_json)) {
                uvoz.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
            Crta()
            Redak(Icons.Filled.FileUpload, stringResource(R.string.kopija_naslov)) { pitajKopiju = true }
            if (radi) LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        Odjeljak(stringResource(R.string.aplikacija), napomena = stringResource(R.string.o_aplikaciji_napomena)) {
            Redak(Icons.Filled.Info, stringResource(R.string.verzija), vrijednost = verzija(context))
            Crta()
            Redak(Icons.Filled.Storage, stringResource(R.string.pohrana), vrijednost = stringResource(R.string.pohrana_lokalno))
        }

        // Demo se nudi dok ga nema — ponovljeni dodir ne smije udvostručiti demo članove.
        if (clanovi.none { it.ime == "Ana Horvat" }) {
            Odjeljak(stringResource(R.string.demo)) {
                Redak(Icons.Filled.ContentCopy, stringResource(R.string.ucitaj_demo)) { vm.dodajSeedData() }
            }
        }
    }

    if (obitelj && clan != null) ObiteljSheet(clanovi, clan.id, onOdabir = { vm.odaberiClana(it) }, onZatvori = { obitelj = false })

    if (noviClan) {
        var ime by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noviClan = false },
            title = { Text(stringResource(R.string.dodaj_clana)) },
            text = { OutlinedTextField(ime, { ime = it }, label = { Text(stringResource(R.string.ime)) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = ime.isNotBlank(), onClick = { vm.dodajClana(ime.trim()); noviClan = false }) { Text(stringResource(R.string.spremi)) }
            },
            dismissButton = { TextButton(onClick = { noviClan = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }

    // Sami nalazi ulaze u kopiju samo ako korisnik to izričito odabere (kao na iOS-u).
    if (pitajKopiju) {
        fun spremi(dokumenti: Boolean) {
            sDokumentima = dokumenti
            pitajKopiju = false
            izvoz.launch("MojTijek-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json")
        }
        AlertDialog(
            onDismissRequest = { pitajKopiju = false },
            title = { Text(stringResource(R.string.kopija_ukljuciti_nalaze)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.kopija_puna_opis))
                    Text(stringResource(R.string.kopija_opis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { spremi(true) }) { Text(stringResource(R.string.kopija_s_nalazima)) }
                        TextButton(onClick = { spremi(false) }) { Text(stringResource(R.string.kopija_samo_podaci)) }
                        TextButton(onClick = { pitajKopiju = false }) { Text(stringResource(R.string.odustani)) }
                    }
                }
            },
            confirmButton = {},
        )
    }

    zaUvoz?.let { (tekst, s) ->
        AlertDialog(
            onDismissRequest = { zaUvoz = null },
            title = { Text(stringResource(R.string.kopija_vrati_naslov)) },
            text = { Text(stringResource(R.string.kopija_vrati_upozorenje, s.clanova, s.terapija, s.dokumenata, s.privitaka)) },
            confirmButton = {
                TextButton(onClick = {
                    zaUvoz = null
                    scope.launch {
                        radi = true
                        poruka = runCatching { vm.uveziKopiju(tekst) }.fold({ vraceno }, { neuspjelo })
                        radi = false
                    }
                }) { Text(stringResource(R.string.kopija_zamijeni)) }
            },
            dismissButton = { TextButton(onClick = { zaUvoz = null }) { Text(stringResource(R.string.odustani)) } },
        )
    }

    poruka?.let {
        AlertDialog(onDismissRequest = { poruka = null }, title = { Text(stringResource(R.string.app_name)) }, text = { Text(it) },
                    confirmButton = { TextButton(onClick = { poruka = null }) { Text(stringResource(R.string.u_redu)) } })
    }
}

/** Skupina redaka s naslovom, kao odjeljak iOS liste. */
@Composable
private fun Odjeljak(naslov: String?, napomena: String? = null, sadrzaj: @Composable () -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (naslov != null) {
            Text(naslov.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                 modifier = Modifier.padding(start = 16.dp))
        }
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column { sadrzaj() }
        }
        if (napomena != null) {
            Text(napomena, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                 modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun Crta() = HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun Redak(ikona: ImageVector, naslov: String, vrijednost: String? = null, strelica: Boolean = false, onClick: (() -> Unit)? = null) {
    Row((if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(ikona, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(naslov, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (vrijednost != null) Text(vrijednost, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (strelica) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
