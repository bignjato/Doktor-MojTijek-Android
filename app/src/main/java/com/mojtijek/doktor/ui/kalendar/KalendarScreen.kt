@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mojtijek.doktor.ui.kalendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.data.DogadjajEntity
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.Brojac
import com.mojtijek.doktor.ui.components.DatumVrijemeRedak
import com.mojtijek.doktor.ui.components.FormaDialog
import com.mojtijek.doktor.ui.components.FormaOdjeljak
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.Pill
import com.mojtijek.doktor.ui.theme.Mt
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

// Kalendar pregleda odabranog člana (iOS `FamilyCalendarView(focusClan:)`, `EventCard`, `EventForm`).
// Još nema: zahtjev za uputnicu e-poštom i upis u Kalendar telefona.

private fun dan(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

/** Vrijeme događaja živi u `vrijeme` ("HH:mm"); `datum` može biti i ponoć (kopija s weba). */
private fun trenutak(d: DogadjajEntity): Long {
    val hm = d.vrijeme?.let { DoseSchedule.normalize(it) } ?: return d.datum
    val c = Calendar.getInstance().apply { timeInMillis = d.datum }
    if (c.get(Calendar.HOUR_OF_DAY) != 0 || c.get(Calendar.MINUTE) != 0) return d.datum
    c.set(Calendar.HOUR_OF_DAY, hm.substringBefore(":").toInt()); c.set(Calendar.MINUTE, hm.substringAfter(":").toInt())
    return c.timeInMillis
}

@Composable
fun KalendarScreen(vm: MojTijekViewModel, onNavigateBack: () -> Unit) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val dogadjaji by remember(vm) { vm.dogadjajiZaAktivnog() }.collectAsState(initial = emptyList())
    var novi by remember { mutableStateOf(false) }
    var zaUrediti by remember { mutableStateOf<DogadjajEntity?>(null) }

    val danas = DoseSchedule.dayStart()
    val nadolazece = dogadjaji.filter { it.datum >= danas && it.status != "otkazano" && it.status != "obavljeno" }.sortedBy { it.datum }
    val proslo = dogadjaji.filter { (it.datum < danas || it.status == "obavljeno") && it.status != "otkazano" }.sortedByDescending { it.datum }.take(30)
    val clanPoId = clanovi.associateBy { it.id }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
                Text(stringResource(R.string.kalendar), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { novi = true }, enabled = clanovi.isNotEmpty()) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.novi_dogadjaj), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text(stringResource(R.string.nadolazece), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (nadolazece.isEmpty()) item { MtCard { Text(stringResource(R.string.nema_dogadjaja), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(nadolazece, key = { it.id }) { d -> Kartica(d, clanPoId[d.clanId], vm) { zaUrediti = d } }
        if (proslo.isNotEmpty()) {
            item { Text(stringResource(R.string.proslo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }
            items(proslo, key = { "p" + it.id }) { d -> Kartica(d, clanPoId[d.clanId], vm) { zaUrediti = d } }
        }
    }

    if (novi) Forma(null, clanovi, aktivniId, onOdustani = { novi = false }, onSpremi = { vm.dodajDogadjaj(it); novi = false })
    zaUrediti?.let { d -> Forma(d, clanovi, d.clanId, onOdustani = { zaUrediti = null }, onSpremi = { vm.azurirajDogadjaj(it); zaUrediti = null }) }
}

@Composable
private fun Kartica(d: DogadjajEntity, clan: ClanEntity?, vm: MojTijekViewModel, onUredi: () -> Unit) {
    var izbornik by remember { mutableStateOf(false) }
    val obavljeno = d.status == "obavljeno"
    fun prebaciObavljeno() = vm.azurirajDogadjaj(d.copy(status = if (obavljeno) "planirano" else "obavljeno"))
    val boja = runCatching { Color(android.graphics.Color.parseColor(clan?.boja ?: "#0E9488")) }.getOrDefault(Mt.Teal)

    Box {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).combinedClickable(onClick = {}, onLongClick = { izbornik = true })) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(5.dp).fillMaxHeight().background(boja))
                Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.naslov, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(if (obavljeno) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, contentDescription = stringResource(R.string.oznaci_obavljeno),
                             tint = if (obavljeno) Mt.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.size(26.dp).clip(CircleShape).clickable { prebaciObavljeno() })
                    }
                    Text(listOfNotNull(clan?.ime, dan(d.datum), d.vrijeme?.takeIf { it.isNotBlank() }, d.lokacija?.takeIf { it.isNotBlank() }).joinToString(" · "),
                         style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    d.priprema?.takeIf { it.isNotBlank() }?.let { Text("📋 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    d.vozac?.takeIf { it.isNotBlank() }?.let { Text("🚗 $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (d.uputnicaPotrebna && !d.uputnicaIzdana) {
                        if (d.uputnicaZatrazena) {
                            Pill(stringResource(R.string.ceka_uputnica_oznaci), Mt.Orange,
                                 Modifier.clip(CircleShape).clickable { vm.azurirajDogadjaj(d.copy(uputnicaIzdana = true)) })
                        } else {
                            Pill(stringResource(R.string.treba_uputnica), Mt.Orange)
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = izbornik, onDismissRequest = { izbornik = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.uredi_dogadjaj_izbornik)) }, onClick = { izbornik = false; onUredi() })
            DropdownMenuItem(text = { Text(stringResource(if (obavljeno) R.string.vrati_u_planirano else R.string.oznaci_obavljeno)) },
                             onClick = { izbornik = false; prebaciObavljeno() })
            if (d.uputnicaPotrebna) {
                DropdownMenuItem(text = { Text(stringResource(if (d.uputnicaIzdana) R.string.uputnica_nije_izdana else R.string.uputnica_je_izdana)) }, onClick = {
                    izbornik = false
                    // Poništavanje vraća na početak: ni zatražena ni izdana.
                    vm.azurirajDogadjaj(if (d.uputnicaIzdana) d.copy(uputnicaIzdana = false, uputnicaZatrazena = false) else d.copy(uputnicaIzdana = true))
                })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.obrisi), color = Mt.Red) }, onClick = { izbornik = false; vm.obrisiDogadjaj(d) })
        }
    }
}

@Composable
private fun Forma(pocetni: DogadjajEntity?, clanovi: List<ClanEntity>, odabraniClan: String?, onOdustani: () -> Unit, onSpremi: (DogadjajEntity) -> Unit) {
    var clanId by remember { mutableStateOf(odabraniClan?.takeIf { id -> clanovi.any { it.id == id } } ?: clanovi.firstOrNull()?.id) }
    var naslov by remember { mutableStateOf(pocetni?.naslov ?: "") }
    var datum by remember { mutableStateOf(pocetni?.let { trenutak(it) } ?: System.currentTimeMillis()) }
    var lokacija by remember { mutableStateOf(pocetni?.lokacija ?: "") }
    var uputnica by remember { mutableStateOf(pocetni?.uputnicaPotrebna ?: false) }
    var priprema by remember { mutableStateOf(pocetni?.priprema ?: "") }
    var dana by remember { mutableStateOf(maxOf(0, pocetni?.podsjetnikDana ?: 3)) }
    var vozac by remember { mutableStateOf(pocetni?.vozac ?: "") }
    var biracClana by remember { mutableStateOf(false) }

    FormaDialog(stringResource(if (pocetni == null) R.string.novi_dogadjaj else R.string.uredi_dogadjaj),
                valjano = naslov.isNotBlank() && clanId != null, onOdustani = onOdustani, onSpremi = {
        val id = clanId ?: return@FormaDialog
        val c = Calendar.getInstance().apply { timeInMillis = datum }
        val osnova = pocetni ?: DogadjajEntity(clanId = id)
        onSpremi(osnova.copy(clanId = id, naslov = naslov.trim(), datum = datum,
                             vrijeme = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)),
                             lokacija = lokacija.trim().ifEmpty { null }, priprema = priprema.trim().ifEmpty { null },
                             uputnicaPotrebna = uputnica, podsjetnikDana = dana, vozac = vozac.trim().ifEmpty { null }))
    }) {
        FormaOdjeljak(stringResource(R.string.pregled)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.clan), Modifier.weight(1f))
                Box {
                    TextButton(onClick = { biracClana = true }) { Text(clanovi.firstOrNull { it.id == clanId }?.ime ?: "—") }
                    DropdownMenu(expanded = biracClana, onDismissRequest = { biracClana = false }) {
                        clanovi.forEach { c -> DropdownMenuItem(text = { Text(c.ime) }, onClick = { clanId = c.id; biracClana = false }) }
                    }
                }
            }
            OutlinedTextField(naslov, { naslov = it }, label = { Text(stringResource(R.string.naziv_pregleda)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            DatumVrijemeRedak(stringResource(R.string.datum_i_vrijeme), datum) { datum = it }
            OutlinedTextField(lokacija, { lokacija = it }, label = { Text(stringResource(R.string.lokacija)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        FormaOdjeljak(stringResource(R.string.priprema)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.treba_uputnica), Modifier.weight(1f))
                Switch(checked = uputnica, onCheckedChange = { uputnica = it })
            }
            OutlinedTextField(priprema, { priprema = it }, label = { Text(stringResource(R.string.priprema_pretrage)) }, modifier = Modifier.fillMaxWidth())
        }
        FormaOdjeljak(stringResource(R.string.podsjetnik)) {
            Brojac(stringResource(R.string.podsjeti_dana_ranije, dana), dana, 0..30) { dana = it }
            OutlinedTextField(vozac, { vozac = it }, label = { Text(stringResource(R.string.tko_vozi)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(if (dana == 0) stringResource(R.string.podsjetnik_na_dan) else stringResource(R.string.podsjetnik_dana_prije, dana),
                 style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
