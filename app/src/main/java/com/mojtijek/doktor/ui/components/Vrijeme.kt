@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mojtijek.doktor.R
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Redak s datumom i vremenom (iOS `DatePicker` bez ograničenja): dva gumba, svaki otvara svoj birač. */
@Composable
fun DatumVrijemeRedak(naslov: String, ms: Long, onPromjena: (Long) -> Unit) {
    val context = LocalContext.current
    var biracDatuma by remember { mutableStateOf(false) }
    var biracVremena by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(naslov, Modifier.weight(1f))
        TextButton(onClick = { biracDatuma = true }) { Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))) }
        TextButton(onClick = { biracVremena = true }) { Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))) }
    }
    if (biracDatuma) {
        // Birač datuma radi u UTC ponoći; iz njega se uzima samo kalendarski dan, vrijeme ostaje.
        val l = Calendar.getInstance().apply { timeInMillis = ms }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
        }
        val stanje = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { biracDatuma = false },
            confirmButton = {
                TextButton(onClick = {
                    stanje.selectedDateMillis?.let { odabrano ->
                        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = odabrano }
                        onPromjena(Calendar.getInstance().apply {
                            timeInMillis = ms
                            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
                        }.timeInMillis)
                    }
                    biracDatuma = false
                }) { Text(stringResource(R.string.gotovo)) }
            },
            dismissButton = { TextButton(onClick = { biracDatuma = false }) { Text(stringResource(R.string.odustani)) } },
        ) { DatePicker(state = stanje) }
    }
    if (biracVremena) {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        val stanje = rememberTimePickerState(initialHour = c.get(Calendar.HOUR_OF_DAY), initialMinute = c.get(Calendar.MINUTE),
                                             is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { biracVremena = false },
            text = { TimePicker(state = stanje) },
            confirmButton = {
                TextButton(onClick = {
                    onPromjena(Calendar.getInstance().apply {
                        timeInMillis = ms
                        set(Calendar.HOUR_OF_DAY, stanje.hour); set(Calendar.MINUTE, stanje.minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis)
                    biracVremena = false
                }) { Text(stringResource(R.string.gotovo)) }
            },
            dismissButton = { TextButton(onClick = { biracVremena = false }) { Text(stringResource(R.string.odustani)) } },
        )
    }
}

/** Brojčani izbor s − i + (iOS `Stepper`). */
@Composable
fun Brojac(natpis: String, vrijednost: Int, raspon: IntRange, onPromjena: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(natpis, Modifier.weight(1f))
        IconButton(onClick = { onPromjena(vrijednost - 1) }, enabled = vrijednost > raspon.first) {
            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "−", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { onPromjena(vrijednost + 1) }, enabled = vrijednost < raspon.last) {
            Icon(Icons.Filled.AddCircleOutline, contentDescription = "+", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
