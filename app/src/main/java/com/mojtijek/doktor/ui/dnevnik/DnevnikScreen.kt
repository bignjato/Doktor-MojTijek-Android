@file:OptIn(ExperimentalMaterial3Api::class)

package com.mojtijek.doktor.ui.dnevnik

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.data.DnevnikUnosEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DnevnikScreen(vm: MojTijekViewModel, onNavigateBack: () -> Unit = {}) {
    val unosi by vm.dnevnikZaAktivnog().collectAsState(initial = emptyList())
    val aktivniId by vm.aktivniClanId.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedUnos by remember { mutableStateOf<DnevnikUnosEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dnevnik", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Filled.Add, contentDescription = "Novi unos")
            }
        }
    ) { padding ->
        if (unosi.isEmpty()) {
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
                        Icons.Filled.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nema unosa u dnevniku",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Zabilježite simptome, raspoloženje ili važne događaje pomoću + gumba.",
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
                items(unosi) { unos ->
                    DnevnikCard(
                        unos = unos,
                        onClick = { selectedUnos = unos },
                        onDelete = { vm.obrisiDnevnikUnos(it) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        NoviDnevnikDialog(
            clanId = aktivniId ?: "",
            onDismiss = { showDialog = false },
            onSpremi = { unos ->
                vm.dodajDnevnikUnos(unos)
                showDialog = false
            }
        )
    }

    selectedUnos?.let { unos ->
        DnevnikDetaljiDialog(
            unos = unos,
            onDismiss = { selectedUnos = null },
            onUpdate = { updated ->
                vm.dodajDnevnikUnos(updated)
                selectedUnos = null
            }
        )
    }
}

@Composable
private fun DnevnikCard(
    unos: DnevnikUnosEntity,
    onClick: () -> Unit,
    onDelete: (DnevnikUnosEntity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Emoji for mood/type
                        val emoji = when {
                            unos.raspolozenje != null -> when {
                                unos.raspolozenje >= 4 -> "😊"
                                unos.raspolozenje >= 3 -> "🙂"
                                unos.raspolozenje >= 2 -> "😐"
                                else -> "😔"
                            }
                            unos.vrsta == "simptom" -> "🤒"
                            unos.vrsta == "bol" -> "💢"
                            else -> "📝"
                        }
                        Text(emoji, style = MaterialTheme.typography.headlineSmall)

                        Column {
                            Text(
                                unos.naslov ?: unos.vrsta.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                SimpleDateFormat("d. MMM yyyy. HH:mm", Locale("hr", "HR")).format(Date(unos.datum ?: System.currentTimeMillis())),
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

            unos.tekst?.takeIf { it.isNotBlank() }?.let { tekst ->
                Spacer(Modifier.height(12.dp))
                Text(
                    tekst.take(150) + if (tekst.length > 150) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (unos.jacina != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Jačina:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    repeat(5) { i ->
                        Icon(
                            if (i < (unos.jacina ?: 0)) Icons.Filled.Circle else Icons.Outlined.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši unos?") },
            text = { Text("Jeste li sigurni da želite obrisati ovaj unos?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(unos)
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
private fun NoviDnevnikDialog(
    clanId: String,
    onDismiss: () -> Unit,
    onSpremi: (DnevnikUnosEntity) -> Unit
) {
    var vrsta by remember { mutableStateOf("bilješka") }
    var naslov by remember { mutableStateOf("") }
    var tekst by remember { mutableStateOf("") }
    var raspolozenje by remember { mutableStateOf<Int?>(null) }
    var jacina by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val vrste = listOf("bilješka", "simptom", "bol", "događaj")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi unos u dnevnik") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    value = naslov,
                    onValueChange = { naslov = it },
                    label = { Text("Naslov") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tekst,
                    onValueChange = { tekst = it },
                    label = { Text("Tekst") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Mood selector
                if (vrsta == "bilješka" || vrsta == "događaj") {
                    Column {
                        Text(
                            "Raspoloženje:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("😔" to 1, "😐" to 2, "🙂" to 3, "😊" to 4, "😄" to 5).forEach { (emoji, value) ->
                                FilterChip(
                                    selected = raspolozenje == value,
                                    onClick = { raspolozenje = if (raspolozenje == value) null else value },
                                    label = { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                                )
                            }
                        }
                    }
                }

                // Intensity selector for symptoms/pain
                if (vrsta == "simptom" || vrsta == "bol") {
                    Column {
                        Text(
                            "Jačina (1-5):",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { value ->
                                FilterChip(
                                    selected = jacina == value,
                                    onClick = { jacina = if (jacina == value) null else value },
                                    label = { Text(value.toString()) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSpremi(
                    DnevnikUnosEntity(
                        clanId = clanId,
                        datum = System.currentTimeMillis(),
                        vrsta = vrsta,
                        naslov = naslov.ifBlank { null },
                        tekst = tekst.ifBlank { null },
                        raspolozenje = raspolozenje,
                        jacina = jacina
                    )
                )
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
private fun DnevnikDetaljiDialog(
    unos: DnevnikUnosEntity,
    onDismiss: () -> Unit,
    onUpdate: (DnevnikUnosEntity) -> Unit
) {
    var vrsta by remember { mutableStateOf(unos.vrsta) }
    var naslov by remember { mutableStateOf(unos.naslov ?: "") }
    var tekst by remember { mutableStateOf(unos.tekst ?: "") }
    var raspolozenje by remember { mutableStateOf(unos.raspolozenje) }
    var jacina by remember { mutableStateOf(unos.jacina) }
    var expanded by remember { mutableStateOf(false) }

    val vrste = listOf("bilješka", "simptom", "bol", "događaj")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uredi unos") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        value = naslov,
                        onValueChange = { naslov = it },
                        label = { Text("Naslov") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = tekst,
                        onValueChange = { tekst = it },
                        label = { Text("Tekst") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (vrsta == "bilješka" || vrsta == "događaj") {
                    item {
                        Column {
                            Text(
                                "Raspoloženje:",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("😔" to 1, "😐" to 2, "🙂" to 3, "😊" to 4, "😄" to 5).forEach { (emoji, value) ->
                                    FilterChip(
                                        selected = raspolozenje == value,
                                        onClick = { raspolozenje = if (raspolozenje == value) null else value },
                                        label = { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (vrsta == "simptom" || vrsta == "bol") {
                    item {
                        Column {
                            Text(
                                "Jačina (1-5):",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                (1..5).forEach { value ->
                                    FilterChip(
                                        selected = jacina == value,
                                        onClick = { jacina = if (jacina == value) null else value },
                                        label = { Text(value.toString()) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(
                    unos.copy(
                        vrsta = vrsta,
                        naslov = naslov.ifBlank { null },
                        tekst = tekst.ifBlank { null },
                        raspolozenje = raspolozenje,
                        jacina = jacina
                    )
                )
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
