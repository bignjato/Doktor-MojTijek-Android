@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mojtijek.doktor.ui.nalazi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mojtijek.doktor.R
import com.mojtijek.doktor.data.DokumentEntity
import com.mojtijek.doktor.data.LabNalazEntity
import com.mojtijek.doktor.ui.MojTijekViewModel
import com.mojtijek.doktor.ui.components.MtCard
import com.mojtijek.doktor.ui.components.ObiteljSheet
import com.mojtijek.doktor.ui.theme.Mt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

// Ekran „Nalazi” / Kartoteka — isti redoslijed kao iOS MedicalRecordsView:
// filtar statusa → dodavanje (PDF, galerija) → Lab trendovi → popis dokumenata → otvaranje dokumenta.
// Još nema: Skeniraj, Portal zdravlja, Pošalji u bolnicu / e-poštom i AI čitanje (dolaze sa svojim ekranima).

private fun dan(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
private fun broj(v: Double): String = NumberFormat.getInstance().apply { maximumFractionDigits = 2 }.format(v)

private fun statusTekst(faza: String?): Int = when (faza) {
    "potvrda" -> R.string.treba_potvrdu
    "obrada" -> R.string.obradjuje_se
    "greska" -> R.string.obrada_nije_uspjela
    "potvrdjeno" -> R.string.potvrdjeno
    "gotovo" -> R.string.ranije_obradjeno
    else -> R.string.ceka_obradu
}

private val VRSTE = mapOf("nalaz" to R.string.vrsta_nalaz, "otpusno" to R.string.vrsta_otpusno, "uputnica" to R.string.vrsta_uputnica,
                          "recept" to R.string.recept, "ostalo" to R.string.ostalo)

private fun vrstaIkona(vrsta: String): ImageVector = when (vrsta) {
    "nalaz" -> Icons.Filled.MedicalServices
    "otpusno" -> Icons.Filled.Apartment
    "uputnica" -> Icons.Filled.Email
    "recept" -> Icons.Filled.Medication
    else -> Icons.Filled.Description
}

// ---- čitanje datoteka ----

private class Ucitano(val bajtovi: ByteArray, val mime: String, val ime: String)

private fun ucitaj(context: Context, uri: Uri, ime: String? = null): Ucitano? {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: return null
    if (mime != "application/pdf" && !mime.startsWith("image/")) return null
    val bajtovi = cr.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val izvorno = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    return Ucitano(bajtovi, mime, ime ?: izvorno ?: "Dokument")
}

private const val NAJVECA_STRANA = 1600

/** Slika smanjena na najviše ~1600 px po duljoj strani, s poštovanom orijentacijom (API 28+). */
private fun dekodiraj(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val najveca = maxOf(info.size.width, info.size.height)
            if (najveca > NAJVECA_STRANA) d.setTargetSampleSize((najveca + NAJVECA_STRANA - 1) / NAJVECA_STRANA)
        }
    } else {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
} catch (e: Exception) { null }

/** Više slika → jedan PDF, svaka slika jedna stranica (kao iOS). */
private fun slikeUPdf(context: Context, uris: List<Uri>): ByteArray? {
    val pdf = PdfDocument()
    try {
        uris.forEachIndexed { i, uri ->
            val slika = dekodiraj(context, uri) ?: return null
            val stranica = pdf.startPage(PdfDocument.PageInfo.Builder(slika.width, slika.height, i + 1).create())
            stranica.canvas.drawBitmap(slika, 0f, 0f, null)
            pdf.finishPage(stranica)
            slika.recycle()
        }
        return ByteArrayOutputStream().also { pdf.writeTo(it) }.toByteArray()
    } finally { pdf.close() }
}

// ---- popis ----

@Composable
fun NalaziScreen(vm: MojTijekViewModel, onNavigateBack: (() -> Unit)? = null) {
    val clanovi by vm.clanovi.collectAsState()
    val aktivniId by vm.aktivniClanId.collectAsState()
    val clan = clanovi.firstOrNull { it.id == aktivniId } ?: clanovi.firstOrNull()
    val dokumenti by remember(vm) { vm.dokumentiZaAktivnog() }.collectAsState(initial = emptyList())
    val lab by remember(vm) { vm.labZaAktivnog() }.collectAsState(initial = emptyList())

    var otvoreniId by rememberSaveable { mutableStateOf<String?>(null) }
    var trendovi by rememberSaveable { mutableStateOf(false) }
    var filtar by rememberSaveable { mutableStateOf("sve") }
    var trazi by rememberSaveable { mutableStateOf("") }
    var obitelj by remember { mutableStateOf(false) }
    var radi by remember { mutableStateOf(false) }
    var greska by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val otvoreni = dokumenti.firstOrNull { it.id == otvoreniId }
    if (otvoreni != null) {
        DokumentDetalj(otvoreni, vm.dokumentDatoteka(otvoreni.id)) { otvoreniId = null }
        return
    }
    if (trendovi) {
        LabTrendovi(lab) { trendovi = false }
        return
    }

    fun spremi(clanId: String, neuspjeh: Int, priprema: () -> Ucitano?) {
        scope.launch {
            radi = true
            try {
                val u = withContext(Dispatchers.IO) { priprema() }
                if (u == null) greska = context.getString(neuspjeh)
                else if (!vm.spremiDokument(clanId, u.bajtovi, u.mime, u.ime)) greska = context.getString(R.string.dokument_vec_postoji)
            } catch (e: Exception) {
                greska = context.getString(neuspjeh)
            } finally { radi = false }
        }
    }
    val datoteka = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val c = clan
        if (uri != null && c != null) spremi(c.id, R.string.nepodrzan_format) { ucitaj(context, uri) }
    }
    val galerija = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        val c = clan
        if (uris.isNotEmpty() && c != null) spremi(c.id, R.string.slika_neuspjeh) {
            if (uris.size == 1) ucitaj(context, uris[0], "Slika iz galerije")
            else slikeUPdf(context, uris)?.let { Ucitano(it, "application/pdf", "Slike iz galerije.pdf") }
        }
    }

    val oznake = VRSTE.mapValues { stringResource(it.value) }
    val prikazani = remember(dokumenti, filtar, trazi, oznake) {
        val q = trazi.trim().lowercase()
        dokumenti.filter { filtar == "sve" || it.obradaFaza == filtar }.sortedByDescending { it.stvoren }.filter { d ->
            q.isEmpty() || listOfNotNull(d.naziv, d.origIme, d.lijecnik, d.ustanova, oznake[d.vrsta] ?: d.vrsta).any { it.lowercase().contains(q) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
                }
                Text(stringResource(R.string.tab_nalazi), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                     modifier = Modifier.weight(1f))
                if (clan != null) {
                    TextButton(onClick = { obitelj = true }) {
                        Text(clan.ime, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (clan == null) {
            item {
                Poruka(Icons.Filled.Description, stringResource(R.string.nema_clanova), stringResource(R.string.dodaj_clana_opis))
            }
            return@LazyColumn
        }
        item {
            OutlinedTextField(trazi, { trazi = it }, placeholder = { Text(stringResource(R.string.pretrazi_nalaze)) }, singleLine = true,
                              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }, shape = RoundedCornerShape(14.dp),
                              modifier = Modifier.fillMaxWidth())
        }
        item {
            val filtri = listOf("sve" to R.string.sve, "potvrda" to R.string.za_potvrdu, "potvrdjeno" to R.string.potvrdjeno)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                filtri.forEachIndexed { i, (kljuc, naziv) ->
                    SegmentedButton(selected = filtar == kljuc, onClick = { filtar = kljuc },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = filtri.size)) {
                        Text(stringResource(naziv), maxLines = 1)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Radnja(stringResource(R.string.ucitaj_pdf), Icons.Filled.NoteAdd, Mt.Blue, Modifier.weight(1f)) {
                    datoteka.launch(arrayOf("application/pdf", "image/*"))
                }
                Radnja(stringResource(R.string.iz_galerije), Icons.Filled.PhotoLibrary, Mt.Purple, Modifier.weight(1f)) {
                    galerija.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            if (radi) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        if (lab.isNotEmpty()) {
            item {
                Surface(onClick = { trendovi = true }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Mt.Purple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = Mt.Purple, modifier = Modifier.size(22.dp))
                        }
                        Text(stringResource(R.string.lab_trendovi), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                             modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dokumenti), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                     modifier = Modifier.weight(1f))
                Text(prikazani.size.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (prikazani.isEmpty()) {
            item { MtCard { Text(stringResource(R.string.nema_dokumenata), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
        items(prikazani, key = { it.id }) { d ->
            KarticaDokumenta(d, oznake[d.vrsta] ?: d.vrsta, onOtvori = { otvoreniId = d.id }, onObrisi = { vm.obrisiDokument(d) })
        }
    }

    if (obitelj && clan != null) ObiteljSheet(clanovi, clan.id, onOdabir = { vm.odaberiClana(it) }, onZatvori = { obitelj = false })
    greska?.let {
        AlertDialog(onDismissRequest = { greska = null }, title = { Text(stringResource(R.string.greska)) }, text = { Text(it) },
                    confirmButton = { TextButton(onClick = { greska = null }) { Text(stringResource(R.string.u_redu)) } })
    }
}

@Composable
private fun Radnja(naslov: String, ikona: ImageVector, boja: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), color = boja.copy(alpha = 0.11f)) {
        Column(Modifier.padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(ikona, contentDescription = null, tint = boja, modifier = Modifier.size(26.dp))
            Text(naslov, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = boja)
        }
    }
}

@Composable
private fun KarticaDokumenta(d: DokumentEntity, vrsta: String, onOtvori: () -> Unit, onObrisi: () -> Unit) {
    var izbornik by remember { mutableStateOf(false) }
    MtCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClick = onOtvori),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(vrstaIkona(d.vrsta), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(d.naziv ?: d.origIme ?: stringResource(R.string.dokument), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                         maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(vrsta, d.datum?.let { dan(it) }, d.lijecnik?.takeIf { it.isNotBlank() }).joinToString(" · "),
                         style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(statusTekst(d.obradaFaza)), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                     color = if (d.obradaFaza == "potvrda") Mt.Orange else MaterialTheme.colorScheme.primary)
                if (d.obrada == "ai") Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Mt.Purple, modifier = Modifier.size(14.dp))
            }
            Box {
                IconButton(onClick = { izbornik = true }) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = izbornik, onDismissRequest = { izbornik = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.obrisi_dokument), color = Mt.Red) }, onClick = { izbornik = false; onObrisi() })
                }
            }
        }
    }
}

@Composable
private fun Poruka(ikona: ImageVector, naslov: String, opis: String? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(ikona, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
        Text(naslov, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (opis != null) Text(opis, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Zaglavlje(naslov: String, onNatrag: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNatrag) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.natrag)) }
        Text(naslov, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---- otvoreni dokument ----

@Composable
private fun DokumentDetalj(d: DokumentEntity, datoteka: File, onNatrag: () -> Unit) {
    BackHandler(onBack = onNatrag)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Zaglavlje(d.naziv ?: d.origIme ?: stringResource(R.string.dokument), onNatrag)
        Text(stringResource(statusTekst(d.obradaFaza)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
             color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        when {
            !datoteka.exists() -> Poruka(Icons.Filled.CloudOff, stringResource(R.string.dokument_nedostupan))
            (d.mime ?: "").contains("pdf") -> PdfPrikaz(datoteka)
            else -> SlikaPrikaz(datoteka)
        }
    }
}

/** Povećavanje s dva prsta; pomicanje jednim prstom tek kad je povećano, da popis stranica inače normalno klizi. */
@Composable
private fun Zum(sadrzaj: @Composable (Modifier) -> Unit) {
    var mjerilo by remember { mutableStateOf(1f) }
    var pomak by remember { mutableStateOf(Offset.Zero) }
    var okvir by remember { mutableStateOf(IntSize.Zero) }
    val stanje = rememberTransformableState { zoom, pan, _ ->
        mjerilo = (mjerilo * zoom).coerceIn(1f, 5f)
        val mx = okvir.width * (mjerilo - 1f) / 2f
        val my = okvir.height * (mjerilo - 1f) / 2f
        pomak = Offset((pomak.x + pan.x).coerceIn(-mx, mx), (pomak.y + pan.y).coerceIn(-my, my))
    }
    Box(Modifier.fillMaxSize().clipToBounds().onSizeChanged { okvir = it }.transformable(stanje, canPan = { mjerilo > 1f })) {
        sadrzaj(Modifier.graphicsLayer { scaleX = mjerilo; scaleY = mjerilo; translationX = pomak.x; translationY = pomak.y })
    }
}

/** PdfRenderer smije imati otvorenu samo jednu stranicu i ne smije se zatvoriti usred crtanja. */
private class Pdf(datoteka: File) {
    private val r = PdfRenderer(ParcelFileDescriptor.open(datoteka, ParcelFileDescriptor.MODE_READ_ONLY))
    private var zatvoren = false
    val brojStranica: Int = r.pageCount

    @Synchronized fun stranica(i: Int, sirina: Int): Bitmap? {
        if (zatvoren) return null
        val p = r.openPage(i)
        try {
            val b = Bitmap.createBitmap(sirina, maxOf(1, sirina * p.height / p.width), Bitmap.Config.ARGB_8888)
            b.eraseColor(android.graphics.Color.WHITE)
            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return b
        } finally { p.close() }
    }

    @Synchronized fun zatvori() { if (!zatvoren) { zatvoren = true; r.close() } }
}

@Composable
private fun PdfPrikaz(datoteka: File) {
    val sirina = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }.let { minOf(it * 3 / 2, NAJVECA_STRANA) }
    val pdf = remember(datoteka) { try { Pdf(datoteka) } catch (e: Exception) { null } }
    DisposableEffect(pdf) { onDispose { pdf?.zatvori() } }
    if (pdf == null) {
        Poruka(Icons.Filled.Description, stringResource(R.string.nepodrzan_format))
        return
    }
    Zum { m ->
        LazyColumn(m.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pdf.brojStranica) { i ->
                val slika by produceState<Bitmap?>(null, i) {
                    value = withContext(Dispatchers.IO) { try { pdf.stranica(i, sirina) } catch (e: Exception) { null } }
                }
                val s = slika
                if (s != null) Image(s.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
                else Box(Modifier.fillMaxWidth().height(420.dp).background(Color.White))
            }
        }
    }
}

@Composable
private fun SlikaPrikaz(datoteka: File) {
    val context = LocalContext.current
    // first = učitavanje završeno
    val stanje by produceState<Pair<Boolean, Bitmap?>>(false to null, datoteka) {
        value = true to withContext(Dispatchers.IO) { dekodiraj(context, Uri.fromFile(datoteka)) }
    }
    val slika = stanje.second
    when {
        slika != null -> Zum { m -> Image(slika.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = m.fillMaxSize()) }
        stanje.first -> Poruka(Icons.Filled.Description, stringResource(R.string.nepodrzan_format))
    }
}

// ---- lab trendovi (iOS LabTrendsView) ----

@Composable
private fun LabTrendovi(lab: List<LabNalazEntity>, onNatrag: () -> Unit) {
    BackHandler(onBack = onNatrag)
    var trazi by rememberSaveable { mutableStateOf("") }
    val bezJedinice = stringResource(R.string.bez_jedinice)
    // Jedinica je dio identiteta niza: iste pretrage u različitim jedinicama se ne miješaju.
    val grupe = remember(lab, trazi, bezJedinice) {
        val q = trazi.trim().lowercase()
        lab.filter { it.vrijednost != null }
            .groupBy { it.naziv.trim() + " · " + (it.jedinica?.trim()?.takeIf { j -> j.isNotEmpty() } ?: bezJedinice) }
            .mapValues { (_, v) -> v.sortedBy { it.datum ?: 0L } }
            .filter { (naziv, v) -> q.isEmpty() || naziv.lowercase().contains(q) || v.first().kratica?.lowercase()?.contains(q) == true }
            .toList().sortedBy { it.first.lowercase() }
    }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
               contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Zaglavlje(stringResource(R.string.lab_trendovi), onNatrag) }
        item {
            OutlinedTextField(trazi, { trazi = it }, placeholder = { Text(stringResource(R.string.pretrazi_pretrage)) }, singleLine = true,
                              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }, shape = RoundedCornerShape(14.dp),
                              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        if (grupe.isEmpty()) item { Poruka(Icons.Filled.Science, stringResource(R.string.nema_lab)) }
        items(grupe, key = { it.first }) { (naziv, stavke) -> Box(Modifier.padding(horizontal = 16.dp)) { LabKartica(naziv, stavke) } }
    }
}

@Composable
private fun LabKartica(naziv: String, stavke: List<LabNalazEntity>) {
    val zadnji = stavke.last()
    val jedinica = zadnji.jedinica ?: ""
    val refLo = zadnji.refLo?.takeIf { it >= 0 }
    val refHi = zadnji.refHi?.takeIf { it >= 0 }
    val boja = when (zadnji.status?.lowercase()) {
        "visoko", "nisko" -> Mt.Orange
        "uredno" -> Mt.Green
        else -> Mt.ink
    }
    val tocke = stavke.mapNotNull { s -> val v = s.vrijednost; val d = s.datum; if (v != null && d != null) d to v else null }
    MtCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(naziv, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    zadnji.kratica?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                zadnji.vrijednost?.let { v ->
                    val strelica = when (zadnji.status?.lowercase()) { "visoko" -> " ↑"; "nisko" -> " ↓"; "uredno" -> " ✓"; else -> "" }
                    Text("${broj(v)} $jedinica$strelica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = boja)
                }
            }
            if (tocke.size >= 2) {
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 4.dp, vertical = 6.dp)) {
                    val t0 = tocke.first().first
                    val t1 = tocke.last().first
                    var lo = tocke.minOf { it.second }
                    var hi = tocke.maxOf { it.second }
                    if (refLo != null && refHi != null) { lo = minOf(lo, refLo); hi = maxOf(hi, refHi) }
                    val raspon = (hi - lo).takeIf { it > 0 } ?: 1.0
                    fun x(t: Long) = if (t1 == t0) size.width / 2 else (t - t0).toFloat() / (t1 - t0) * size.width
                    fun y(v: Double) = size.height - ((v - lo) / raspon).toFloat() * size.height
                    if (refLo != null && refHi != null) {
                        drawRect(Mt.Green.copy(alpha = 0.10f), topLeft = Offset(0f, y(refHi)), size = Size(size.width, y(refLo) - y(refHi)))
                    }
                    tocke.zipWithNext { a, b -> drawLine(Mt.Teal, Offset(x(a.first), y(a.second)), Offset(x(b.first), y(b.second)), strokeWidth = 2.dp.toPx()) }
                    tocke.forEach { drawCircle(Mt.Teal, radius = 3.dp.toPx(), center = Offset(x(it.first), y(it.second))) }
                }
            } else {
                Text(stringResource(R.string.trend_min_dva), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (refLo != null && refHi != null) {
                Text(stringResource(R.string.referentno, broj(refLo), broj(refHi), jedinica), style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
