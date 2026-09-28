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
