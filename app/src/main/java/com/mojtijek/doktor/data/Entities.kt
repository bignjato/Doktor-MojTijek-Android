package com.mojtijek.doktor.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clan")
data class ClanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ime: String = "",
    val datumRodjenja: Long? = null,
    val oib: String? = null,
    val spol: String? = null,
    val krvnaGrupa: String? = null,
    val visina: Double? = null,
    val alergije: String? = null,
    val kronicneBolesti: String? = null,
    val lijecnik: String? = null,
    val lijecnikEmail: String? = null,
    val mbo: String? = null,
    val hitniKontakt: String? = null,
    val hitniTelefon: String? = null,
    val hitnaNapomena: String? = null,
    val boja: String = "#0891b2",
    val redoslijed: Int = 0,
    val aktivan: Boolean = true,
    val stvoren: Long = System.currentTimeMillis(),
    // ginekologija
    val kontracepcija: String? = null,
    val zadnjiPapaTest: Long? = null,
    val zadnjiGinekoloski: Long? = null,
    val prosjecniCiklus: Int? = null,
    val trudna: Boolean = false,
    val terminPorod: Long? = null
)

@Entity(
    tableName = "terapija",
    foreignKeys = [ForeignKey(
        entity = ClanEntity::class,
        parentColumns = ["id"],
        childColumns = ["clanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clanId")]
)
data class TerapijaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val naziv: String = "",
    val jacina: String? = null,
    val oblik: String = "tableta",
    val atc: String? = null,
    val dozaKom: Double = 1.0,
    val putaDnevno: Int = 1,
    val vremena: String = "", // CSV "08:00,20:00"
    val danUTjednu: Int? = null, // 1=ned..7=sub, null=svaki dan
    val komPoKutiji: Double = 30.0,
    val kolicina: Double = 0.0,
    val pragDana: Int? = null,
    val trajni: Boolean = true,
    val razlog: String? = null,
    val napomena: String? = null,
    val opis: String? = null,
    val aktivna: Boolean = true,
    val autoRezim: Boolean = true,
    val datumPocetka: Long? = null,
    val datumKraja: Long? = null,
    val datumPunjenja: Long? = null,
    val narucenoTs: Long? = null,
    val receptDo: Long? = null,
    val stvoren: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "uzimanje",
    foreignKeys = [
        ForeignKey(entity = TerapijaEntity::class, parentColumns = ["id"], childColumns = ["terapijaId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("terapijaId"), Index("clanId")]
)
data class UzimanjeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val terapijaId: String,
    val clanId: String,
    val ts: Long = System.currentTimeMillis(),
    val datum: Long? = null,
    val slot: String? = null,
    val kolicina: Double = 1.0,
    val korisnik: String? = null,
    val preskoceno: Boolean = false
)

@Entity(
    tableName = "dogadjaj",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class DogadjajEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val vrsta: String = "pregled",
    val naslov: String = "",
    val datum: Long = System.currentTimeMillis(),
    val vrijeme: String? = null,
    val lokacija: String? = null,
    val priprema: String? = null,
    val uputnicaPotrebna: Boolean = false,
    val uputnicaZatrazena: Boolean = false,
    val uputnicaIzdana: Boolean = false,
    val narudzbaObavljena: Boolean = false,
    val status: String = "planirano",
    val podsjetnikDana: Int = 3,
    val napomena: String? = null,
    val vozac: String? = null
)

@Entity(
    tableName = "dokument",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class DokumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val vrsta: String = "nalaz",
    val naziv: String? = null,
    val datum: Long? = null,
    val ustanova: String? = null,
    val lijecnik: String? = null,
    val napomena: String? = null,
    val objasnjenje: String? = null,
    val poslanoDoktoru: Boolean = false,
    val stvoren: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "lab_nalaz",
    foreignKeys = [
        ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DokumentEntity::class, parentColumns = ["id"], childColumns = ["dokumentId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("clanId"), Index("dokumentId")]
)
data class LabNalazEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val dokumentId: String? = null,
    val datum: Long? = null,
    val naziv: String = "",
    val kratica: String? = null,
    val vrijednost: Double? = null,
    val jedinica: String? = null,
    val refLo: Double? = null,
    val refHi: Double? = null,
    val status: String? = null
)

@Entity(
    tableName = "mjerenje",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class MjerenjeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val tip: String = "",
    val vrijednost: Double = 0.0,
    val vrijednost2: Double? = null,
    val jedinica: String? = null,
    val ts: Long = System.currentTimeMillis(),
    val napomena: String? = null,
    val izvor: String = "rucno"
)

@Entity(
    tableName = "cijepljenje",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class CijepljenjeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val naziv: String = "",
    val datum: Long? = null,
    val sljedece: Long? = null,
    val napomena: String? = null
)

@Entity(
    tableName = "dnevnik_unos",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class DnevnikUnosEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val datum: Long? = null,
    val vrsta: String = "biljeska",
    val naslov: String? = null,
    val raspolozenje: Int? = null,
    val jacina: Int? = null,
    val tekst: String? = null
)

@Entity(
    tableName = "menstruacija",
    foreignKeys = [ForeignKey(entity = ClanEntity::class, parentColumns = ["id"], childColumns = ["clanId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clanId")]
)
data class MenstruacijaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val clanId: String,
    val datumPocetka: Long = System.currentTimeMillis(),
    val datumKraja: Long? = null,
    val trajanjeDana: Int? = null,
    val tok: String? = null,
    val raspolozenje: Int? = null,
    val simptomi: String? = null,
    val napomena: String? = null
)
