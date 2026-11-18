@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mojtijek.doktor.ui.terapije

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.Adherencija
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.data.TerapijaEntity
import com.mojtijek.doktor.data.UzimanjeEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.IzbornikClana
import com.mojtijek.doktor.ui.components.MemberAvatar
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.Pill
import com.mojtijek.doktor.ui.components.PotvrdiDozuDialog
import com.mojtijek.doktor.ui.theme.Mt
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Ekran „Terapije” — isti redoslijed kao iOS TherapiesView:
// član → raspored (dnevno/tjedno) → pridržavanje → „Uredi terapije i zalihe” s karticama lijekova.
// Još nema: Pametna terapija, skeniranje kutije i AI opis lijeka (dolaze sa svojim ekranima).

private fun dan(ms: Long, stil: Int = DateFormat.MEDIUM): String = DateFormat.getDateInstance(stil).format(Date(ms))
private fun sat(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))
private fun imeDana(d: Int): String = DateFormatSymbols.getInstance().weekdays.getOrNull(d) ?: ""
/** Dani 1 = nedjelja … 7 = subota, redom kako tjedan počinje u regiji korisnika. */
private fun redoslijedDana(): List<Int> { val prvi = Calendar.getInstance().firstDayOfWeek; return (0 until 7).map { (prvi - 1 + it) % 7 + 1 } }
private fun danPlus(ms: Long, dana: Int): Long = Calendar.getInstance().apply { timeInMillis = ms; add(Calendar.DAY_OF_YEAR, dana) }.timeInMillis

@Composable
fun TerapijeScreen(vm: MojTijekViewModel) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val clan = clanovi.firstOrNull { it.id == aktivniId } ?: clanovi.firstOrNull()

    val sve by remember(vm) { vm.terapijeZaAktivnog() }.collectAsState(initial = emptyList())
    val uzimanja by remember(vm) { vm.svaUzimanja() }.collectAsState(initial = emptyList())
    val sada by produceState(System.currentTimeMillis()) {
        while (true) { delay(60_000); value = System.currentTimeMillis() }
    }
    val terapije = remember(sve) { sve.filter { it.aktivna }.sortedBy { it.naziv } }

    var nova by remember { mutableStateOf(false) }
    var zaUrediti by remember { mutableStateOf<TerapijaEntity?>(null) }
    var otvoreno by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_terapije), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                 modifier = Modifier.weight(1f))
            if (clan != null) {
                IconButton(onClick = { nova = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.nova_terapija), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (clan == null) {
            Prazno(stringResource(R.string.nema_clanova), stringResource(R.string.dodaj_clana_opis))
            return@Column
        }
        IzbornikClana(clan, clanovi) { vm.odaberiClana(it) }
        Raspored(terapije, uzimanja, sada, vm)
        PridrzavanjeKartica(terapije, uzimanja, sada)
        if (terapije.isEmpty()) Prazno(stringResource(R.string.nema_terapija), stringResource(R.string.dodaj_terapiju_opis))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { otvoreno = !otvoreno }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.uredi_terapije), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Icon(if (otvoreno) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        if (otvoreno) terapije.forEach { t -> KarticaTerapije(t, uzimanja, sada, vm) { zaUrediti = t } }
    }

    if (nova && clan != null) {
        TerapijaForma(null, clan.id, onOdustani = { nova = false }, onSpremi = { vm.dodajTerapiju(it); nova = false })
    }
    zaUrediti?.let { t ->
        TerapijaForma(t, t.clanId, onOdustani = { zaUrediti = null }, onSpremi = { vm.dodajTerapiju(it); zaUrediti = null })
    }
}

@Composable
private fun Prazno(naslov: String, opis: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
        Text(naslov, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(opis, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Raspored doza za dan (iOS `TherapyAgendaView`): danas ili jedan od idućih sedam dana. */
@Composable
private fun Raspored(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long, vm: MojTijekViewModel) {
    var tjedno by rememberSaveable { mutableStateOf(false) }
    var pomak by rememberSaveable { mutableStateOf(0) }
    var zaPotvrdu by remember { mutableStateOf<DoseSchedule.ScheduledDose?>(null) }
    val odabrani = danPlus(sada, if (tjedno) pomak else 0)
    val jeDanas = DoseSchedule.dayStart(odabrani) == DoseSchedule.dayStart(sada)
    val doze = remember(terapije, odabrani) { DoseSchedule.entriesForDay(terapije, odabrani) }
    val potvrdjeno = doze.count { DoseSchedule.zapis(it, uzimanja)?.preskoceno == false }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(dan(odabrani, DateFormat.FULL), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !tjedno, onClick = { tjedno = false }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                Text(stringResource(R.string.dnevno))
            }
            SegmentedButton(selected = tjedno, onClick = { tjedno = true }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                Text(stringResource(R.string.tjedno))
            }
        }
        if (tjedno) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 7).forEach { i ->
                    Text(SimpleDateFormat("EEE d", Locale.getDefault()).format(Date(danPlus(sada, i))),
                         modifier = Modifier.clip(RoundedCornerShape(10.dp))
                             .background(if (pomak == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                             .clickable { pomak = i }.padding(10.dp))
                }
            }
        }
        Text(stringResource(R.string.potvrdjeno_od, potvrdjeno, doze.size), style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (doze.isEmpty()) {
            MtCard { Text(stringResource(R.string.nema_doza_dan), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        doze.forEach { doza ->
            val zapis = DoseSchedule.zapis(doza, uzimanja)
            MtCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(doza.slot, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(doza.terapija.naziv, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (zapis != null) {
                            val boja = if (zapis.preskoceno) MaterialTheme.colorScheme.onSurfaceVariant else Mt.Green
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(if (zapis.preskoceno) Icons.Filled.RemoveCircle else Icons.Filled.CheckCircle, contentDescription = null,
                                     tint = boja, modifier = Modifier.size(16.dp))
                                Text(if (zapis.preskoceno) stringResource(R.string.preskoceno) else stringResource(R.string.uzeto_u, sat(zapis.ts)),
                                     style = MaterialTheme.typography.bodySmall, color = boja)
                            }
                        } else {
                            Text(stringResource(if (doza.plannedMillis < sada) R.string.ceka_potvrdu else R.string.planirano),
                                 style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (jeDanas) {
                                Button(onClick = { zaPotvrdu = doza }, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.potvrdi_uzimanje)) }
                            }
                        }
                    }
                }
            }
        }
    }
    zaPotvrdu?.let { doza ->
        PotvrdiDozuDialog(doza, onPotvrdi = { vm.potvrdiDozu(doza.terapija, doza.slot, doza.plannedMillis); zaPotvrdu = null },
                          onOdustani = { zaPotvrdu = null })
    }
}

@Composable
private fun PridrzavanjeKartica(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long) {
    if (!Adherencija.imaPlan(terapije)) return
    val danas = DoseSchedule.dayStart(sada)
    val niz = remember(terapije, uzimanja, danas) { Adherencija.niz(terapije, uzimanja, sada) }
    val tjedni = remember(terapije, uzimanja, danas) { Adherencija.prosjek(terapije, uzimanja, sada, 7) }
    val dani = remember(terapije, uzimanja, danas) { Adherencija.poDanu(terapije, uzimanja, sada, 7).reversed() }
    MtCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val boja = if (niz > 0) Mt.Orange else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = boja, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.niz_dana, niz), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                     color = boja, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.sedam_dana, tjedni), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                dani.forEach { p ->
                    Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (p >= 100) Mt.Green else if (p >= 50) Mt.Orange else Mt.Red.copy(alpha = 0.55f)))
                }
            }
        }
    }
}

@Composable
private fun rezimOpis(t: TerapijaEntity): String {
    val dijelovi = ArrayList<String>()
    val d = t.danUTjednu
    if (d != null) {
        dijelovi += stringResource(R.string.x_tjedno, t.putaDnevno)
        dijelovi += imeDana(d)
    } else {
        dijelovi += stringResource(R.string.x_dnevno, t.putaDnevno)
    }
    val vremena = DoseSchedule.parse(t.vremena) ?: emptyList()
    if (vremena.isNotEmpty()) dijelovi += vremena.joinToString(", ")
    return dijelovi.joinToString(" · ")
}

@Composable
private fun KarticaTerapije(t: TerapijaEntity, uzimanja: List<UzimanjeEntity>, sada: Long, vm: MojTijekViewModel, onUredi: () -> Unit) {
    var izbornik by remember { mutableStateOf(false) }
    var brisanje by remember { mutableStateOf(false) }
    val priKraju = DoseSchedule.priKraju(t)
    val naruceno = t.narucenoTs != null

    MtCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onUredi), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Medication, contentDescription = null, tint = Mt.Purple)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(listOfNotNull(t.naziv, t.jacina?.takeIf { it.isNotBlank() }).joinToString(" "),
                             style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(rezimOpis(t), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (priKraju && !naruceno) Pill(stringResource(R.string.pri_kraju), Mt.Orange)
                else if (naruceno) Pill(stringResource(R.string.naruceno), Mt.Green)
                t.receptDo?.let { r ->
                    val dana = ((DoseSchedule.dayStart(r) - DoseSchedule.dayStart(sada)) / 86_400_000L).toInt()
                    if (dana <= 21) {
                        Pill(if (dana < 0) stringResource(R.string.recept_istekao) else stringResource(R.string.recept_do, dan(r)),
                             if (dana < 0) Mt.Red else Mt.Orange)
                    }
                }
                Box {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.uredi), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.clip(CircleShape).clickable { izbornik = true }.padding(4.dp))
                    DropdownMenu(expanded = izbornik, onDismissRequest = { izbornik = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.uredi_zaliha)) }, onClick = { izbornik = false; onUredi() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.napuni_kutija)) }, onClick = { izbornik = false; vm.napuniZalihu(t) })
                        if (naruceno) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.ponisti_naruceno)) }, onClick = { izbornik = false; vm.oznaciNaruceno(t, false) })
                        } else if (priKraju) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.oznaci_naruceno)) }, onClick = { izbornik = false; vm.oznaciNaruceno(t, true) })
                        }
                        DropdownMenuItem(text = { Text(stringResource(R.string.obrisi), color = Mt.Red) }, onClick = { izbornik = false; brisanje = true })
                    }
                }
            }

            val vremena = DoseSchedule.parse(t.vremena) ?: emptyList()
            if (vremena.isNotEmpty()) {
                val uzimaSeDanas = DoseSchedule.uzimaSe(t, Calendar.getInstance().apply { timeInMillis = sada })
                val sljedeci = DoseSchedule.sljedeciDanDoze(t, sada)
                if (uzimaSeDanas) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        vremena.forEach { slot -> CipDoze(t, slot, uzimanja, sada, vm) }
                    }
                } else if (sljedeci != null) {
                    // Tjedna terapija na dan kad se ne uzima: umjesto čipova, kad je sljedeća.
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mt.Purple.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Mt.Purple, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.sljedeca_doza_dan, imeDana(Calendar.getInstance().apply { timeInMillis = sljedeci }.get(Calendar.DAY_OF_WEEK))),
                             style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Mt.Purple)
                    }
                }
            }
            t.opis?.takeIf { it.isNotBlank() }?.let { opis ->
                Text(opis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp))
            }
            val dana = DoseSchedule.danaPreostalo(t)
            if (dana != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.procjena_zalihe), style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.oko_dana, dana.toInt()), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { vm.napuniZalihu(t) }) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.napuni))
                    }
                }
                LinearProgressIndicator(progress = { (dana / 30.0).coerceIn(0.0, 1.0).toFloat() },
                                        color = if (priKraju) Mt.Orange else Mt.Teal, modifier = Modifier.fillMaxWidth())
            } else {
                TextButton(onClick = onUredi) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.postavi_zalihu))
                }
            }
        }
    }

    if (brisanje) {
        AlertDialog(
            onDismissRequest = { brisanje = false },
            title = { Text(stringResource(R.string.obrisati_terapiju)) },
            text = { Text(stringResource(R.string.obrisati_terapiju_opis)) },
            confirmButton = { TextButton(onClick = { vm.obrisiTerapiju(t); brisanje = false }) { Text(stringResource(R.string.obrisi), color = Mt.Red) } },
            dismissButton = { TextButton(onClick = { brisanje = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

/** Čip današnje doze: dodir = uzeto / poništi, dugi pritisak = preskoči (iOS `DoseChip`). */
@Composable
private fun CipDoze(t: TerapijaEntity, slot: String, uzimanja: List<UzimanjeEntity>, sada: Long, vm: MojTijekViewModel) {
    var izbornik by remember { mutableStateOf(false) }
    val danas = DoseSchedule.dayStart(sada)
    val zapis = uzimanja.firstOrNull { it.terapijaId == t.id && it.slot == slot && DoseSchedule.dayStart(it.datum ?: it.ts) == danas }
    val uzeto = zapis != null && !zapis.preskoceno
    val preskoceno = zapis?.preskoceno == true
    val boja = if (uzeto) Mt.Green else if (preskoceno) MaterialTheme.colorScheme.onSurfaceVariant else Mt.Teal
    val dio = DoseSchedule.dioDana(slot)
    val naziv = stringResource(when (dio) { "ujutro" -> R.string.ujutro; "podne" -> R.string.podne; else -> R.string.navecer })
    val ikona = when {
        uzeto -> Icons.Filled.CheckCircle
        preskoceno -> Icons.Filled.RemoveCircle
        dio == "ujutro" -> Icons.Filled.WbTwilight
        dio == "podne" -> Icons.Filled.WbSunny
        else -> Icons.Filled.NightsStay
    }
    Box {
        Row(Modifier.clip(CircleShape).background(boja.copy(alpha = 0.12f))
                .combinedClickable(onClick = { vm.prebaciDozu(t, slot) }, onLongClick = { izbornik = true })
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(16.dp))
            Text("$naziv $slot", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = boja,
                 textDecoration = if (preskoceno) TextDecoration.LineThrough else null)
        }
        DropdownMenu(expanded = izbornik, onDismissRequest = { izbornik = false }) {
            DropdownMenuItem(text = { Text(stringResource(if (preskoceno) R.string.ponisti_preskakanje else R.string.preskoci_dozu)) },
                             onClick = { izbornik = false; vm.preskociDozu(t, slot) })
        }
    }
}

// ---- unos i uređivanje (iOS AddTherapyView / EditTherapyView) ----

/** Birač datuma radi u UTC ponoći; baza čuva lokalnu ponoć istog kalendarskog dana. */
private fun utcULokalno(utc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear(); set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
private fun lokalnoUUtc(lokalno: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = lokalno }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@Composable
private fun Odjeljak(naslov: String, napomena: String? = null, sadrzaj: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(naslov.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sadrzaj()
        if (napomena != null) Text(napomena, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TerapijaForma(pocetna: TerapijaEntity?, clanId: String, onOdustani: () -> Unit, onSpremi: (TerapijaEntity) -> Unit) {
    var naziv by remember { mutableStateOf(pocetna?.naziv ?: "") }
    var jacina by remember { mutableStateOf(pocetna?.jacina ?: "") }
    var vremena by remember { mutableStateOf(pocetna?.let { (DoseSchedule.parse(it.vremena) ?: emptyList()).joinToString(", ") } ?: "08:00") }
    var tjedno by remember { mutableStateOf(pocetna?.danUTjednu != null) }
    var danUTjednu by remember { mutableStateOf(pocetna?.danUTjednu ?: Calendar.getInstance().firstDayOfWeek) }
    var zaliha by remember {
        mutableStateOf(when {
            pocetna == null -> "30"
            pocetna.kolicina == 0.0 -> ""
            pocetna.kolicina % 1.0 == 0.0 -> pocetna.kolicina.toInt().toString()
            else -> pocetna.kolicina.toString()
        })
    }
    var prag by remember { mutableStateOf((pocetna?.pragDana ?: 7).toString()) }
    var razlog by remember { mutableStateOf(pocetna?.razlog ?: "") }
    var opis by remember { mutableStateOf(pocetna?.opis ?: "") }
    var aktivna by remember { mutableStateOf(pocetna?.aktivna ?: true) }
    var imaRecept by remember { mutableStateOf(pocetna?.receptDo != null) }
    var receptDo by remember { mutableStateOf(pocetna?.receptDo ?: DoseSchedule.dayStart()) }
    var biracDatuma by remember { mutableStateOf(false) }
    var biracDana by remember { mutableStateOf(false) }

    val parsirano = DoseSchedule.parse(vremena)
    val valjano = naziv.isNotBlank() && parsirano != null

    fun spremi() {
        val v = parsirano ?: return
        val osnova = pocetna ?: TerapijaEntity(clanId = clanId)
        onSpremi(osnova.copy(
            naziv = naziv.trim(), jacina = jacina.trim().ifEmpty { null },
            vremena = v.joinToString(","), putaDnevno = maxOf(v.size, 1),
            danUTjednu = if (tjedno) danUTjednu else null,
            kolicina = zaliha.replace(',', '.').toDoubleOrNull() ?: 0.0,
            pragDana = prag.toIntOrNull() ?: 7,
            razlog = if (pocetna == null) osnova.razlog else razlog.trim().ifEmpty { null },
            opis = if (pocetna == null) osnova.opis else opis.trim().ifEmpty { null },
            aktivna = aktivna,
            receptDo = if (pocetna == null) osnova.receptDo else if (imaRecept) receptDo else null,
        ))
    }

    Dialog(onDismissRequest = onOdustani, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (pocetna == null) R.string.nova_terapija else R.string.uredi_terapiju)) },
                    navigationIcon = { TextButton(onClick = onOdustani) { Text(stringResource(R.string.odustani)) } },
                    actions = { TextButton(onClick = { spremi() }, enabled = valjano) { Text(stringResource(R.string.spremi)) } },
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                   verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Odjeljak(stringResource(R.string.lijek)) {
                    OutlinedTextField(naziv, { naziv = it }, label = { Text(stringResource(R.string.naziv)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(jacina, { jacina = it }, label = { Text(stringResource(R.string.jacina)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Odjeljak(stringResource(R.string.rezim),
                         napomena = if (tjedno) stringResource(R.string.tjedno_napomena) else if (pocetna == null) stringResource(R.string.vremena_napomena) else null) {
                    Text(stringResource(R.string.ucestalost), style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = !tjedno, onClick = { tjedno = false }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                            Text(stringResource(R.string.svaki_dan))
                        }
                        SegmentedButton(selected = tjedno, onClick = { tjedno = true }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                            Text(stringResource(R.string.jednom_tjedno))
                        }
                    }
                    if (tjedno) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dan_u_tjednu), Modifier.weight(1f))
                            Box {
                                TextButton(onClick = { biracDana = true }) { Text(imeDana(danUTjednu)) }
                                DropdownMenu(expanded = biracDana, onDismissRequest = { biracDana = false }) {
                                    redoslijedDana().forEach { d ->
                                        DropdownMenuItem(text = { Text(imeDana(d)) }, onClick = { danUTjednu = d; biracDana = false })
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(vremena, { vremena = it }, label = { Text(stringResource(R.string.vremena_npr)) }, singleLine = true,
                                      isError = parsirano == null, modifier = Modifier.fillMaxWidth())
                    if (pocetna != null) {
                        OutlinedTextField(razlog, { razlog = it }, label = { Text(stringResource(R.string.razlog)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                Odjeljak(stringResource(R.string.zaliha)) {
                    OutlinedTextField(zaliha, { zaliha = it }, label = { Text(stringResource(R.string.broj_komada)) }, singleLine = true,
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(prag, { prag = it }, label = { Text(stringResource(R.string.upozori_dana)) }, singleLine = true,
                                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
                if (pocetna != null) {
                    Odjeljak(stringResource(R.string.recept)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.prati_recept), Modifier.weight(1f))
                            Switch(checked = imaRecept, onCheckedChange = { imaRecept = it })
                        }
                        if (imaRecept) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.recept_vrijedi_do), Modifier.weight(1f))
                                TextButton(onClick = { biracDatuma = true }) { Text(dan(receptDo)) }
                            }
                        }
                    }
                    Odjeljak(stringResource(R.string.opis)) {
                        OutlinedTextField(opis, { opis = it }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.aktivna_terapija), Modifier.weight(1f))
                        Switch(checked = aktivna, onCheckedChange = { aktivna = it })
                    }
                }
            }
        }
    }

    if (biracDatuma) {
        val stanje = rememberDatePickerState(initialSelectedDateMillis = lokalnoUUtc(receptDo))
        DatePickerDialog(
            onDismissRequest = { biracDatuma = false },
            confirmButton = {
                TextButton(onClick = { stanje.selectedDateMillis?.let { receptDo = utcULokalno(it) }; biracDatuma = false }) {
                    Text(stringResource(R.string.gotovo))
                }
            },
            dismissButton = { TextButton(onClick = { biracDatuma = false }) { Text(stringResource(R.string.odustani)) } },
        ) { DatePicker(state = stanje) }
    }
}
