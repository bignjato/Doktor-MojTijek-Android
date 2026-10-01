@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.dokumenti

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.DokumentEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DokumentiScreen(vm: MojTijekViewModel, onNavigateBack: () -> Unit = {}) {
    val dokumenti by vm.dokumentiZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedDokument by remember { mutableStateOf<DokumentEntity?>(null) }
    var showViewer by remember { mutableStateOf(false) }

    // Show document viewer full screen
    if (showViewer && selectedDokument != null) {
        DokumentViewerScreen(
            dokument = selectedDokument!!,
            onNavigateBack = { showViewer = false; selectedDokument = null },
            onEdit = { showViewer = false },
            onDelete = { 
                vm.obrisiDokument(it)
                showViewer = false
                selectedDokument = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kartoteka", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Natrag")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj dokument")
            }
        }
    ) { padding ->
        if (dokumenti.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nema dokumenata",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dodajte dokumente kao što su nalazi, recepti ili uputnice pomoću + gumba.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                    items(dokumenti) { dok ->
                        DokumentCard(
                            dokument = dok,
                            onClick = { 
                                selectedDokument = dok
                                showViewer = true
                            },
                            onDelete = { vm.obrisiDokument(it) }
                        )
                    }
            }
        }
    }

    if (showDialog) {
        NoviDokumentDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { dok ->
                vm.dodajDokument(dok)
                showDialog = false
            }
        )
    }
}

@Composable
private fun DokumentViewerScreen(
    dokument: DokumentEntity,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (DokumentEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(dokument.naziv ?: "Dokument", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Natrag")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, "Uredi")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Obriši")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Document header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            dokument.vrsta.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Date
                    dokument.datum?.let {
                        Text(
                            SimpleDateFormat("EEEE, d. MMMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Institution and Doctor
                    if (dokument.ustanova != null || dokument.lijecnik != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            dokument.ustanova?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Business,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            dokument.lijecnik?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Document body
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(20.dp)) {
                        val sadrzaj = dokument.objasnjenje ?: dokument.napomena
                        if (sadrzaj != null && sadrzaj.isNotBlank()) {
                            Text(
                                sadrzaj,
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.5
                            )
                        } else {
                            Text(
                                "Nema sadržaja dokumenta.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši dokument?") },
            text = { Text("Jeste li sigurni da želite obrisati ovaj dokument?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(dokument)
                    showDeleteDialog = false
                }) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }

    if (showEditDialog) {
        DokumentEditDialog(
            dokument = dokument,
            onDismiss = { showEditDialog = false },
            onSave = { /* Handle save in parent */ }
        )
    }
}

@Composable
private fun DokumentCard(
    dokument: DokumentEntity,
    onClick: () -> Unit,
    onDelete: (DokumentEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (dokument.vrsta) {
                        "nalaz" -> Icons.Filled.Description
                        "recept" -> Icons.Filled.Receipt
                        "uputnica" -> Icons.Filled.Assignment
                        "izvjesce" -> Icons.Filled.Assessment
                        else -> Icons.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    dokument.naziv ?: "Dokument",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dokument.vrsta.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    dokument.datum?.let {
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(it)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Obriši",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši dokument?") },
            text = { Text("Jeste li sigurni da želite obrisati ovaj dokument?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(dokument)
                    showDeleteDialog = false
                }) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoviDokumentDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (DokumentEntity) -> Unit
) {
    var naziv by remember { mutableStateOf("") }
    var vrsta by remember { mutableStateOf("nalaz") }
    var expanded by remember { mutableStateOf(false) }
    var datum by remember { mutableStateOf(System.currentTimeMillis()) }
    var ustanova by remember { mutableStateOf("") }
    var lijecnik by remember { mutableStateOf("") }
    var napomena by remember { mutableStateOf("") }

    val vrste = listOf("nalaz", "recept", "uputnica", "izvješće", "ostalo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi dokument") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = naziv,
                    onValueChange = { naziv = it },
                    label = { Text("Naziv dokumenta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vrsta.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vrsta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vrste.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    vrsta = v
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = ustanova,
                    onValueChange = { ustanova = it },
                    label = { Text("Ustanova") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lijecnik,
                    onValueChange = { lijecnik = it },
                    label = { Text("Liječnik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = napomena,
                    onValueChange = { napomena = it },
                    label = { Text("Napomena") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSpremi(
                        DokumentEntity(
                            clanId = clanId,
                            naziv = naziv,
                            vrsta = vrsta,
                            datum = datum,
                            ustanova = ustanova.ifBlank { null },
                            lijecnik = lijecnik.ifBlank { null },
                            napomena = napomena.ifBlank { null }
                        )
                    )
                }
            }) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DokumentEditDialog(
    dokument: DokumentEntity,
    onDismiss: () -> Unit,
    onSave: (DokumentEntity) -> Unit = {}
) {
    var naziv by remember { mutableStateOf(dokument.naziv ?: "") }
    var vrsta by remember { mutableStateOf(dokument.vrsta) }
    var expanded by remember { mutableStateOf(false) }
    var ustanova by remember { mutableStateOf(dokument.ustanova ?: "") }
    var lijecnik by remember { mutableStateOf(dokument.lijecnik ?: "") }
    var napomena by remember { mutableStateOf(dokument.napomena ?: "") }
    var objasnjenje by remember { mutableStateOf(dokument.objasnjenje ?: "") }

    val vrste = listOf("nalaz", "recept", "uputnica", "izvješće", "ostalo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalji dokumenta") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = naziv,
                        onValueChange = { naziv = it },
                        label = { Text("Naziv dokumenta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = vrsta.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vrsta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            vrste.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        vrsta = v
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = ustanova,
                        onValueChange = { ustanova = it },
                        label = { Text("Ustanova") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = lijecnik,
                        onValueChange = { lijecnik = it },
                        label = { Text("Liječnik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = napomena,
                        onValueChange = { napomena = it },
                        label = { Text("Napomena") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = objasnjenje,
                        onValueChange = { objasnjenje = it },
                        label = { Text("Objašnjenje") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naziv.isNotBlank()) {
                    onSave(
                        dokument.copy(
                            naziv = naziv,
                            vrsta = vrsta,
                            ustanova = ustanova.ifBlank { null },
                            lijecnik = lijecnik.ifBlank { null },
                            napomena = napomena.ifBlank { null },
                            objasnjenje = objasnjenje.ifBlank { null }
                        )
                    )
                    onDismiss()
                }
            }) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}
