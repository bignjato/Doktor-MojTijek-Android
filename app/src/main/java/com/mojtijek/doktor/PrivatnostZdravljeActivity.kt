package com.mojtijek.doktor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.ui.theme.DoktorMojTijekTheme

/** Health Connect ovaj ekran otvara iz dijaloga dozvola („kako aplikacija koristi podatke”). */
class PrivatnostZdravljeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DoktorMojTijekTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.hc_privatnost_naslov), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.hc_privatnost))
                        TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://infobot.hr/mojtijek/privatnost/"))) }) {
                            Text(stringResource(R.string.politika_privatnosti))
                        }
                    }
                }
            }
        }
    }
}
