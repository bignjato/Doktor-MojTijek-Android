@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.kalendar

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
import com.mojtijek.doktor.data.DogadjajEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun KalendarScreen(vm: MojTijekViewModel, onNavigateBack: () -> Unit = {}) {
    val dogadjaji by vm.dogadjajiZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedDogadjaj by remember { mutableStateOf<DogadjajEntity?>(null) }

    val nadolazeciDogadjaji = remember(dogadjaji) {
        dogadjaji.filter { it.datum >= System.currentTimeMillis() && it.status != "obavljeno" }
            .sortedBy { it.datum }
    }
    
    val prosliDogadjaji = remember(dogadjaji) {
        dogadjaji.filter { it.datum < System.currentTimeMillis() || it.status == "obavljeno" }
            .sortedByDescending { it.datum }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Kalendar", fontWeight = FontWeight.Bold) },
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
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Novi događaj")
            }
        }
    ) { padding ->
        if (dogadjaji.isEmpty()) {
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
                        Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nema zakazanih događaja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dodajte preglede, termine i važne događaje pomoću + gumba.",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (nadolazeciDogadjaji.isNotEmpty()) {
                    item {
                        Text(
                            "Nadolazeći",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(nadolazeciDogadjaji) { dogadjaj ->
                        DogadjajCard(
                            dogadjaj = dogadjaj,
                            onClick = { selectedDogadjaj = dogadjaj },
                            onDelete = { vm.obrisiDogadjaj(it) },
                            onStatusChange = { updated -> vm.azurirajDogadjaj(updated) }
                        )
                    }
                }
                
                if (prosliDogadjaji.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Prošli",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(prosliDogadjaji) { dogadjaj ->
                        DogadjajCard(
                            dogadjaj = dogadjaj,
                            onClick = { selectedDogadjaj = dogadjaj },
                            onDelete = { vm.obrisiDogadjaj(it) },
                            onStatusChange = { updated -> vm.azurirajDogadjaj(updated) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        NoviDogadjajDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { dogadjaj ->
                vm.dodajDogadjaj(dogadjaj)
                showDialog = false
            }
        )
    }

    selectedDogadjaj?.let { dogadjaj ->
        DogadjajDetaljiDialog(
            dogadjaj = dogadjaj,
            onDismiss = { selectedDogadjaj = null },
            onUpdate = { updated ->
                vm.azurirajDogadjaj(updated)
                selectedDogadjaj = null
            }
        )
    }
}

@Composable
private fun DogadjajCard(
    dogadjaj: DogadjajEntity,
    onClick: () -> Unit,
    onDelete: (DogadjajEntity) -> Unit,
    onStatusChange: (DogadjajEntity) -> Unit
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (dogadjaj.vrsta) {
                        "pregled" -> Icons.Filled.MedicalServices
                        "kontrola" -> Icons.Filled.FactCheck
                        "cjepivo" -> Icons.Filled.Vaccines
                        else -> Icons.Filled.Event
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    dogadjaj.naslov,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        SimpleDateFormat("d. MMM yyyy.", Locale("hr", "HR")).format(Date(dogadjaj.datum)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    dogadjaj.vrijeme?.let {
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (dogadjaj.uputnicaIzdana) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Uputnica",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (dogadjaj.status != "obavljeno") {
                    IconButton(onClick = {
                        onStatusChange(dogadjaj.copy(status = "obavljeno"))
                    }) {
                        Icon(
                            Icons.Filled.CheckCircleOutline,
                            contentDescription = "Označi obavljenim",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši događaj?") },
            text = { Text("Jeste li sigurni da želite obrisati ovaj događaj?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(dogadjaj)
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
private fun NoviDogadjajDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (DogadjajEntity) -> Unit
) {
    var naslov by remember { mutableStateOf("") }
    var vrsta by remember { mutableStateOf("pregled") }
    var datum by remember { mutableStateOf(System.currentTimeMillis()) }
    var vrijeme by remember { mutableStateOf("") }
    var lokacija by remember { mutableStateOf("") }
    var napomena by remember { mutableStateOf("") }
    var uputnicaPotrebna by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val vrste = listOf("pregled", "kontrola", "cjepivo", "termin", "ostalo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi događaj") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = naslov,
                        onValueChange = { naslov = it },
                        label = { Text("Naziv događaja") },
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
                        value = vrijeme,
                        onValueChange = { vrijeme = it },
                        label = { Text("Vrijeme (npr. 10:30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = lokacija,
                        onValueChange = { lokacija = it },
                        label = { Text("Lokacija") },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Potrebna uputnica", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uputnicaPotrebna,
                            onCheckedChange = { uputnicaPotrebna = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naslov.isNotBlank()) {
                    onSpremi(
                        DogadjajEntity(
                            clanId = clanId,
                            naslov = naslov,
                            vrsta = vrsta,
                            datum = datum,
                            vrijeme = vrijeme.ifBlank { null },
                            lokacija = lokacija.ifBlank { null },
                            napomena = napomena.ifBlank { null },
                            uputnicaPotrebna = uputnicaPotrebna,
                            status = "planirano"
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
private fun DogadjajDetaljiDialog(
    dogadjaj: DogadjajEntity,
    onDismiss: () -> Unit,
    onUpdate: (DogadjajEntity) -> Unit
) {
    var naslov by remember { mutableStateOf(dogadjaj.naslov) }
    var vrsta by remember { mutableStateOf(dogadjaj.vrsta) }
    var vrijeme by remember { mutableStateOf(dogadjaj.vrijeme ?: "") }
    var lokacija by remember { mutableStateOf(dogadjaj.lokacija ?: "") }
    var napomena by remember { mutableStateOf(dogadjaj.napomena ?: "") }
    var priprema by remember { mutableStateOf(dogadjaj.priprema ?: "") }
    var uputnicaPotrebna by remember { mutableStateOf(dogadjaj.uputnicaPotrebna) }
    var uputnicaIzdana by remember { mutableStateOf(dogadjaj.uputnicaIzdana) }
    var expanded by remember { mutableStateOf(false) }

    val vrste = listOf("pregled", "kontrola", "cjepivo", "termin", "ostalo")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalji događaja") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = naslov,
                        onValueChange = { naslov = it },
                        label = { Text("Naziv događaja") },
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
                        value = vrijeme,
                        onValueChange = { vrijeme = it },
                        label = { Text("Vrijeme (npr. 10:30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = lokacija,
                        onValueChange = { lokacija = it },
                        label = { Text("Lokacija") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = priprema,
                        onValueChange = { priprema = it },
                        label = { Text("Priprema") },
                        maxLines = 3,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Potrebna uputnica", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uputnicaPotrebna,
                            onCheckedChange = { uputnicaPotrebna = it }
                        )
                    }
                }

                if (uputnicaPotrebna) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Uputnica izdana", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = uputnicaIzdana,
                                onCheckedChange = { uputnicaIzdana = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (naslov.isNotBlank()) {
                    onUpdate(
                        dogadjaj.copy(
                            naslov = naslov,
                            vrsta = vrsta,
                            vrijeme = vrijeme.ifBlank { null },
                            lokacija = lokacija.ifBlank { null },
                            priprema = priprema.ifBlank { null },
                            napomena = napomena.ifBlank { null },
                            uputnicaPotrebna = uputnicaPotrebna,
                            uputnicaIzdana = uputnicaIzdana
                        )
                    )
                }
            }) {
                Text("Spremi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zatvori")
            }
        }
    )
}
