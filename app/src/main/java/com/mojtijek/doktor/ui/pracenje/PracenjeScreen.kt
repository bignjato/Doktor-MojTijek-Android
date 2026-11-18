@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.pracenje

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.data.HealthConnect
import com.mojtijek.doktor.data.MjerenjeEntity
import com.mojtijek.doktor.data.Pracenje
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.IzbornikClana
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.Pill
import com.mojtijek.doktor.ui.components.VITALI
import com.mojtijek.doktor.ui.components.danVrijeme
import com.mojtijek.doktor.ui.components.formatMjerenja
import com.mojtijek.doktor.ui.components.vitalNaziv
import com.mojtijek.doktor.ui.theme.Mt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Ekran „Praćenje” — isti redoslijed kao iOS TrackingView:
// član → razdoblje → datum → Health Connect (umjesto Apple Healtha) → kartice pokazatelja → detalj s grafom.
// Još nema: Žensko zdravlje (ciklus) i upozorenje na vrijednost izvan raspona (traži podsjetnike).

private val POKAZATELJI = listOf("koraci", "puls", "tlak", "san", "saturacija", "secer", "tezina")
private val TIPOVI_UNOSA = listOf("tlak", "puls", "secer", "saturacija", "tezina", "temperatura", "koraci", "san")

private fun dan(ms: Long, stil: Int = DateFormat.MEDIUM): String = DateFormat.getDateInstance(stil).format(Date(ms))
private fun poUzorku(ms: Long, kostur: String): String =
    SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), kostur), Locale.getDefault()).format(Date(ms))
private fun broj(v: Double, decimala: Int = 1): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = decimala }.format(v)
private fun pomakni(ms: Long, polje: Int, koliko: Int): Long = Calendar.getInstance().apply { timeInMillis = ms; add(polje, koliko) }.timeInMillis

// ---- Health Connect ----

private const val HC_CLAN = "health_clan_id"
private const val RAZMAK_MS = 15 * 60_000L
private var zadnjaSinkronizacija = 0L

/**
 * Automatsko osvježavanje (iOS `HealthAutoSync.syncNow`): samo za člana pod kojim je korisnik sam pokrenuo uvoz,
 * najviše jednom u 15 minuta osim kad je `force`. Vraća broj pokazatelja ili null kad se nije čitalo.
 */
suspend fun osvjeziZdravlje(context: Context, vm: MojTijekViewModel, force: Boolean = false): Int? {
    val clanId = context.getSharedPreferences("mojtijek", Context.MODE_PRIVATE).getString(HC_CLAN, null) ?: return null
    val sada = System.currentTimeMillis()
    if (!force && sada - zadnjaSinkronizacija < RAZMAK_MS) return null
    return try {
        if (!HealthConnect.dostupan(context) || !HealthConnect.imaDozvolu(context)) return null
        zadnjaSinkronizacija = sada
        vm.upisiZdravlje(clanId, HealthConnect.najnovije(context))
    } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
}

@Composable
private fun HealthConnectKartica(clanId: String, vm: MojTijekViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var poruka by remember { mutableStateOf<String?>(null) }
    var cita by remember { mutableStateOf(false) }

    fun uvezi() {
        scope.launch {
            cita = true
            try {
                // Zapamti ovog člana kao vlasnika telefona za automatsko osvježavanje.
                context.getSharedPreferences("mojtijek", Context.MODE_PRIVATE).edit().putString(HC_CLAN, clanId).apply()
                val n = osvjeziZdravlje(context, vm, force = true) ?: 0
                poruka = if (n == 0) context.getString(R.string.hc_nema_podataka) else context.getString(R.string.osvjezeno_n, n)
            } finally { cita = false }
        }
    }
    val dozvole = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { odobreno ->
        if (odobreno.isNotEmpty()) uvezi() else poruka = context.getString(R.string.hc_bez_dozvole)
    }

    MtCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Mt.Red), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Health Connect", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(poruka ?: stringResource(R.string.hc_uvezi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(enabled = !cita, onClick = {
                when (HealthConnect.stanje(context)) {
                    HealthConnectClient.SDK_AVAILABLE -> scope.launch {
                        val ima = try { HealthConnect.imaDozvolu(context) } catch (e: CancellationException) { throw e } catch (e: Exception) { false }
                        if (ima) uvezi() else dozvole.launch(HealthConnect.DOZVOLE)
                    }
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> poruka = context.getString(R.string.hc_azuriraj)
                    else -> poruka = context.getString(R.string.hc_nedostupan)
                }
            }) { Text(stringResource(if (cita) R.string.citam else R.string.osvjezi)) }
        }
    }
}

// ---- popis pokazatelja ----

@Composable
fun PracenjeScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val clan = clanovi.firstOrNull { it.id == aktivniId } ?: clanovi.firstOrNull()
    val mjerenja by remember(vm) { vm.mjerenjaZaAktivnog() }.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var razdoblje by rememberSaveable { mutableStateOf(1) } // 0 dan, 1 tjedan, 2 mjesec, 3 godina
    var datum by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var novo by remember { mutableStateOf(false) }
    var otvoreniTip by rememberSaveable { mutableStateOf<String?>(null) }

    // Osvježi današnje vrijednosti (npr. zbroj koraka do sada).
    LaunchedEffect(Unit) { osvjeziZdravlje(context, vm) }

    val tip = otvoreniTip
    if (tip != null) {
        DetaljPokazatelja(tip, mjerenja.filter { it.tip == tip }) { otvoreniTip = null }
        return
    }

    val sada = System.currentTimeMillis()
    val zadnja = remember(mjerenja) { mjerenja.groupBy { it.tip }.mapValues { (_, v) -> v.maxByOrNull { it.ts } } }
    val odKada = sada - listOf(1, 7, 31, 365)[razdoblje] * 86_400_000L

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_pracenje), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                IconButton(onClick = { novo = true }, enabled = clan != null) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dodaj_podatak), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (clan == null) {
            item { Text(stringResource(R.string.nema_clanova), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 32.dp)) }
            return@LazyColumn
        }
        item { IzbornikClana(clan, clanovi) { vm.odaberiClana(it) } }
        item {
            val nazivi = listOf(R.string.dan, R.string.tjedan, R.string.mjesec, R.string.godina)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                nazivi.forEachIndexed { i, naziv ->
                    SegmentedButton(selected = razdoblje == i, onClick = { razdoblje = i }, icon = {},
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = nazivi.size)) {
                        Text(stringResource(naziv), maxLines = 1)
                    }
                }
            }
        }
        item {
            val polje = listOf(Calendar.DAY_OF_YEAR, Calendar.WEEK_OF_YEAR, Calendar.MONTH, Calendar.YEAR)[razdoblje]
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { datum = pomakni(datum, polje, -1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
                Text(when (razdoblje) {
                         0 -> dan(datum, DateFormat.LONG)
                         1 -> stringResource(R.string.tjedan_datum, poUzorku(datum, "MMMd"))
                         2 -> poUzorku(datum, "MMMMy")
                         else -> poUzorku(datum, "y")
                     },
                     style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { datum = pomakni(datum, polje, 1) },
                           enabled = !(razdoblje == 0 && DoseSchedule.dayStart(datum) == DoseSchedule.dayStart(sada))) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null)
                }
            }
        }
        item { HealthConnectKartica(clan.id, vm) }
        items(POKAZATELJI) { t ->
            KarticaPokazatelja(t, zadnja[t], mjerenja.filter { it.tip == t && it.ts >= odKada }.sortedBy { it.ts }) { otvoreniTip = t }
        }
    }

    if (novo && clan != null) {
        NovoMjerenjeDialog(clanId = clan.id, onDismiss = { novo = false }, onSpremi = { vm.dodajMjerenje(it); novo = false })
    }
}

@Composable
private fun KarticaPokazatelja(tip: String, zadnje: MjerenjeEntity?, uzorci: List<MjerenjeEntity>, onClick: () -> Unit) {
    val info = VITALI[tip]
    val boja = info?.boja ?: Mt.Teal
    MtCard(Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(boja.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                if (info != null) Icon(info.ikona, contentDescription = null, tint = boja, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vitalNaziv(tip), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (zadnje != null) {
                        Text("${formatMjerenja(zadnje)} ${info?.jedinica ?: ""}".trim(), style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.SemiBold, color = Mt.ink)
                    }
                }
                if (tip == "koraci" && zadnje != null) {
                    LinearProgressIndicator(progress = { (zadnje.vrijednost / 10000.0).coerceIn(0.0, 1.0).toFloat() }, color = boja,
                                            trackColor = boja.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.cilj_koraka, formatMjerenja(zadnje)), style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (uzorci.size > 1) {
                    Graf(uzorci.takeLast(14).map { it.ts to it.vrijednost }, boja, Modifier.fillMaxWidth().height(44.dp))
                } else if (zadnje == null) {
                    Text(stringResource(R.string.nema_mjerenja), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** Linijski graf; os Y uključuje nulu, kao zadani iOS graf. */
@Composable
private fun Graf(tocke: List<Pair<Long, Double>>, boja: Color, modifier: Modifier, linija: Boolean = true, ispuna: Boolean = false,
                 tockice: Boolean = false, debljina: Dp = 2.dp) {
    Canvas(modifier.padding(4.dp)) {
        if (tocke.isEmpty()) return@Canvas
        val t0 = tocke.first().first
        val t1 = tocke.last().first
        val lo = minOf(0.0, tocke.minOf { it.second })
        val hi = maxOf(0.0, tocke.maxOf { it.second })
        val raspon = (hi - lo).takeIf { it > 0 } ?: 1.0
        val p = tocke.map { (t, v) ->
            Offset(if (t1 == t0) size.width / 2 else (t - t0).toFloat() / (t1 - t0) * size.width,
                   size.height - ((v - lo) / raspon).toFloat() * size.height)
        }
        if (ispuna && p.size > 1) {
            val put = Path().apply {
                moveTo(p.first().x, size.height)
                p.forEach { lineTo(it.x, it.y) }
                lineTo(p.last().x, size.height)
                close()
            }
            drawPath(put, Brush.verticalGradient(listOf(boja.copy(alpha = 0.22f), boja.copy(alpha = 0.02f))))
        }
        if (linija) p.zipWithNext { a, b -> drawLine(boja, a, b, strokeWidth = debljina.toPx()) }
        if (tockice) p.forEach { drawCircle(boja, radius = 3.5.dp.toPx(), center = it) }
    }
}

// ---- detalj pokazatelja (iOS VitalDetailView) ----

@Composable
private fun DetaljPokazatelja(tip: String, sva: List<MjerenjeEntity>, onNatrag: () -> Unit) {
    BackHandler(onBack = onNatrag)
    val info = VITALI[tip]
    val boja = info?.boja ?: Mt.Teal
    val jedinica = info?.jedinica ?: ""
    var raspon by rememberSaveable { mutableStateOf(30) }
    // Prozor su puni kalendarski dani, ne „sada minus N × 24 h”.
    val pocetak = pomakni(DoseSchedule.dayStart(), Calendar.DAY_OF_YEAR, -(raspon - 1))
    val sirova = remember(sva, pocetak) { sva.filter { it.ts >= pocetak }.sortedBy { it.ts } }
    val dani = remember(sirova, tip) { Pracenje.dnevneVrijednosti(tip, sirova) }
    val ukupno = Pracenje.agregat(tip, dani)
    val poDanima = raspon > 1

    fun formatDana(d: Pracenje.Dan): String {
        val dia = d.vrijednost2
        return when {
            tip == "tlak" && dia != null -> "${Math.round(d.vrijednost)}/${Math.round(dia)}"
            tip == "koraci" -> NumberFormat.getIntegerInstance().format(Math.round(d.vrijednost))
            else -> broj(d.vrijednost)
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNatrag) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
                Text(vitalNaziv(tip), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            val rasponi = listOf(1 to R.string.raspon_1d, 7 to R.string.raspon_7d, 30 to R.string.raspon_30d, 90 to R.string.raspon_90d, 365 to R.string.raspon_1g)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                rasponi.forEachIndexed { i, (dana, naziv) ->
                    SegmentedButton(selected = raspon == dana, onClick = { raspon = dana }, icon = {},
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = rasponi.size)) {
                        Text(stringResource(naziv), maxLines = 1)
                    }
                }
            }
        }
        item {
            MtCard {
                when {
                    dani.size > 1 -> Graf(dani.map { it.dan to it.vrijednost }, boja, Modifier.fillMaxWidth().height(250.dp),
                                          ispuna = true, tockice = true, debljina = 3.dp)
                    // Više mjerenja istog dana: samo točke.
                    sirova.size > 1 -> Graf(sirova.map { it.ts to it.vrijednost }, boja, Modifier.fillMaxWidth().height(250.dp),
                                            linija = false, tockice = true)
                    else -> Text(stringResource(R.string.trend_min_dva), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            MtCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(if (tip == "koraci") R.string.ukupno else R.string.prosjek), style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${broj(ukupno)} $jedinica".trim(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        // Sam zbroj ne govori puno bez broja dana kroz koje je skupljen.
                        if (tip == "koraci" && dani.size > 1) {
                            Text(stringResource(R.string.prosjek_dnevno, broj(ukupno / dani.size, 0)), style = MaterialTheme.typography.labelSmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Pill(if (poDanima) pluralStringResource(R.plurals.n_dana, dani.size, dani.size)
                         else pluralStringResource(R.plurals.n_ocitanja, sirova.size, sirova.size), boja)
                }
            }
        }
        item { Text(stringResource(R.string.povijest), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (poDanima) {
            items(dani.reversed(), key = { it.dan }) { d -> RedPovijesti(dan(d.dan), "${formatDana(d)} $jedinica".trim()) }
        } else {
            items(sirova.reversed(), key = { it.id }) { m -> RedPovijesti(danVrijeme(m.ts), "${formatMjerenja(m)} $jedinica".trim()) }
        }
    }
}

@Composable
private fun RedPovijesti(kada: String, vrijednost: String) {
    MtCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(kada, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(vrijednost, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ---- ručni unos (iOS AddMeasurementView) ----

@Composable
private fun Odjeljak(naslov: String, sadrzaj: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(naslov.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sadrzaj()
    }
}

@Composable
internal fun NovoMjerenjeDialog(clanId: String, onDismiss: () -> Unit, onSpremi: (MjerenjeEntity) -> Unit) {
    val context = LocalContext.current
    var tip by remember { mutableStateOf("tlak") }
    var vrijednost by remember { mutableStateOf("") }
    var druga by remember { mutableStateOf("") }
    var ts by remember { mutableStateOf(System.currentTimeMillis()) }
    var napomena by remember { mutableStateOf("") }
    var biracTipa by remember { mutableStateOf(false) }
    var biracDatuma by remember { mutableStateOf(false) }
    var biracVremena by remember { mutableStateOf(false) }

    val v = vrijednost.replace(',', '.').toDoubleOrNull()
    val v2 = druga.replace(',', '.').toDoubleOrNull()
    val valjano = v != null && (tip != "tlak" || v2 != null)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.dodaj_podatak)) },
                    navigationIcon = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.odustani)) } },
                    actions = {
                        TextButton(enabled = valjano, onClick = {
                            if (v != null) onSpremi(MjerenjeEntity(clanId = clanId, tip = tip, vrijednost = v, vrijednost2 = if (tip == "tlak") v2 else null,
                                                                   ts = ts, datum = ts, jedinica = VITALI[tip]?.jedinica,
                                                                   napomena = napomena.trim().ifEmpty { null }, izvor = "rucno"))
                        }) { Text(stringResource(R.string.spremi)) }
                    },
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                   verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Odjeljak(stringResource(R.string.pokazatelj)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.vrsta), Modifier.weight(1f))
                        Box {
                            TextButton(onClick = { biracTipa = true }) { Text(vitalNaziv(tip)) }
                            DropdownMenu(expanded = biracTipa, onDismissRequest = { biracTipa = false }) {
                                TIPOVI_UNOSA.forEach { t -> DropdownMenuItem(text = { Text(vitalNaziv(t)) }, onClick = { tip = t; biracTipa = false }) }
                            }
                        }
                    }
                }
                Odjeljak(stringResource(R.string.vrijednost)) {
                    val natpis = when (tip) { "tlak" -> R.string.sistolicki; "puls" -> R.string.puls; "san" -> R.string.sati_sna; else -> R.string.vrijednost }
                    OutlinedTextField(vrijednost, { vrijednost = it }, label = { Text(stringResource(natpis)) }, singleLine = true,
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    if (tip == "tlak") {
                        OutlinedTextField(druga, { druga = it }, label = { Text(stringResource(R.string.dijastolicki)) }, singleLine = true,
                                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                    Text(VITALI[tip]?.jedinica ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Odjeljak(stringResource(R.string.vrijeme_i_napomena)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.datum_i_vrijeme), Modifier.weight(1f))
                        TextButton(onClick = { biracDatuma = true }) { Text(dan(ts)) }
                        TextButton(onClick = { biracVremena = true }) { Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ts))) }
                    }
                    OutlinedTextField(napomena, { napomena = it }, label = { Text(stringResource(R.string.napomena)) }, modifier = Modifier.fillMaxWidth())
                }
                Text(stringResource(R.string.rucni_podaci), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (biracDatuma) {
        // Birač datuma radi u UTC ponoći; iz njega se uzima samo kalendarski dan.
        val lokalno = Calendar.getInstance().apply { timeInMillis = ts }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(lokalno.get(Calendar.YEAR), lokalno.get(Calendar.MONTH), lokalno.get(Calendar.DAY_OF_MONTH))
        }
        val stanje = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { biracDatuma = false },
            confirmButton = {
                TextButton(onClick = {
                    stanje.selectedDateMillis?.let { odabrano ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = odabrano }
                        ts = Calendar.getInstance().apply {
                            timeInMillis = ts
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis
                    }
                    biracDatuma = false
                }) { Text(stringResource(R.string.gotovo)) }
            },
            dismissButton = { TextButton(onClick = { biracDatuma = false }) { Text(stringResource(R.string.odustani)) } },
        ) { DatePicker(state = stanje) }
    }
    if (biracVremena) {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        val stanje = rememberTimePickerState(initialHour = c.get(Calendar.HOUR_OF_DAY), initialMinute = c.get(Calendar.MINUTE),
                                             is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { biracVremena = false },
            text = { TimePicker(state = stanje) },
            confirmButton = {
                TextButton(onClick = {
                    ts = Calendar.getInstance().apply {
                        timeInMillis = ts
                        set(Calendar.HOUR_OF_DAY, stanje.hour); set(Calendar.MINUTE, stanje.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    biracVremena = false
                }) { Text(stringResource(R.string.gotovo)) }
            },
            dismissButton = { TextButton(onClick = { biracVremena = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}
