package com.mojtijek.doktor.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClanDao {
    @Query("SELECT * FROM clan WHERE aktivan = 1 ORDER BY redoslijed ASC")
    fun observeAll(): Flow<List<ClanEntity>>

    @Query("SELECT * FROM clan WHERE id = :id")
    suspend fun getById(id: String): ClanEntity?

    @Upsert
    suspend fun upsert(clan: ClanEntity)

    @Delete
    suspend fun delete(clan: ClanEntity)

    @Query("SELECT COUNT(*) FROM clan")
    suspend fun count(): Int
}

@Dao
interface TerapijaDao {
    @Query("SELECT * FROM terapija WHERE clanId = :clanId AND aktivna = 1 ORDER BY naziv ASC")
    fun observeByClan(clanId: String): Flow<List<TerapijaEntity>>

    @Query("SELECT * FROM terapija WHERE clanId = :clanId ORDER BY naziv ASC")
    fun observeAllByClan(clanId: String): Flow<List<TerapijaEntity>>

    @Query("SELECT * FROM terapija WHERE id = :id")
    suspend fun getById(id: String): TerapijaEntity?

    @Upsert
    suspend fun upsert(terapija: TerapijaEntity)

    @Delete
    suspend fun delete(terapija: TerapijaEntity)
}

@Dao
interface UzimanjeDao {
    @Query("SELECT * FROM uzimanje WHERE terapijaId = :terapijaId ORDER BY ts DESC")
    fun observeByTerapija(terapijaId: String): Flow<List<UzimanjeEntity>>

    @Query("SELECT * FROM uzimanje WHERE clanId = :clanId AND datum = :dan")
    fun observeByClanAndDay(clanId: String, dan: Long): Flow<List<UzimanjeEntity>>
    
    @Query("SELECT * FROM uzimanje WHERE clanId = :clanId ORDER BY ts DESC")
    fun observeByClan(clanId: String): Flow<List<UzimanjeEntity>>

    @Upsert
    suspend fun upsert(uzimanje: UzimanjeEntity)

    @Query("SELECT * FROM uzimanje WHERE terapijaId = :terapijaId AND datum = :dan AND slot = :slot LIMIT 1")
    suspend fun getForSlot(terapijaId: String, dan: Long, slot: String): UzimanjeEntity?

    @Query("DELETE FROM uzimanje WHERE terapijaId = :terapijaId AND datum = :dan AND slot = :slot")
    suspend fun deleteForSlot(terapijaId: String, dan: Long, slot: String)
}

@Dao
interface DogadjajDao {
    @Query("SELECT * FROM dogadjaj WHERE clanId = :clanId ORDER BY datum ASC")
    fun observeByClan(clanId: String): Flow<List<DogadjajEntity>>

    @Query("SELECT * FROM dogadjaj WHERE datum >= :from ORDER BY datum ASC")
    fun observeUpcoming(from: Long): Flow<List<DogadjajEntity>>

    @Upsert
    suspend fun upsert(dogadjaj: DogadjajEntity)

    @Delete
    suspend fun delete(dogadjaj: DogadjajEntity)
}

@Dao
interface DokumentDao {
    @Query("SELECT * FROM dokument WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<DokumentEntity>>

    @Upsert
    suspend fun upsert(dokument: DokumentEntity)

    @Delete
    suspend fun delete(dokument: DokumentEntity)
}

@Dao
interface LabNalazDao {
    @Query("SELECT * FROM lab_nalaz WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<LabNalazEntity>>

    @Query("SELECT * FROM lab_nalaz WHERE clanId = :clanId AND kratica = :kratica ORDER BY datum ASC")
    fun observeTrend(clanId: String, kratica: String): Flow<List<LabNalazEntity>>

    @Upsert
    suspend fun upsert(lab: LabNalazEntity)
}

@Dao
interface MjerenjeDao {
    @Query("SELECT * FROM mjerenje WHERE clanId = :clanId ORDER BY ts DESC")
    fun observeByClan(clanId: String): Flow<List<MjerenjeEntity>>

    @Query("SELECT * FROM mjerenje WHERE clanId = :clanId AND tip = :tip ORDER BY ts ASC")
    fun observeByType(clanId: String, tip: String): Flow<List<MjerenjeEntity>>

    @Upsert
    suspend fun upsert(mjerenje: MjerenjeEntity)

    @Delete
    suspend fun delete(mjerenje: MjerenjeEntity)
}

@Dao
interface CijepljenjeDao {
    @Query("SELECT * FROM cijepljenje WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<CijepljenjeEntity>>

    @Upsert
    suspend fun upsert(cijepljenje: CijepljenjeEntity)

    @Delete
    suspend fun delete(cijepljenje: CijepljenjeEntity)
}

@Dao
interface DnevnikDao {
    @Query("SELECT * FROM dnevnik_unos WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<DnevnikUnosEntity>>

    @Upsert
    suspend fun upsert(unos: DnevnikUnosEntity)

    @Delete
    suspend fun delete(unos: DnevnikUnosEntity)
}

@Dao
interface MenstruacijaDao {
    @Query("SELECT * FROM menstruacija WHERE clanId = :clanId ORDER BY datumPocetka DESC")
    fun observeByClan(clanId: String): Flow<List<MenstruacijaEntity>>

    @Upsert
    suspend fun upsert(m: MenstruacijaEntity)

    @Delete
    suspend fun delete(m: MenstruacijaEntity)
}

/** Sve za sigurnosnu kopiju: cijele tablice redom unosa, skupni upis, brisanje. */
@Dao
interface KopijaDao {
    @Query("SELECT * FROM clan ORDER BY redoslijed, rowid") suspend fun clanovi(): List<ClanEntity>
    @Query("SELECT * FROM terapija ORDER BY rowid") suspend fun terapije(): List<TerapijaEntity>
    @Query("SELECT * FROM uzimanje ORDER BY rowid") suspend fun uzimanja(): List<UzimanjeEntity>
    @Query("SELECT * FROM dogadjaj ORDER BY rowid") suspend fun dogadjaji(): List<DogadjajEntity>
    @Query("SELECT * FROM dokument ORDER BY rowid") suspend fun dokumenti(): List<DokumentEntity>
    @Query("SELECT * FROM lab_nalaz ORDER BY rowid") suspend fun lab(): List<LabNalazEntity>
    @Query("SELECT * FROM mjerenje ORDER BY rowid") suspend fun mjerenja(): List<MjerenjeEntity>
    @Query("SELECT * FROM cijepljenje ORDER BY rowid") suspend fun cijepljenja(): List<CijepljenjeEntity>
    @Query("SELECT * FROM dnevnik_unos ORDER BY rowid") suspend fun dnevnik(): List<DnevnikUnosEntity>
    @Query("SELECT * FROM menstruacija ORDER BY rowid") suspend fun ciklusi(): List<MenstruacijaEntity>
    @Query("SELECT * FROM narudzba ORDER BY rowid") suspend fun narudzbe(): List<NarudzbaEntity>
    @Query("SELECT * FROM narudzba_stavka ORDER BY rowid") suspend fun stavke(): List<NarudzbaStavkaEntity>
    @Query("SELECT * FROM audit ORDER BY rowid") suspend fun audit(): List<AuditEntity>

    @Insert suspend fun dodajClanove(x: List<ClanEntity>)
    @Insert suspend fun dodajTerapije(x: List<TerapijaEntity>)
    @Insert suspend fun dodajUzimanja(x: List<UzimanjeEntity>)
    @Insert suspend fun dodajDogadjaje(x: List<DogadjajEntity>)
    @Insert suspend fun dodajDokumente(x: List<DokumentEntity>)
    @Insert suspend fun dodajLab(x: List<LabNalazEntity>)
    @Insert suspend fun dodajMjerenja(x: List<MjerenjeEntity>)
    @Insert suspend fun dodajCijepljenja(x: List<CijepljenjeEntity>)
    @Insert suspend fun dodajDnevnik(x: List<DnevnikUnosEntity>)
    @Insert suspend fun dodajCikluse(x: List<MenstruacijaEntity>)
    @Insert suspend fun dodajNarudzbe(x: List<NarudzbaEntity>)
    @Insert suspend fun dodajStavke(x: List<NarudzbaStavkaEntity>)
    @Insert suspend fun dodajAudit(x: List<AuditEntity>)

    /** Kaskadno briše sve što visi na članovima. */
    @Query("DELETE FROM clan") suspend fun obrisiClanove()
    @Query("DELETE FROM narudzba") suspend fun obrisiNarudzbe()
    @Query("DELETE FROM audit") suspend fun obrisiAudit()
}

@Database(
    entities = [
        ClanEntity::class, TerapijaEntity::class, UzimanjeEntity::class,
        DogadjajEntity::class, DokumentEntity::class, LabNalazEntity::class,
        MjerenjeEntity::class, CijepljenjeEntity::class, DnevnikUnosEntity::class,
        MenstruacijaEntity::class,
        NarudzbaEntity::class, NarudzbaStavkaEntity::class, AuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MojTijekDatabase : RoomDatabase() {
    abstract fun clanDao(): ClanDao
    abstract fun terapijaDao(): TerapijaDao
    abstract fun uzimanjeDao(): UzimanjeDao
    abstract fun dogadjajDao(): DogadjajDao
    abstract fun dokumentDao(): DokumentDao
    abstract fun labNalazDao(): LabNalazDao
    abstract fun mjerenjeDao(): MjerenjeDao
    abstract fun cijepljenjeDao(): CijepljenjeDao
    abstract fun dnevnikDao(): DnevnikDao
    abstract fun menstruacijaDao(): MenstruacijaDao
    abstract fun kopijaDao(): KopijaDao

    companion object {
        @Volatile private var INSTANCE: MojTijekDatabase? = null

        fun getInstance(context: android.content.Context): MojTijekDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MojTijekDatabase::class.java,
                    "mojtijek.db"
                ).build().also { INSTANCE = it }
            }
    }
}
