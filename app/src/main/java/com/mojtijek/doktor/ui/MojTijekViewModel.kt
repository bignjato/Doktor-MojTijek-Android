package com.mojtijek.doktor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mojtijek.doktor.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class MojTijekViewModel(private val repo: MojTijekRepository) : ViewModel() {

    val clanovi: StateFlow<List<ClanEntity>> = repo.clanovi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aktivniClanId = MutableStateFlow<String?>(null)
    val aktivniClanId: StateFlow<String?> = _aktivniClanId.asStateFlow()

    init {
        viewModelScope.launch {
            clanovi.collect { list ->
                if (_aktivniClanId.value == null && list.isNotEmpty()) {
                    _aktivniClanId.value = list.first().id
                }
            }
        }
    }

    fun odaberiClana(id: String) { _aktivniClanId.value = id }

    fun dodajClana(ime: String) = viewModelScope.launch {
        val redoslijed = repo.clanCount()
        val clan = ClanEntity(ime = ime, redoslijed = redoslijed)
        repo.upsertClan(clan)
        _aktivniClanId.value = clan.id
    }
    
    fun dodajSeedData() = viewModelScope.launch {
        // Demo family member
        val ana = ClanEntity(
            ime = "Ana Horvat",
            datumRodjenja = System.currentTimeMillis() - (35L * 365 * 24 * 60 * 60 * 1000),
            spol = "žensko",
            krvnaGrupa = "A+",
            visina = 168.0,
            oib = "12345678901",
            mbo = "123456789",
            lijecnik = "Dr. Marko Kovač",
            lijecnikEmail = "marko.kovac@example.hr",
            alergije = "Polen, penicilin",
            hitniKontakt = "Ivan Horvat (suprug)",
            hitniTelefon = "+385 98 123 4567",
            boja = "#4ECDC4",
            redoslijed = 0,
            aktivan = true
        )
        repo.upsertClan(ana)
        _aktivniClanId.value = ana.id
        
        // Demo therapies with multiple doses per day
        val terapija1 = TerapijaEntity(
            clanId = ana.id,
            naziv = "Euthyrox",
            jacina = "100mcg",
            oblik = "tableta",
            dozaKom = 1.0,
            putaDnevno = 1,
            vremena = "07:00",
            komPoKutiji = 100.0,
            kolicina = 8.0,
            pragDana = 7,
            trajni = true,
            aktivna = true,
            razlog = "Hipotireoza",
            napomena = "Uzeti natašte, 30 minuta prije doručka. Izbjegavati istovremenu primjenu s kalcijem.",
            receptDo = System.currentTimeMillis() + (20L * 24 * 60 * 60 * 1000)
        )
        repo.upsertTerapija(terapija1)
        
        val terapija2 = TerapijaEntity(
            clanId = ana.id,
            naziv = "Vitamin D3",
            jacina = "2000 IU",
            oblik = "tableta",
            dozaKom = 1.0,
            putaDnevno = 1,
            vremena = "08:00",
            komPoKutiji = 60.0,
            kolicina = 18.0,
            pragDana = 7,
            trajni = true,
            aktivna = true,
            razlog = "Nedostatak vitamina D",
            napomena = "Uzimati s obrokom koji sadrži masti za bolju apsorpciju."
        )
        repo.upsertTerapija(terapija2)
        
        val terapija3 = TerapijaEntity(
            clanId = ana.id,
            naziv = "Omega-3",
            jacina = "1000mg",
            oblik = "kapsula",
            dozaKom = 1.0,
            putaDnevno = 2,
            vremena = "12:00,20:00",
            komPoKutiji = 90.0,
            kolicina = 35.0,
            pragDana = 10,
            trajni = true,
            aktivna = true,
            razlog = "Kardiovaskularno zdravlje"
        )
        repo.upsertTerapija(terapija3)
        
        val terapija4 = TerapijaEntity(
            clanId = ana.id,
            naziv = "Probiotik",
            jacina = "10 milijardi CFU",
            oblik = "kapsula",
            dozaKom = 1.0,
            putaDnevno = 1,
            vremena = "22:00",
            komPoKutiji = 30.0,
            kolicina = 22.0,
            pragDana = 5,
            trajni = true,
            aktivna = true,
            razlog = "Zdravlje crijeva"
        )
        repo.upsertTerapija(terapija4)
        
        // Demo appointment
        val pregled = DogadjajEntity(
            clanId = ana.id,
            vrsta = "pregled",
            naslov = "Kontrola kod endokrinologa",
            datum = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000),
            vrijeme = "10:30",
            lokacija = "Poliklinika Medico, Zagreb",
            uputnicaPotrebna = true,
            uputnicaIzdana = true,
            status = "planirano",
            napomena = "Dr. Kovač - endokrinološki pregled, uzeti posljednje nalaze"
        )
        repo.upsertDogadjaj(pregled)
        
        // Rich demo measurements to match iOS
        val koraci = MjerenjeEntity(
            clanId = ana.id,
            tip = "koraci",
            vrijednost = 7542.0,
            jedinica = "koraka",
            ts = System.currentTimeMillis() - (1L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(koraci)
        
        val tlak = MjerenjeEntity(
            clanId = ana.id,
            tip = "krvni tlak",
            vrijednost = 125.0,
            vrijednost2 = 71.0,
            jedinica = "mmHg",
            ts = System.currentTimeMillis() - (3L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(tlak)
        
        val secer = MjerenjeEntity(
            clanId = ana.id,
            tip = "šećer",
            vrijednost = 5.2,
            jedinica = "mmol/L",
            ts = System.currentTimeMillis() - (4L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(secer)
        
        val puls = MjerenjeEntity(
            clanId = ana.id,
            tip = "puls",
            vrijednost = 72.0,
            jedinica = "otkucaja/min",
            ts = System.currentTimeMillis() - (2L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(puls)
        
        val tezina = MjerenjeEntity(
            clanId = ana.id,
            tip = "težina",
            vrijednost = 68.5,
            jedinica = "kg",
            ts = System.currentTimeMillis() - (12L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(tezina)
        
        val temp = MjerenjeEntity(
            clanId = ana.id,
            tip = "temperatura",
            vrijednost = 36.6,
            jedinica = "°C",
            ts = System.currentTimeMillis() - (8L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(temp)
        
        // Demo documents
        val nalaz1 = DokumentEntity(
            clanId = ana.id,
            naziv = "Kompletna krvna slika",
            vrsta = "nalaz",
            datum = System.currentTimeMillis() - (15L * 24 * 60 * 60 * 1000),
            ustanova = "Poliklinika Medico",
            lijecnik = "Dr. Marić",
            napomena = "Nalazi uredni"
        )
        repo.upsertDokument(nalaz1)
        
        val nalaz2 = DokumentEntity(
            clanId = ana.id,
            naziv = "EKG pregled",
            vrsta = "nalaz",
            datum = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000),
            ustanova = "KBC Zagreb",
            lijecnik = "Dr. Kovač",
            napomena = "Srce uredno"
        )
        repo.upsertDokument(nalaz2)
        
        val recept = DokumentEntity(
            clanId = ana.id,
            naziv = "Recept za Amlopin",
            vrsta = "recept",
            datum = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000),
            ustanova = "Dom zdravlja Zagreb",
            lijecnik = "Dr. Novak"
        )
        repo.upsertDokument(recept)
    }

    fun azurirajClana(clan: ClanEntity) = viewModelScope.launch { repo.upsertClan(clan) }

    fun terapijeZaAktivnog(): Flow<List<TerapijaEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.terapijeAktivne(id) }

    fun sveTerapijeZaAktivnog(): Flow<List<TerapijaEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.terapijeSve(id) }

    fun dodajTerapiju(t: TerapijaEntity) = viewModelScope.launch { repo.upsertTerapija(t) }
    fun obrisiTerapiju(t: TerapijaEntity) = viewModelScope.launch { repo.deleteTerapija(t) }

    fun uzimanjaDanas(): Flow<List<UzimanjeEntity>> {
        val danas = DoseSchedule.dayStart()
        return aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.uzimanjaZaDan(id, danas) }
    }
    
    fun svaUzimanja(): Flow<List<UzimanjeEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.svaUzimanjaZaClana(id) }

    fun potvrdiDozu(terapija: TerapijaEntity, slot: String) = viewModelScope.launch {
        val clanId = _aktivniClanId.value ?: return@launch
        val danas = DoseSchedule.dayStart()
        repo.potvrdiUzimanje(
            UzimanjeEntity(
                terapijaId = terapija.id, clanId = clanId,
                datum = danas, slot = slot, kolicina = terapija.dozaKom
            )
        )
    }

    fun preskociDozu(terapija: TerapijaEntity, slot: String) = viewModelScope.launch {
        val clanId = _aktivniClanId.value ?: return@launch
        val danas = DoseSchedule.dayStart()
        repo.potvrdiUzimanje(
            UzimanjeEntity(
                terapijaId = terapija.id, clanId = clanId,
                datum = danas, slot = slot, kolicina = 0.0, preskoceno = true
            )
        )
    }

    fun dogadjajiZaAktivnog(): Flow<List<DogadjajEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.dogadjajiZaClana(id) }

    fun dodajDogadjaj(d: DogadjajEntity) = viewModelScope.launch { repo.upsertDogadjaj(d) }
    fun obrisiDogadjaj(d: DogadjajEntity) = viewModelScope.launch { repo.deleteDogadjaj(d) }
    fun azurirajDogadjaj(d: DogadjajEntity) = viewModelScope.launch { repo.upsertDogadjaj(d) }

    fun dokumentiZaAktivnog(): Flow<List<DokumentEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.dokumentiZaClana(id) }
    fun dodajDokument(d: DokumentEntity) = viewModelScope.launch { repo.upsertDokument(d) }
    fun obrisiDokument(d: DokumentEntity) = viewModelScope.launch { repo.deleteDokument(d) }

    fun labZaAktivnog(): Flow<List<LabNalazEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.labZaClana(id) }

    fun mjerenjaZaAktivnog(): Flow<List<MjerenjeEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.mjerenjaZaClana(id) }
    fun dodajMjerenje(m: MjerenjeEntity) = viewModelScope.launch { repo.upsertMjerenje(m) }
    fun obrisiMjerenje(m: MjerenjeEntity) = viewModelScope.launch { repo.deleteMjerenje(m) }

    fun cijepljenjaZaAktivnog(): Flow<List<CijepljenjeEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.cijepljenjaZaClana(id) }
    fun dodajCijepljenje(c: CijepljenjeEntity) = viewModelScope.launch { repo.upsertCijepljenje(c) }

    fun dnevnikZaAktivnog(): Flow<List<DnevnikUnosEntity>> =
        aktivniClanId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.dnevnikZaClana(id) }
    fun dodajDnevnikUnos(d: DnevnikUnosEntity) = viewModelScope.launch { repo.upsertDnevnik(d) }
    fun obrisiDnevnikUnos(d: DnevnikUnosEntity) = viewModelScope.launch { repo.deleteDnevnik(d) }

    fun exportDatabaseToJson() = viewModelScope.launch {
        try {
            val clanovi = repo.clanovi.first()
            val export = mutableMapOf<String, Any>()
            
            export["timestamp"] = System.currentTimeMillis()
            export["version"] = "1.0"
            export["clanoviCount"] = clanovi.size
            
            clanovi.forEach { clan ->
                val terapije = repo.terapijeSve(clan.id).first()
                val uzimanja = repo.svaUzimanjaZaClana(clan.id).first()
                val dogadjaji = repo.dogadjajiZaClana(clan.id).first()
                val dokumenti = repo.dokumentiZaClana(clan.id).first()
                val mjerenja = repo.mjerenjaZaClana(clan.id).first()
                val dnevnik = repo.dnevnikZaClana(clan.id).first()
                
                export["${clan.ime}_terapije"] = terapije.size
                export["${clan.ime}_uzimanja"] = uzimanja.size
                export["${clan.ime}_dogadjaji"] = dogadjaji.size
                export["${clan.ime}_dokumenti"] = dokumenti.size
                export["${clan.ime}_mjerenja"] = mjerenja.size
                export["${clan.ime}_dnevnik"] = dnevnik.size
            }
            
            val json = export.entries.joinToString(",\n  ", "{\n  ", "\n}") { (k, v) ->
                "\"$k\": ${if (v is String) "\"$v\"" else v}"
            }
            
            android.util.Log.d("MojTijek", "Database export summary:\n$json")
            // In a real implementation, this would serialize full entities and write to a file
        } catch (e: Exception) {
            android.util.Log.e("MojTijek", "Export failed", e)
        }
    }

    class Factory(private val repo: MojTijekRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MojTijekViewModel(repo) as T
    }
}
