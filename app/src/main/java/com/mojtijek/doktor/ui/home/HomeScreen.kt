@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.data.DogadjajEntity
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.data.MjerenjeEntity
import com.mojtijek.doktor.data.TerapijaEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.MemberAvatar
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.ObiteljSheet
import com.mojtijek.doktor.ui.components.Pill
import com.mojtijek.doktor.ui.components.PotvrdiDozuDialog
import com.mojtijek.doktor.ui.components.VITALI
import com.mojtijek.doktor.ui.components.formatMjerenja
import com.mojtijek.doktor.ui.components.vitalNaziv
import com.mojtijek.doktor.ui.pracenje.NovoMjerenjeDialog
import com.mojtijek.doktor.ui.pracenje.osvjeziZdravlje
import com.mojtijek.doktor.ui.theme.Mt
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

// Ekran „Moj dan” — isti redoslijed i tekstovi kao iOS HomeView:
// član → terapije koje treba uzeti → sljedeći pregled → moji pokazatelji → narudžba → brzi pristup → napomena.
// Još nema (dolazi kad ti ekrani budu postojali): Odgodi (traži podsjetnike), kratki opis lijeka,
// Uputnica/bolnica, ekran narudžbe, hitni podaci (ICE). Gumb bez funkcije se ne prikazuje.

private val POKAZATELJI = listOf("koraci", "puls", "tlak", "san", "saturacija", "secer", "tezina")

private fun dan(ms: Long, stil: Int = DateFormat.MEDIUM): String = DateFormat.getDateInstance(stil).format(Date(ms))
private fun danVrijeme(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@Composable
fun HomeScreen(
    vm: MojTijekViewModel,
    onDnevnik: () -> Unit,
    onKartoteka: () -> Unit,
    onKalendar: () -> Unit,
    onPracenje: () -> Unit,
) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val clan = clanovi.firstOrNull { it.id == aktivniId } ?: clanovi.firstOrNull()
    if (clan == null) {
        PrviKoraci(vm)
        return
    }

    val terapije by remember(vm) { vm.terapijeZaAktivnog() }.collectAsState(initial = emptyList())
    val uzimanja by remember(vm) { vm.svaUzimanja() }.collectAsState(initial = emptyList())
    val dogadjaji by remember(vm) { vm.dogadjajiZaAktivnog() }.collectAsState(initial = emptyList())
    val mjerenja by remember(vm) { vm.mjerenjaZaAktivnog() }.collectAsState(initial = emptyList())
    // Kartica doze ovisi o trenutku („čeka potvrdu” / „sljedeća”), pa se osvježava svake minute.
    val sada by produceState(System.currentTimeMillis()) {
        while (true) { delay(60_000); value = System.currentTimeMillis() }
    }
    val doze = remember(terapije, uzimanja, sada) { DoseSchedule.homeDoses(terapije, uzimanja, sada) }
    val dospjele = remember(terapije, uzimanja, sada) { DoseSchedule.dueToday(terapije, uzimanja, sada) }
    val pregled = remember(dogadjaji, sada) {
        val danas = DoseSchedule.dayStart(sada)
        dogadjaji.filter { it.datum >= danas && it.status != "otkazano" && it.status != "obavljeno" }.minByOrNull { it.datum }
    }
    val zadnja = remember(mjerenja) { mjerenja.groupBy { it.tip }.mapValues { (_, v) -> v.maxByOrNull { it.ts } } }
    val priKraju = remember(terapije) { terapije.count { it.aktivna && DoseSchedule.trebaNaruciti(it) } }

    var obitelj by remember { mutableStateOf(false) }
    var novoMjerenje by remember { mutableStateOf(false) }
    // Health Connect: osvježi današnje vrijednosti kad se otvori početni ekran (najviše jednom u 15 min).
    val kontekst = LocalContext.current
    LaunchedEffect(Unit) { osvjeziZdravlje(kontekst, vm) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.moj_dan), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                 modifier = Modifier.weight(1f))
            IconButton(onClick = { novoMjerenje = true }) {
                Icon(Icons.Filled.AddCircle, contentDescription = stringResource(R.string.novo_mjerenje),
                     tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
        ZaglavljeClana(clan, sada) { obitelj = true }
        TerapijeZaUzeti(doze, dospjele, sada, vm)
        SljedeciPregled(pregled) { vm.azurirajDogadjaj(it.copy(uputnicaIzdana = true)) }
        MojiPokazatelji(clan, zadnja, onPracenje)
        if (priKraju > 0) NarudzbaLijekova(priKraju)
        BrziPristup(onDnevnik, onKartoteka, onKalendar)
        Text(stringResource(R.string.odricanje), style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
             modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
    }

    if (obitelj) ObiteljSheet(clanovi, clan.id, onOdabir = { vm.odaberiClana(it) }, onZatvori = { obitelj = false })
    if (novoMjerenje) {
        NovoMjerenjeDialog(clanId = clan.id, onDismiss = { novoMjerenje = false },
                           onSpremi = { vm.dodajMjerenje(it); novoMjerenje = false })
    }
}

@Composable
private fun ZaglavljeClana(clan: ClanEntity, sada: Long, onOdabir: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOdabir),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MemberAvatar(clan, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(clan.ime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Mt.ink)
            Text(dan(sada, DateFormat.FULL), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Naslov(tekst: String, modifier: Modifier = Modifier) {
    Text(tekst, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
private fun TerapijeZaUzeti(
    doze: List<DoseSchedule.ScheduledDose>, dospjele: List<DoseSchedule.ScheduledDose>, sada: Long, vm: MojTijekViewModel,
) {
    val context = LocalContext.current
    var zaPotvrdu by remember { mutableStateOf<DoseSchedule.ScheduledDose?>(null) }
    var potvrdaSvih by remember { mutableStateOf(false) }
    var poruka by remember { mutableStateOf<String?>(null) }

    if (doze.isEmpty()) {
        MtCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.nema_doza), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        val pager = rememberPagerState(pageCount = { doze.size })
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Naslov(stringResource(R.string.terapije_za_uzeti), Modifier.weight(1f))
                if (doze.size > 1) {
                    Text("${minOf(pager.currentPage + 1, doze.size)} / ${doze.size}", style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalPager(state = pager, pageSpacing = 12.dp, verticalAlignment = Alignment.Top) { i ->
                doze.getOrNull(i)?.let { doza -> KarticaDoze(doza, sada) { zaPotvrdu = doza } }
            }
            if (doze.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    doze.indices.forEach { i ->
                        Box(Modifier.padding(horizontal = 4.dp).size(7.dp).clip(CircleShape).background(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Swipe, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.povuci_za_sljedeci), style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (dospjele.size > 1) {
                Button(onClick = { potvrdaSvih = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.uzeto_sve, dospjele.size))
                }
                Text(stringResource(R.string.uzeto_sve_napomena), style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    zaPotvrdu?.let { doza ->
        PotvrdiDozuDialog(doza, onPotvrdi = { vm.potvrdiDozu(doza.terapija, doza.slot, doza.plannedMillis); zaPotvrdu = null },
                          onOdustani = { zaPotvrdu = null })
    }
    if (potvrdaSvih) {
        AlertDialog(
            onDismissRequest = { potvrdaSvih = false },
            title = { Text(stringResource(R.string.potvrdi_sve_doze)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.oznacit_ce_se), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    dospjele.forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(d.slot, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Column(Modifier.weight(1f)) {
                                Text(d.terapija.naziv, fontWeight = FontWeight.SemiBold)
                                d.terapija.jacina?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Podatak(stringResource(R.string.vrijeme_uzimanja), danVrijeme(System.currentTimeMillis()))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = dospjele.size
                    vm.potvrdiDoze(dospjele)
                    potvrdaSvih = false
                    poruka = context.getString(R.string.potvrdjene_doze, n)
                }) { Text(stringResource(R.string.potvrdi_sve_kao_uzeto)) }
            },
            dismissButton = { TextButton(onClick = { potvrdaSvih = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
    poruka?.let {
        AlertDialog(onDismissRequest = { poruka = null }, title = { Text(stringResource(R.string.app_name)) }, text = { Text(it) },
                    confirmButton = { TextButton(onClick = { poruka = null }) { Text(stringResource(R.string.u_redu)) } })
    }
}

@Composable
private fun Podatak(naziv: String, vrijednost: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(naziv, Modifier.weight(1f))
        Text(vrijednost, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun KarticaDoze(doza: DoseSchedule.ScheduledDose, sada: Long, onUzeto: () -> Unit) {
    val t = doza.terapija
    val naglasak = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Medication, contentDescription = null, tint = naglasak, modifier = Modifier.size(18.dp))
            Text(stringResource(if (doza.plannedMillis < sada) R.string.doza_ceka else R.string.sljedeca_doza),
                 style = MaterialTheme.typography.bodyMedium, color = naglasak)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(doza.slot, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = naglasak)
            if (DoseSchedule.dayStart(doza.plannedMillis) != DoseSchedule.dayStart(sada)) {
                Text(dan(doza.plannedMillis), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        Text(t.naziv, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(t.jacina?.takeIf { it.isNotBlank() } ?: stringResource(R.string.prema_rasporedu),
             style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val opis = kratkiOpis(t)
        if (opis != null) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)).padding(10.dp),
                   verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.kratko_o_lijeku), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = naglasak)
                Text(opis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onUzeto, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(50.dp)) {
            Text(stringResource(R.string.uzeto))
        }
    }
}

/** Prvi odlomak opisa; bez njega razlog koji je korisnik upisao. Medicinska namjena se ne izmišlja. */
@Composable
private fun kratkiOpis(t: TerapijaEntity): String? {
    val opis = t.opis?.trim()?.substringBefore("\n\n")?.trim()?.takeIf { it.isNotEmpty() }
    val razlog = t.razlog?.trim()?.takeIf { it.isNotEmpty() }
    val tekst = when {
        opis != null -> opis
        razlog != null -> stringResource(R.string.za_razlog, razlog)
        else -> return null
    }
    return if (tekst.length > 150) tekst.take(150).trim() + "…" else tekst
}

@Composable
private fun SljedeciPregled(pregled: DogadjajEntity?, onUputnicaIzdana: (DogadjajEntity) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Naslov(stringResource(R.string.sljedeci_pregled))
        if (pregled == null) {
            MtCard { Text(stringResource(R.string.nema_pregleda), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            MtCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(pregled.naslov, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(dan(pregled.datum), pregled.vrijeme?.takeIf { it.isNotBlank() }, pregled.lokacija?.takeIf { it.isNotBlank() })
                                 .joinToString(" · "),
                             style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (pregled.uputnicaPotrebna && !pregled.uputnicaIzdana) {
                            if (pregled.uputnicaZatrazena) {
                                Pill(stringResource(R.string.ceka_uputnica), Mt.Orange,
                                     Modifier.clip(CircleShape).clickable { onUputnicaIzdana(pregled) })
                            } else {
                                Pill(stringResource(R.string.treba_uputnica), Mt.Orange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MojiPokazatelji(clan: ClanEntity, zadnja: Map<String, MjerenjeEntity?>, onPracenje: () -> Unit) {
    val prefs = LocalContext.current.getSharedPreferences("mojtijek", Context.MODE_PRIVATE)
    val kljuc = "home_metrics_${clan.id}"
    var izbor by remember(clan.id) {
        mutableStateOf((prefs.getString(kljuc, null) ?: "koraci,tlak").split(",").filter { it in POKAZATELJI })
    }
    var uredi by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Naslov(stringResource(R.string.moji_pokazatelji), Modifier.weight(1f))
            TextButton(onClick = { uredi = true }) { Text(stringResource(R.string.uredi)) }
        }
        POKAZATELJI.filter { it in izbor }.chunked(2).forEach { red ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                red.forEach { tip -> Pokazatelj(tip, zadnja[tip], Modifier.weight(1f), onPracenje) }
                if (red.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (izbor.isEmpty()) Text(stringResource(R.string.odaberi_pokazatelje), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (uredi) {
        AlertDialog(
            onDismissRequest = { uredi = false },
            title = { Text(stringResource(R.string.moji_pokazatelji)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    POKAZATELJI.forEach { tip ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(vitalNaziv(tip), Modifier.weight(1f))
                            Switch(checked = tip in izbor, onCheckedChange = { ukljuci ->
                                val novi = POKAZATELJI.filter { if (it == tip) ukljuci else it in izbor }
                                izbor = novi
                                prefs.edit().putString(kljuc, novi.joinToString(",")).apply()
                            })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { uredi = false }) { Text(stringResource(R.string.gotovo)) } },
        )
    }
}

@Composable
private fun Pokazatelj(tip: String, m: MjerenjeEntity?, modifier: Modifier, onClick: () -> Unit) {
    val info = VITALI[tip]
    val boja = info?.boja ?: Mt.Teal
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Column(Modifier.defaultMinSize(minHeight = 104.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vitalNaziv(tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (info != null) Icon(info.ikona, contentDescription = null, tint = boja, modifier = Modifier.size(20.dp))
            }
            if (m == null) {
                Text("—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.nema_podatka), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatMjerenja(m), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Mt.ink)
                    Text(info?.jedinica ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(bottom = 3.dp))
                }
                val izvor = when (m.izvor) { "healthkit" -> "Apple Health"; "healthconnect" -> "Health Connect"; else -> stringResource(R.string.rucno) }
                Text("$izvor · ${danVrijeme(m.ts)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tip == "koraci") {
                    LinearProgressIndicator(progress = { (m.vrijednost / 10000.0).coerceIn(0.0, 1.0).toFloat() }, color = boja,
                                            trackColor = boja.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
                } else {
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(boja.copy(alpha = 0.15f))) {
                        Box(Modifier.width(54.dp).height(4.dp).clip(CircleShape).background(boja))
                    }
                }
            }
        }
    }
}

@Composable
private fun NarudzbaLijekova(priKraju: Int) {
    MtCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Mt.Orange, modifier = Modifier.size(28.dp))
            Column {
                Text(stringResource(R.string.narudzba_lijekova), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.lijekova_pri_kraju, priKraju), style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BrziPristup(onDnevnik: () -> Unit, onKartoteka: () -> Unit, onKalendar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Naslov(stringResource(R.string.brzi_pristup))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrzaRadnja(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.dnevnik), Mt.Purple, Modifier.weight(1f), onDnevnik)
            BrzaRadnja(Icons.Filled.MedicalServices, stringResource(R.string.kartoteka), Mt.Teal, Modifier.weight(1f), onKartoteka)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrzaRadnja(Icons.Filled.CalendarMonth, stringResource(R.string.kalendar), Mt.Blue, Modifier.weight(1f), onKalendar)
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun BrzaRadnja(ikona: ImageVector, naslov: String, boja: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(boja.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(22.dp))
            }
            Text(naslov, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Prazna aplikacija (iOS `FirstStepsView`). „Skeniraj prvi nalaz” dolazi s kartotekom. */
@Composable
private fun PrviKoraci(vm: MojTijekViewModel) {
    var noviClan by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(28.dp),
           horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
        Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        Text(stringResource(R.string.kreni_od_sebe), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Button(onClick = { noviClan = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.dodaj_clana))
        }
        TextButton(onClick = { vm.dodajSeedData() }) { Text(stringResource(R.string.ucitaj_demo)) }
    }
    if (noviClan) {
        var ime by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { noviClan = false },
            title = { Text(stringResource(R.string.dodaj_clana)) },
            text = { OutlinedTextField(ime, { ime = it }, label = { Text(stringResource(R.string.ime)) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.dodajClana(ime.trim()); noviClan = false }, enabled = ime.isNotBlank()) {
                    Text(stringResource(R.string.spremi))
                }
            },
            dismissButton = { TextButton(onClick = { noviClan = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}
