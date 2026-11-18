@file:OptIn(ExperimentalFoundationApi::class)

package com.mojtijek.doktor.ui.profil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.CijepljenjeEntity
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.DatumRedak
import com.mojtijek.doktor.ui.components.FormaDialog
import com.mojtijek.doktor.ui.components.FormaOdjeljak
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.Pill
import com.mojtijek.doktor.ui.theme.Mt
import java.text.DateFormat
import java.util.Date

// Cijepljenja člana (iOS `VaccinationsView` / `AddVaccinationView`). Podsjetnik na sljedeću dozu dolazi s podsjetnicima;
// do tada se datum samo bilježi i prikazuje.

private fun dan(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

@Composable
fun CijepljenjaScreen(vm: MojTijekViewModel, clanId: String, onNatrag: () -> Unit) {
    BackHandler(onBack = onNatrag)
    val lista by remember(vm) { vm.cijepljenjaZaAktivnog() }.collectAsState(initial = emptyList())
    var novo by remember { mutableStateOf(false) }
    var izbornikZa by remember { mutableStateOf<String?>(null) }
    val sada = System.currentTimeMillis()

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNatrag) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
                Text(stringResource(R.string.cijepljenja), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { novo = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.novo_cijepljenje), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (lista.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Vaccines, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                    Text(stringResource(R.string.nema_cijepljenja), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.cijepljenja_opis), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        items(lista.sortedByDescending { it.datum ?: 0L }, key = { it.id }) { c ->
            Box {
                MtCard(Modifier.clip(RoundedCornerShape(20.dp)).combinedClickable(onClick = {}, onLongClick = { izbornikZa = c.id })) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Mt.Blue), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Vaccines, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(c.naziv, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val opis = listOfNotNull(c.datum?.let { dan(it) }, c.napomena?.takeIf { it.isNotBlank() }).joinToString(" · ")
                            if (opis.isNotEmpty()) Text(opis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            c.sljedece?.let { Pill(stringResource(R.string.sljedeca_datum, dan(it)), if (it < sada) Mt.Orange else Mt.Teal) }
                        }
                    }
                }
                DropdownMenu(expanded = izbornikZa == c.id, onDismissRequest = { izbornikZa = null }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.obrisi), color = Mt.Red) }, onClick = { izbornikZa = null; vm.obrisiCijepljenje(c) })
                }
            }
        }
        if (lista.isNotEmpty()) {
            item { Text(stringResource(R.string.dugi_pritisak_brisanje), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (novo) {
        var naziv by remember { mutableStateOf("") }
        var datum by remember { mutableStateOf(DoseSchedule.dayStart()) }
        var imaSljedecu by remember { mutableStateOf(false) }
        var sljedeca by remember { mutableStateOf(DoseSchedule.dayStart()) }
        var napomena by remember { mutableStateOf("") }
        FormaDialog(stringResource(R.string.novo_cijepljenje), valjano = naziv.isNotBlank(), onOdustani = { novo = false }, onSpremi = {
            vm.dodajCijepljenje(CijepljenjeEntity(clanId = clanId, naziv = naziv.trim(), datum = datum, sljedece = if (imaSljedecu) sljedeca else null,
                                                  napomena = napomena.trim().ifEmpty { null }))
            novo = false
        }) {
            FormaOdjeljak(stringResource(R.string.cjepivo)) {
                OutlinedTextField(naziv, { naziv = it }, label = { Text(stringResource(R.string.cjepivo_naziv)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DatumRedak(stringResource(R.string.datum_primanja), datum) { datum = it }
            }
            FormaOdjeljak(stringResource(R.string.sljedeca_doza_naslov)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.upisi_sljedecu_dozu), Modifier.weight(1f))
                    Switch(checked = imaSljedecu, onCheckedChange = { imaSljedecu = it })
                }
                if (imaSljedecu) DatumRedak(stringResource(R.string.datum), sljedeca) { sljedeca = it }
            }
            FormaOdjeljak(stringResource(R.string.napomena)) {
                OutlinedTextField(napomena, { napomena = it }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
