package com.mojtijek.doktor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.DoseSchedule
import com.mojtijek.doktor.ui.theme.Mt
import java.text.DateFormat
import java.util.Date

fun danVrijeme(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

/** Potvrda jedne doze (iOS `AppModalPresenter.confirmDose`) — ista na ekranu Danas i u rasporedu terapija. */
@Composable
fun PotvrdiDozuDialog(doza: DoseSchedule.ScheduledDose, onPotvrdi: () -> Unit, onOdustani: () -> Unit) {
    AlertDialog(
        onDismissRequest = onOdustani,
        title = { Text(stringResource(R.string.potvrdi_dozu)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(doza.terapija.naziv, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PodatakRed(stringResource(R.string.planirano), danVrijeme(doza.plannedMillis))
                PodatakRed(stringResource(R.string.vrijeme_uzimanja), danVrijeme(System.currentTimeMillis()))
                if (DoseSchedule.dayStart(doza.plannedMillis) != DoseSchedule.dayStart()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Mt.Orange, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.doza_drugi_dan), color = Mt.Orange)
                    }
                }
                Text(stringResource(R.string.potvrdi_samo_ako), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onPotvrdi) { Text(stringResource(R.string.potvrdi_uzimanje)) } },
        dismissButton = { TextButton(onClick = onOdustani) { Text(stringResource(R.string.odustani)) } },
    )
}

@Composable
fun PodatakRed(naziv: String, vrijednost: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(naziv, Modifier.weight(1f))
        Text(vrijednost, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
