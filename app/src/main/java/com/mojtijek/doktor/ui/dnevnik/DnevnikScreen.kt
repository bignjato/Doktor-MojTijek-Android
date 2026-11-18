@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.dnevnik

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.DnevnikUnosEntity
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.Brojac
import com.mojtijek.doktor.ui.components.DatumVrijemeRedak
import com.mojtijek.doktor.ui.components.FormaDialog
import com.mojtijek.doktor.ui.components.FormaOdjeljak
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.theme.Mt
import java.text.DateFormat
import java.util.Date

// Dnevnik člana (iOS `DiaryView` / `AddDiaryEntryView`): filtar po vrsti, zapisi grupirani po danu.

private val VRSTE = listOf("simptom" to R.string.simptom, "lijek" to R.string.lijek, "navika" to R.string.navika, "biljeska" to R.string.biljeska)

@Composable
fun DnevnikScreen(vm: MojTijekViewModel, onNavigateBack: () -> Unit) {
    val aktivniId by vm.aktivniClanId.collectAsState()
    val unosi by remember(vm) { vm.dnevnikZaAktivnog() }.collectAsState(initial = emptyList())
    var filtar by rememberSaveable { mutableStateOf("") }
    var novi by remember { mutableStateOf(false) }

    val sada = System.currentTimeMillis()
    val danas = DoseSchedule.dayStart(sada)
    val jucer = DoseSchedule.dayStart(danas - 1)
    val grupe = remember(unosi, filtar) {
        unosi.filter { filtar.isEmpty() || it.vrsta == filtar }.sortedByDescending { it.datum ?: 0L }
            .groupBy { DoseSchedule.dayStart(it.datum ?: sada) }.toList().sortedByDescending { it.first }
    }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
                Text(stringResource(R.string.dnevnik), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { novi = true }, enabled = aktivniId != null) {
                    Icon(Icons.Filled.AddCircle, contentDescription = stringResource(R.string.novi_zapis), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            val filtri = listOf("" to R.string.sve, "simptom" to R.string.simptomi, "lijek" to R.string.lijekovi, "navika" to R.string.navike)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                filtri.forEachIndexed { i, (kljuc, naziv) ->
                    SegmentedButton(selected = filtar == kljuc, onClick = { filtar = kljuc }, icon = {},
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = filtri.size)) {
                        Text(stringResource(naziv), maxLines = 1)
                    }
                }
            }
        }
        if (grupe.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                    Text(stringResource(R.string.nema_zapisa), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.dnevnik_opis), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        items(grupe, key = { it.first }) { (dan, zapisi) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(when (dan) {
                         danas -> stringResource(R.string.tab_danas)
                         jucer -> stringResource(R.string.jucer)
                         else -> DateFormat.getDateInstance(DateFormat.LONG).format(Date(dan))
                     }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                MtCard {
                    Column {
                        zapisi.forEachIndexed { i, e ->
                            Red(e)
                            if (i < zapisi.size - 1) HorizontalDivider(Modifier.padding(start = 42.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    val clanId = aktivniId
    if (novi && clanId != null) {
        var vrsta by remember { mutableStateOf("simptom") }
        var naslov by remember { mutableStateOf("") }
        var napomena by remember { mutableStateOf("") }
        var jacina by remember { mutableStateOf(1) }
        var datum by remember { mutableStateOf(System.currentTimeMillis()) }
        FormaDialog(stringResource(R.string.novi_zapis), valjano = naslov.isNotBlank() || napomena.isNotBlank(), onOdustani = { novi = false }, onSpremi = {
            vm.dodajDnevnikUnos(DnevnikUnosEntity(clanId = clanId, datum = datum, vrsta = vrsta, naslov = naslov.trim().ifEmpty { null },
                                                  tekst = napomena.trim().ifEmpty { null }, jacina = if (vrsta == "simptom") jacina else null))
            novi = false
        }) {
            FormaOdjeljak(stringResource(R.string.vrsta)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    VRSTE.forEachIndexed { i, (kljuc, naziv) ->
                        SegmentedButton(selected = vrsta == kljuc, onClick = { vrsta = kljuc }, icon = {},
                                        shape = SegmentedButtonDefaults.itemShape(index = i, count = VRSTE.size)) {
                            Text(stringResource(naziv), maxLines = 1)
                        }
                    }
                }
            }
            FormaOdjeljak(stringResource(R.string.zapis)) {
                OutlinedTextField(naslov, { naslov = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                                  label = { Text(stringResource(if (vrsta == "simptom") R.string.npr_glavobolja else R.string.naslov)) })
                OutlinedTextField(napomena, { napomena = it }, label = { Text(stringResource(R.string.napomena)) }, modifier = Modifier.fillMaxWidth())
                if (vrsta == "simptom") Brojac(stringResource(R.string.jacina_od_5, jacina), jacina, 1..5) { jacina = it }
                DatumVrijemeRedak(stringResource(R.string.vrijeme), datum) { datum = it }
            }
        }
    }
}

@Composable
private fun Red(e: DnevnikUnosEntity) {
    val boja = when (e.vrsta) { "simptom" -> Mt.Red; "lijek" -> Mt.Purple; "navika" -> Mt.Green; else -> Mt.Teal }
    val ikona = when (e.vrsta) {
        "simptom" -> Icons.Filled.Healing
        "lijek" -> Icons.Filled.Medication
        "navika" -> Icons.AutoMirrored.Filled.DirectionsRun
        else -> Icons.AutoMirrored.Filled.Notes
    }
    val zamjenski = stringResource(VRSTE.firstOrNull { it.first == e.vrsta }?.second ?: R.string.zapis)
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(boja.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(e.naslov?.takeIf { it.isNotBlank() } ?: zamjenski, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            e.tekst?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            e.jacina?.let { Text(stringResource(R.string.jacina_od_5, it), style = MaterialTheme.typography.labelSmall, color = boja) }
        }
        Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.datum ?: System.currentTimeMillis())),
             style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
