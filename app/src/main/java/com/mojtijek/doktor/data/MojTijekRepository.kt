package com.mojtijek.doktor.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class MojTijekRepository(private val db: MojTijekDatabase) {
    val clanovi: Flow<List<ClanEntity>> get() = db.clanDao().observeAll()

    suspend fun upsertClan(clan: ClanEntity) = db.clanDao().upsert(clan)
    suspend fun deleteClan(clan: ClanEntity) = db.clanDao().delete(clan)
    suspend fun clanCount() = db.clanDao().count()

    fun terapijeAktivne(clanId: String): Flow<List<TerapijaEntity>> = db.terapijaDao().observeByClan(clanId)
    fun terapijeSve(clanId: String): Flow<List<TerapijaEntity>> = db.terapijaDao().observeAllByClan(clanId)
    suspend fun upsertTerapija(t: TerapijaEntity) = db.terapijaDao().upsert(t)
    suspend fun deleteTerapija(t: TerapijaEntity) = db.terapijaDao().delete(t)

    fun uzimanjaZaDan(clanId: String, danMillis: Long): Flow<List<UzimanjeEntity>> =
        db.uzimanjeDao().observeByClanAndDay(clanId, danMillis)
    fun svaUzimanjaZaClana(clanId: String): Flow<List<UzimanjeEntity>> =
        db.uzimanjeDao().observeByClan(clanId)

    /**
     * Potvrda ili preskakanje doze (kao iOS DoseRecorder): jedan zapis po terapiji/danu/slotu,
     * ponovljeni dodir ne upisuje duplikat i ne troši zalihu dvaput.
     */
    suspend fun zabiljeziDozu(terapijaId: String, slot: String, dan: Long, preskoceno: Boolean) = db.withTransaction {
        val t = db.terapijaDao().getById(terapijaId) ?: return@withTransaction
        val postojeci = db.uzimanjeDao().getForSlot(terapijaId, dan, slot)
        if (postojeci != null && postojeci.preskoceno == preskoceno) return@withTransaction
        val osnova = postojeci ?: UzimanjeEntity(terapijaId = t.id, clanId = t.clanId, datum = dan, slot = slot)
        db.uzimanjeDao().upsert(osnova.copy(ts = System.currentTimeMillis(), preskoceno = preskoceno,
                                            kolicina = if (preskoceno) 0.0 else t.dozaKom))
        // shortcut: iOS troši zalihu samo uz `zalihaSet`; Android to polje još nema, pa „kolicina > 0”
        // znači da se zaliha vodi. Zamijeniti kad shema dobije zalihaSet (paritet sigurnosne kopije).
        // Uzeta pa naknadno preskočena doza zalihu ne vraća — isto kao iOS.
        if (!preskoceno && t.kolicina > 0) db.terapijaDao().upsert(t.copy(kolicina = maxOf(0.0, t.kolicina - t.dozaKom)))
    }

    fun dogadjajiZaClana(clanId: String): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeByClan(clanId)
    fun nadolazeciDogadjaji(fromMillis: Long): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeUpcoming(fromMillis)
    suspend fun upsertDogadjaj(d: DogadjajEntity) = db.dogadjajDao().upsert(d)
    suspend fun deleteDogadjaj(d: DogadjajEntity) = db.dogadjajDao().delete(d)

    fun dokumentiZaClana(clanId: String): Flow<List<DokumentEntity>> = db.dokumentDao().observeByClan(clanId)
    suspend fun upsertDokument(d: DokumentEntity) = db.dokumentDao().upsert(d)
    suspend fun deleteDokument(d: DokumentEntity) = db.dokumentDao().delete(d)

    fun labZaClana(clanId: String): Flow<List<LabNalazEntity>> = db.labNalazDao().observeByClan(clanId)
    suspend fun upsertLab(l: LabNalazEntity) = db.labNalazDao().upsert(l)

    fun mjerenjaZaClana(clanId: String): Flow<List<MjerenjeEntity>> = db.mjerenjeDao().observeByClan(clanId)
    suspend fun upsertMjerenje(m: MjerenjeEntity) = db.mjerenjeDao().upsert(m)
    suspend fun deleteMjerenje(m: MjerenjeEntity) = db.mjerenjeDao().delete(m)

    fun cijepljenjaZaClana(clanId: String): Flow<List<CijepljenjeEntity>> = db.cijepljenjeDao().observeByClan(clanId)
    suspend fun upsertCijepljenje(c: CijepljenjeEntity) = db.cijepljenjeDao().upsert(c)

    fun dnevnikZaClana(clanId: String): Flow<List<DnevnikUnosEntity>> = db.dnevnikDao().observeByClan(clanId)
    suspend fun upsertDnevnik(d: DnevnikUnosEntity) = db.dnevnikDao().upsert(d)
    suspend fun deleteDnevnik(d: DnevnikUnosEntity) = db.dnevnikDao().delete(d)

    fun ciklusiZaClana(clanId: String): Flow<List<MenstruacijaEntity>> = db.menstruacijaDao().observeByClan(clanId)
    suspend fun upsertCiklus(m: MenstruacijaEntity) = db.menstruacijaDao().upsert(m)
}
