@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mojtijek.doktor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.theme.Mt
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

// Obitelj: izbor člana (iOS `FamilyPickerView`) i forma člana (iOS `AddMemberView` / `EditMemberView`).
// Još nema: „Odaberi iz imenika” za hitni kontakt i ograničenje broja članova (Premium).

private val BOJE = listOf("#0E9488", "#8B5CF6", "#0EA5E9", "#F59E0B", "#16A34A")

fun dobGodina(datumRodjenja: Long?, sada: Long = System.currentTimeMillis()): Int? {
    if (datumRodjenja == null) return null
    val r = Calendar.getInstance().apply { timeInMillis = datumRodjenja }
    val s = Calendar.getInstance().apply { timeInMillis = sada }
    var god = s.get(Calendar.YEAR) - r.get(Calendar.YEAR)
    if (s.get(Calendar.DAY_OF_YEAR) < r.get(Calendar.DAY_OF_YEAR)) god--
    return god
}

/** Redak s datumom koji otvara birač. Birač radi u UTC ponoći; sprema se lokalna ponoć istog kalendarskog dana. */
@Composable
fun DatumRedak(naslov: String, ms: Long, onPromjena: (Long) -> Unit) {
    var birac by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(naslov, Modifier.weight(1f))
        TextButton(onClick = { birac = true }) { Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))) }
    }
    if (birac) {
        val l = Calendar.getInstance().apply { timeInMillis = ms }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
        }
        val stanje = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { birac = false },
            confirmButton = {
                TextButton(onClick = {
                    stanje.selectedDateMillis?.let { odabrano ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = odabrano }
                        onPromjena(Calendar.getInstance().apply {
                            clear(); set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis)
                    }
                    birac = false
                }) { Text(stringResource(R.string.gotovo)) }
            },
            dismissButton = { TextButton(onClick = { birac = false }) { Text(stringResource(R.string.odustani)) } },
        ) { DatePicker(state = stanje) }
    }
}

@Composable
fun FormaOdjeljak(naslov: String, sadrzaj: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(naslov.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sadrzaj()
    }
}

/** Forma preko cijelog ekrana s „Odustani” i „Spremi” na vrhu, kao iOS list s formom. */
@Composable
fun FormaDialog(naslov: String, valjano: Boolean, onOdustani: () -> Unit, onSpremi: () -> Unit, sadrzaj: @Composable () -> Unit) {
    Dialog(onDismissRequest = onOdustani, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(naslov) },
                    navigationIcon = { TextButton(onClick = onOdustani) { Text(stringResource(R.string.odustani)) } },
                    actions = { TextButton(onClick = onSpremi, enabled = valjano) { Text(stringResource(R.string.spremi)) } },
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                   verticalArrangement = Arrangement.spacedBy(20.dp)) { sadrzaj() }
        }
    }
}

private fun bezNule(v: Double?): String = when {
    v == null -> ""
    v % 1.0 == 0.0 -> v.toLong().toString()
    else -> v.toString()
}

/** Novi član (`pocetni == null`) ili uređivanje. `tezina` je zadnja izmjerena; nova vrijednost vraća se kroz `onSpremi`. */
@Composable
fun ClanForma(pocetni: ClanEntity?, tezina: Double? = null, onOdustani: () -> Unit, onSpremi: (ClanEntity, Double?) -> Unit) {
    var ime by remember { mutableStateOf(pocetni?.ime ?: "") }
    var rodjen by remember {
        mutableStateOf(pocetni?.datumRodjenja ?: Calendar.getInstance().apply { add(Calendar.YEAR, -40) }.timeInMillis)
    }
    var datumDiran by remember { mutableStateOf(pocetni == null || pocetni.datumRodjenja != null) }
    var spol by remember { mutableStateOf(if (pocetni == null) "žensko" else pocetni.spol ?: "") }
    var krv by remember { mutableStateOf(pocetni?.krvnaGrupa ?: "") }
    var visina by remember { mutableStateOf(bezNule(pocetni?.visina)) }
    var kg by remember { mutableStateOf(bezNule(tezina)) }
    var alergije by remember { mutableStateOf(pocetni?.alergije ?: "") }
    var bolesti by remember { mutableStateOf(pocetni?.kronicneBolesti ?: "") }
    var lijecnik by remember { mutableStateOf(pocetni?.lijecnik ?: "") }
    var email by remember { mutableStateOf(pocetni?.lijecnikEmail ?: "") }
    var kontakt by remember { mutableStateOf(pocetni?.hitniKontakt ?: "") }
    var telefon by remember { mutableStateOf(pocetni?.hitniTelefon ?: "") }
    var hitna by remember { mutableStateOf(pocetni?.hitnaNapomena ?: "") }
    var biracSpola by remember { mutableStateOf(false) }

    val v = visina.replace(',', '.').toDoubleOrNull()
    val w = kg.replace(',', '.').toDoubleOrNull()
    val itm = if (v != null && v > 0 && w != null && w > 0) w / ((v / 100) * (v / 100)) else null
    fun prazno(s: String): String? = s.trim().ifEmpty { null }

    FormaDialog(
        naslov = stringResource(if (pocetni == null) R.string.novi_clan else R.string.uredi_clana),
        valjano = ime.isNotBlank(), onOdustani = onOdustani,
        onSpremi = {
            val osnova = pocetni ?: ClanEntity(boja = BOJE.random())
            onSpremi(osnova.copy(
                ime = ime.trim(), datumRodjenja = if (datumDiran) rodjen else osnova.datumRodjenja, spol = prazno(spol), krvnaGrupa = prazno(krv),
                visina = v, alergije = prazno(alergije), kronicneBolesti = prazno(bolesti), lijecnik = prazno(lijecnik),
                lijecnikEmail = prazno(email), hitniKontakt = prazno(kontakt), hitniTelefon = prazno(telefon),
                hitnaNapomena = if (pocetni == null) osnova.hitnaNapomena else prazno(hitna),
            ), w)
        },
    ) {
        FormaOdjeljak(stringResource(R.string.osnovno)) {
            OutlinedTextField(ime, { ime = it }, label = { Text(stringResource(R.string.ime_i_prezime)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            DatumRedak(stringResource(R.string.datum_rodjenja), rodjen) { rodjen = it; datumDiran = true }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val spolovi = listOf("žensko" to R.string.spol_zensko, "muško" to R.string.spol_musko, "" to R.string.spol_neodredjeno)
                Text(stringResource(R.string.spol), Modifier.weight(1f))
                Box {
                    TextButton(onClick = { biracSpola = true }) { Text(stringResource((spolovi.firstOrNull { it.first == spol } ?: spolovi.last()).second)) }
                    DropdownMenu(expanded = biracSpola, onDismissRequest = { biracSpola = false }) {
                        spolovi.forEach { (kljuc, naziv) -> DropdownMenuItem(text = { Text(stringResource(naziv)) }, onClick = { spol = kljuc; biracSpola = false }) }
                    }
                }
            }
            OutlinedTextField(krv, { krv = it }, label = { Text(stringResource(R.string.krvna_grupa)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(visina, { visina = it }, label = { Text(stringResource(R.string.visina_cm)) }, singleLine = true,
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(kg, { kg = it }, label = { Text(stringResource(R.string.tezina_kg)) }, singleLine = true,
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            if (pocetni != null && itm != null) {
                val kategorija = stringResource(when {
                    itm < 18.5 -> R.string.itm_pothranjenost
                    itm < 25 -> R.string.itm_normalna
                    itm < 30 -> R.string.itm_prekomjerna
                    else -> R.string.itm_pretilost
                })
                Row {
                    Text(stringResource(R.string.itm), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1; maximumFractionDigits = 1 }.format(itm) + " · " + kategorija)
                }
            }
        }
        FormaOdjeljak(stringResource(R.string.medicinski_podaci)) {
            OutlinedTextField(alergije, { alergije = it }, label = { Text(stringResource(R.string.alergije)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bolesti, { bolesti = it }, label = { Text(stringResource(R.string.kronicne_bolesti)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lijecnik, { lijecnik = it }, label = { Text(stringResource(R.string.lijecnik)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.lijecnik_email)) }, singleLine = true,
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        }
        FormaOdjeljak(stringResource(R.string.ice_kontakt)) {
            OutlinedTextField(kontakt, { kontakt = it }, label = { Text(stringResource(R.string.ime_kontakta)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(telefon, { telefon = it }, label = { Text(stringResource(R.string.telefon)) }, singleLine = true,
                              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            if (pocetni != null) {
                OutlinedTextField(hitna, { hitna = it }, label = { Text(stringResource(R.string.hitna_napomena)) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Izbor člana obitelji: dodir odabire, dugi pritisak nudi uređivanje, pomicanje i brisanje. */
@Composable
fun ObiteljSheet(vm: MojTijekViewModel, clanovi: List<ClanEntity>, odabraniId: String, onZatvori: () -> Unit) {
    var novi by remember { mutableStateOf(false) }
    var zaUrediti by remember { mutableStateOf<ClanEntity?>(null) }
    var zaObrisati by remember { mutableStateOf<ClanEntity?>(null) }
    var izbornikZa by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onZatvori) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding().verticalScroll(rememberScrollState()),
               verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.obitelj), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { novi = true }) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = stringResource(R.string.dodaj_clana), tint = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onZatvori) { Text(stringResource(R.string.zatvori)) }
            }
            clanovi.forEach { c ->
                Box {
                    MtCard(Modifier.clip(RoundedCornerShape(20.dp))
                               .combinedClickable(onClick = { vm.odaberiClana(c.id); onZatvori() }, onLongClick = { izbornikZa = c.id })) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MemberAvatar(c, 50.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(c.ime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                dobGodina(c.datumRodjenja)?.let { Pill(stringResource(R.string.n_god, it), Color.Gray) }
                            }
                            if (c.id == odabraniId) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DropdownMenu(expanded = izbornikZa == c.id, onDismissRequest = { izbornikZa = null }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.uredi_clana)) }, onClick = { izbornikZa = null; zaUrediti = c })
                        if (clanovi.size > 1) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.pomakni_gore)) }, onClick = { izbornikZa = null; vm.pomakniClana(c, -1) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.pomakni_dolje)) }, onClick = { izbornikZa = null; vm.pomakniClana(c, 1) })
                        }
                        DropdownMenuItem(text = { Text(stringResource(R.string.obrisi_clana), color = Mt.Red) }, onClick = { izbornikZa = null; zaObrisati = c })
                    }
                }
            }
            if (clanovi.isNotEmpty()) {
                Text(stringResource(R.string.obitelj_dugi_pritisak), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (novi) ClanForma(null, onOdustani = { novi = false }, onSpremi = { c, tezina -> vm.spremiClana(c, tezina, novi = true); novi = false })
    zaUrediti?.let { c ->
        // Težina se u ovoj formi ne prikazuje unaprijed (mjerenja drugog člana nisu učitana); upisana vrijednost se sprema.
        ClanForma(c, onOdustani = { zaUrediti = null }, onSpremi = { novo, tezina -> vm.spremiClana(novo, tezina, novi = false); zaUrediti = null })
    }
    // iOS briše člana bez pitanja; ovdje se pita, jer brisanje odnosi sve podatke člana i ne može se poništiti.
    zaObrisati?.let { c ->
        AlertDialog(
            onDismissRequest = { zaObrisati = null },
            title = { Text(stringResource(R.string.obrisati_clana)) },
            text = { Text(c.ime + "\n\n" + stringResource(R.string.obrisati_clana_opis)) },
            confirmButton = { TextButton(onClick = { vm.obrisiClana(c); zaObrisati = null }) { Text(stringResource(R.string.obrisi_clana), color = Mt.Red) } },
            dismissButton = { TextButton(onClick = { zaObrisati = null }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}
