package com.mojtijek.doktor.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClanDao {
    @Query("SELECT * FROM clan WHERE aktivan = 1 ORDER BY redoslijed ASC")
    fun observeAll(): Flow<List<ClanEntity>>

    @Query("SELECT * FROM clan WHERE id = :id")
    suspend fun getById(id: String): ClanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(uzimanje: UzimanjeEntity)

    @Query("DELETE FROM uzimanje WHERE terapijaId = :terapijaId AND datum = :dan AND slot = :slot")
    suspend fun deleteForSlot(terapijaId: String, dan: Long, slot: String)
}

@Dao
interface DogadjajDao {
    @Query("SELECT * FROM dogadjaj WHERE clanId = :clanId ORDER BY datum ASC")
    fun observeByClan(clanId: String): Flow<List<DogadjajEntity>>

    @Query("SELECT * FROM dogadjaj WHERE datum >= :from ORDER BY datum ASC")
    fun observeUpcoming(from: Long): Flow<List<DogadjajEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dogadjaj: DogadjajEntity)

    @Delete
    suspend fun delete(dogadjaj: DogadjajEntity)
}

@Dao
interface DokumentDao {
    @Query("SELECT * FROM dokument WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<DokumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lab: LabNalazEntity)
}

@Dao
interface MjerenjeDao {
    @Query("SELECT * FROM mjerenje WHERE clanId = :clanId ORDER BY ts DESC")
    fun observeByClan(clanId: String): Flow<List<MjerenjeEntity>>

    @Query("SELECT * FROM mjerenje WHERE clanId = :clanId AND tip = :tip ORDER BY ts ASC")
    fun observeByType(clanId: String, tip: String): Flow<List<MjerenjeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mjerenje: MjerenjeEntity)

    @Delete
    suspend fun delete(mjerenje: MjerenjeEntity)
}

@Dao
interface CijepljenjeDao {
    @Query("SELECT * FROM cijepljenje WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<CijepljenjeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cijepljenje: CijepljenjeEntity)

    @Delete
    suspend fun delete(cijepljenje: CijepljenjeEntity)
}

@Dao
interface DnevnikDao {
    @Query("SELECT * FROM dnevnik_unos WHERE clanId = :clanId ORDER BY datum DESC")
    fun observeByClan(clanId: String): Flow<List<DnevnikUnosEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(unos: DnevnikUnosEntity)

    @Delete
    suspend fun delete(unos: DnevnikUnosEntity)
}

@Dao
interface MenstruacijaDao {
    @Query("SELECT * FROM menstruacija WHERE clanId = :clanId ORDER BY datumPocetka DESC")
    fun observeByClan(clanId: String): Flow<List<MenstruacijaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: MenstruacijaEntity)

    @Delete
    suspend fun delete(m: MenstruacijaEntity)
}

@Database(
    entities = [
        ClanEntity::class, TerapijaEntity::class, UzimanjeEntity::class,
        DogadjajEntity::class, DokumentEntity::class, LabNalazEntity::class,
        MjerenjeEntity::class, CijepljenjeEntity::class, DnevnikUnosEntity::class,
        MenstruacijaEntity::class
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

    companion object {
        @Volatile private var INSTANCE: MojTijekDatabase? = null

        fun getInstance(context: android.content.Context): MojTijekDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MojTijekDatabase::class.java,
                    "mojtijek.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
