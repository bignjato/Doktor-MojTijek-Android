package com.mojtijek.doktor.data

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
    suspend fun potvrdiUzimanje(u: UzimanjeEntity) = db.uzimanjeDao().upsert(u)

    fun dogadjajiZaClana(clanId: String): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeByClan(clanId)
    fun nadolazeciDogadjaji(fromMillis: Long): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeUpcoming(fromMillis)
    suspend fun upsertDogadjaj(d: DogadjajEntity) = db.dogadjajDao().upsert(d)
    suspend fun deleteDogadjaj(d: DogadjajEntity) = db.dogadjajDao().delete(d)

    fun dokumentiZaClana(clanId: String): Flow<List<DokumentEntity>> = db.dokumentDao().observeByClan(clanId)
    suspend fun upsertDokument(d: DokumentEntity) = db.dokumentDao().upsert(d)

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
