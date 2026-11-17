package com.mojtijek.doktor.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.ClanEntity
import com.mojtijek.doktor.data.MjerenjeEntity
import com.mojtijek.doktor.ui.theme.Mt
import java.text.NumberFormat

/** Kartica kao iOS `MTCard`: zaobljenje 20, sekundarna pozadina, unutarnji razmak 16. */
@Composable
fun MtCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

/** Oznaka kao iOS `Pill`. */
@Composable
fun Pill(text: String, color: Color = Mt.Teal, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
         modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.13f)).padding(horizontal = 9.dp, vertical = 5.dp))
}

/** Krug s inicijalima u boji člana (iOS `MemberAvatar`). */
@Composable
fun MemberAvatar(clan: ClanEntity, size: Dp = 44.dp) {
    val boja = runCatching { Color(android.graphics.Color.parseColor(clan.boja)) }.getOrDefault(Mt.Teal)
    val inicijali = clan.ime.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    Box(Modifier.size(size).clip(CircleShape).background(boja), contentAlignment = Alignment.Center) {
        Text(inicijali, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.32f).sp)
    }
}

/** Pokazatelj: naziv, jedinica, ikona i boja. Ključevi (`tip`) su isti kao na iOS-u (VITALI) — po njima se mjerenja prenose kopijom. */
class Vital(@StringRes val naziv: Int, val jedinica: String, val ikona: ImageVector, val boja: Color)

val VITALI: Map<String, Vital> = mapOf(
    "koraci" to Vital(R.string.vital_koraci, "", Icons.AutoMirrored.Filled.DirectionsWalk, Mt.Green),
    "puls" to Vital(R.string.vital_puls, "bpm", Icons.Filled.Favorite, Mt.Red),
    "tlak" to Vital(R.string.vital_tlak, "mmHg", Icons.Filled.MonitorHeart, Mt.Orange),
    "san" to Vital(R.string.vital_san, "h", Icons.Filled.Bedtime, Mt.Purple),
    "saturacija" to Vital(R.string.vital_saturacija, "%", Icons.Filled.Air, Mt.Blue),
    "secer" to Vital(R.string.vital_secer, "mmol/L", Icons.Filled.WaterDrop, Mt.Orange),
    "tezina" to Vital(R.string.vital_tezina, "kg", Icons.Filled.MonitorWeight, Mt.Green),
    "temperatura" to Vital(R.string.vital_temperatura, "°C", Icons.Filled.Thermostat, Mt.Purple),
)

@Composable
fun vitalNaziv(tip: String): String = VITALI[tip]?.let { stringResource(it.naziv) } ?: tip

fun formatMjerenja(m: MjerenjeEntity): String = when {
    m.tip == "tlak" && m.vrijednost2 != null -> "${m.vrijednost.toInt()}/${m.vrijednost2.toInt()}"
    m.tip == "koraci" -> NumberFormat.getIntegerInstance().format(m.vrijednost.toLong())
    else -> NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(m.vrijednost)
}
