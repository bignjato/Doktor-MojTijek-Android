package com.mojtijek.doktor.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MojTijekRepository(private val db: MojTijekDatabase, private val datoteke: Datoteke) {
    suspend fun izveziKopiju(ukljuciDokumente: Boolean): String = Kopija.izvezi(db, datoteke, ukljuciDokumente)
    suspend fun uveziKopiju(tekst: String) = Kopija.uvezi(db, datoteke, tekst)

    val clanovi: Flow<List<ClanEntity>> get() = db.clanDao().observeAll()

    suspend fun upsertClan(clan: ClanEntity) = db.clanDao().upsert(clan)
    suspend fun deleteClan(clan: ClanEntity) = db.clanDao().delete(clan)
    suspend fun clanCount() = db.clanDao().count()

    fun terapijeAktivne(clanId: String): Flow<List<TerapijaEntity>> = db.terapijaDao().observeByClan(clanId)
    fun terapijeSve(clanId: String): Flow<List<TerapijaEntity>> = db.terapijaDao().observeAllByClan(clanId)
    /** Korisnikov unos/izmjena terapije: zaliha se vodi ako je upisana (kao iOS). */
    suspend fun upsertTerapija(t: TerapijaEntity) {
        val s = t.copy(zalihaSet = t.kolicina > 0)
        // Dovoljno zalihe → oznaka „naručeno” više ne vrijedi.
        db.terapijaDao().upsert(if (DoseSchedule.priKraju(s)) s else s.copy(narucenoTs = null))
    }

    /** Zaprimljena kutija: +1 kutija, zaliha se vodi, oznaka „naručeno” se briše. */
    suspend fun napuniZalihu(terapijaId: String) {
        val t = db.terapijaDao().getById(terapijaId) ?: return
        db.terapijaDao().upsert(t.copy(kolicina = t.kolicina + (if (t.komPoKutiji > 0) t.komPoKutiji else 30.0), zalihaSet = true,
                                       datumPunjenja = System.currentTimeMillis(), narucenoTs = null))
    }

    suspend fun oznaciNaruceno(terapijaId: String, naruceno: Boolean) {
        val t = db.terapijaDao().getById(terapijaId) ?: return
        db.terapijaDao().upsert(t.copy(narucenoTs = if (naruceno) System.currentTimeMillis() else null))
    }

    private suspend fun promijeniZalihu(t: TerapijaEntity, promjena: Double) {
        if (t.zalihaSet) db.terapijaDao().upsert(t.copy(kolicina = maxOf(0.0, t.kolicina + promjena)))
    }

    /** Dodir na čip doze (iOS toggleDose): nema zapisa → uzeto; preskočeno → uzeto; uzeto → poništi i vrati zalihu. */
    suspend fun prebaciDozu(terapijaId: String, slot: String, dan: Long) = db.withTransaction {
        val t = db.terapijaDao().getById(terapijaId) ?: return@withTransaction
        val p = db.uzimanjeDao().getForSlot(terapijaId, dan, slot)
        if (p != null && !p.preskoceno) {
            db.uzimanjeDao().deleteForSlot(terapijaId, dan, slot)
            promijeniZalihu(t, p.kolicina)
        } else {
            val osnova = p ?: UzimanjeEntity(terapijaId = t.id, clanId = t.clanId, datum = dan, slot = slot)
            db.uzimanjeDao().upsert(osnova.copy(ts = System.currentTimeMillis(), preskoceno = false, kolicina = t.dozaKom))
            promijeniZalihu(t, -t.dozaKom)
        }
    }

    /** Preskakanje doze (iOS skipDose): ne troši zalihu; ponovni poziv poništava; uzeta doza vraća zalihu. */
    suspend fun preskociDozu(terapijaId: String, slot: String, dan: Long) = db.withTransaction {
        val t = db.terapijaDao().getById(terapijaId) ?: return@withTransaction
        val p = db.uzimanjeDao().getForSlot(terapijaId, dan, slot)
        if (p == null) {
            db.uzimanjeDao().upsert(UzimanjeEntity(terapijaId = t.id, clanId = t.clanId, datum = dan, slot = slot, kolicina = 0.0, preskoceno = true))
        } else if (p.preskoceno) {
            db.uzimanjeDao().deleteForSlot(terapijaId, dan, slot)
        } else {
            db.uzimanjeDao().upsert(p.copy(kolicina = 0.0, preskoceno = true))
            promijeniZalihu(t, p.kolicina)
        }
    }
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
        // Uzeta pa naknadno preskočena doza zalihu ne vraća — isto kao iOS.
        if (!preskoceno && t.zalihaSet) db.terapijaDao().upsert(t.copy(kolicina = maxOf(0.0, t.kolicina - t.dozaKom)))
    }

    fun dogadjajiZaClana(clanId: String): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeByClan(clanId)
    fun nadolazeciDogadjaji(fromMillis: Long): Flow<List<DogadjajEntity>> = db.dogadjajDao().observeUpcoming(fromMillis)
    suspend fun upsertDogadjaj(d: DogadjajEntity) = db.dogadjajDao().upsert(d)
    suspend fun deleteDogadjaj(d: DogadjajEntity) = db.dogadjajDao().delete(d)

    fun dokumentiZaClana(clanId: String): Flow<List<DokumentEntity>> = db.dokumentDao().observeByClan(clanId)
    suspend fun upsertDokument(d: DokumentEntity) = db.dokumentDao().upsert(d)
    /** Briše zapis i samu datoteku — dokument ne smije ostati na disku nakon brisanja. */
    suspend fun deleteDokument(d: DokumentEntity) {
        db.dokumentDao().delete(d)
        datoteke.dokument(d.id).delete()
    }

    fun dokumentDatoteka(id: String): java.io.File = datoteke.dokument(id)

    /** Novi dokument u kartoteci (iOS saveDocument). Isti sadržaj kod istog člana se ne dodaje ponovno → false. */
    suspend fun spremiDokument(clanId: String, bajtovi: ByteArray, mime: String, ime: String): Boolean = withContext(Dispatchers.IO) {
        val vecPostoji = db.dokumentDao().observeByClan(clanId).first().any { p ->
            p.velicina == bajtovi.size && datoteke.dokument(p.id).let { it.exists() && it.readBytes().contentEquals(bajtovi) }
        }
        if (vecPostoji) return@withContext false
        val d = DokumentEntity(clanId = clanId, vrsta = "nalaz", naziv = ime, origIme = ime, mime = mime, velicina = bajtovi.size,
                               datum = System.currentTimeMillis(), obradaFaza = "ceka")
        datoteke.dokument(d.id).writeBytes(bajtovi)
        db.dokumentDao().upsert(d)
        true
    }

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
