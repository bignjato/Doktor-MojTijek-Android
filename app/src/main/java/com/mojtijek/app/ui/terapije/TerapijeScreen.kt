package com.mojtijek.app.ui.terapije

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.shared.domain.model.Terapija

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerapijeScreen() {
    var terapije by remember { mutableStateOf(sampleTerapije()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terapije", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (terapije.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nema aktivnih terapija", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(terapije) { terapija ->
                    TerapijaCard(terapija)
                }
            }
        }
    }
}

@Composable
private fun TerapijaCard(terapija: Terapija) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (terapija.aktivna)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = terapija.naziv,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (terapija.aktivna) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Aktivna",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = terapija.dijagnoza,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Trajanje: ${terapija.trajanjeDana} dana",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (terapija.napomene.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = terapija.napomene,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sampleTerapije(): List<Terapija> = listOf(
    Terapija(
        id = "1",
        naziv = "Terapija A",
        dijagnoza = "Dijagnoza: Anksioznost",
        trajanjeDana = 30,
        aktivna = true,
        napomene = "Uzimati ujutro s hranom"
    ),
    Terapija(
        id = "2",
        naziv = "Terapija B",
        dijagnoza = "Dijagnoza: Insomnija",
        trajanjeDana = 14,
        aktivna = false,
        napomene = "Prije spavanja, ne voziti"
    )
)
