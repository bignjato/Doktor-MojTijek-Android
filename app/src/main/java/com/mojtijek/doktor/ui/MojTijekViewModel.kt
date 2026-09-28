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
            boja = "#E91E63",
            redoslijed = 0,
            aktivan = true
        )
        repo.upsertClan(ana)
        _aktivniClanId.value = ana.id
        
        // Demo therapies
        val terapija1 = TerapijaEntity(
            clanId = ana.id,
            naziv = "Euthyrox",
            jacina = "100mcg",
            oblik = "tableta",
            dozaKom = 1.0,
            putaDnevno = 1,
            vremena = "07:00",
            komPoKutiji = 100.0,
            kolicina = 45.0,
            pragDana = 7,
            trajni = true,
            aktivna = true,
            razlog = "Hipotireoza",
            receptDo = System.currentTimeMillis() + (60L * 24 * 60 * 60 * 1000)
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
            razlog = "Nedostatak vitamina D"
        )
        repo.upsertTerapija(terapija2)
        
        // Demo appointment
        val pregled = DogadjajEntity(
            clanId = ana.id,
            vrsta = "pregled",
            naslov = "Kontrola kod endokrinologa",
            datum = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
            vrijeme = "10:30",
            lokacija = "Poliklinika Medico, Zagreb",
            uputnicaPotrebna = true,
            uputnicaIzdana = true,
            status = "planirano"
        )
        repo.upsertDogadjaj(pregled)
        
        // Demo measurements
        val tlak = MjerenjeEntity(
            clanId = ana.id,
            tip = "tlak",
            vrijednost = 125.0,
            vrijednost2 = 82.0,
            jedinica = "mmHg",
            ts = System.currentTimeMillis() - (2L * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(tlak)
        
        val tezina = MjerenjeEntity(
            clanId = ana.id,
            tip = "težina",
            vrijednost = 68.5,
            jedinica = "kg",
            ts = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
        )
        repo.upsertMjerenje(tezina)
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

    class Factory(private val repo: MojTijekRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MojTijekViewModel(repo) as T
    }
}
