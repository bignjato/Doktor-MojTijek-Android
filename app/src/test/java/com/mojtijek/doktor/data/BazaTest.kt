package com.mojtijek.doktor.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Obična Application: DoktorApp bi u svakom testu ponovno pokretao Koin.
@Config(sdk = [34], application = android.app.Application::class)
class BazaTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MojTijekDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = MojTijekRepository(db, Datoteke(java.nio.file.Files.createTempDirectory("mt").toFile()))
    private val dan = DoseSchedule.dayStart()

    @After fun zatvori() = db.close()

    /** Uređivanje člana ili terapije ne smije obrisati ono što visi na njima (REPLACE + CASCADE je to radio). */
    @Test fun uredjivanjeNeBriseDjecu() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        val t = TerapijaEntity(clanId = clan.id, naziv = "Lijek", vremena = "08:00"); repo.upsertTerapija(t)
        repo.zabiljeziDozu(t.id, "08:00", dan, preskoceno = false)

        repo.upsertClan(clan.copy(ime = "Test Novi"))
        assertEquals(1, repo.terapijeSve(clan.id).first().size)

        repo.upsertTerapija(t.copy(jacina = "5 mg"))
        assertEquals(1, repo.uzimanjaZaDan(clan.id, dan).first().size)
    }

    @Test fun potvrdaDozeJeJednokratna() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        val t = TerapijaEntity(clanId = clan.id, naziv = "Lijek", vremena = "08:00", dozaKom = 2.0, kolicina = 10.0)
        repo.upsertTerapija(t)

        repo.zabiljeziDozu(t.id, "08:00", dan, preskoceno = true)
        assertEquals(10.0, db.terapijaDao().getById(t.id)!!.kolicina, 0.0)   // preskočeno ne troši zalihu

        repo.zabiljeziDozu(t.id, "08:00", dan, preskoceno = false)
        repo.zabiljeziDozu(t.id, "08:00", dan, preskoceno = false)           // drugi dodir
        val zapisi = repo.uzimanjaZaDan(clan.id, dan).first()
        assertEquals(1, zapisi.size)
        assertEquals(false, zapisi[0].preskoceno)
        assertEquals(8.0, db.terapijaDao().getById(t.id)!!.kolicina, 0.0)

        // Potrošena zaliha je „0 dana”, ne „zaliha se ne prati”.
        (1..4).forEach { repo.zabiljeziDozu(t.id, "08:00", dan - it * 86_400_000L, preskoceno = false) }
        val prazna = db.terapijaDao().getById(t.id)!!
        assertEquals(0.0, prazna.kolicina, 0.0)
        assertEquals(0.0, DoseSchedule.danaPreostalo(prazna)!!, 0.0)
        assertEquals(null, DoseSchedule.danaPreostalo(TerapijaEntity(clanId = clan.id, naziv = "Bez zalihe", vremena = "08:00")))
    }

    /** Pravila početnog ekrana (iOS homeDoses / dueToday). */
    @Test fun pocetniEkranNudiOtvoreneDoze() {
        val t = TerapijaEntity(clanId = "c", naziv = "A", vremena = "08:00,20:00")
        val podne = dan + 12 * 3_600_000L
        assertEquals(listOf("08:00", "20:00"), DoseSchedule.homeDoses(listOf(t), emptyList(), podne).map { it.slot })
        assertEquals("buduća doza ne ulazi u „Uzeto sve”", listOf("08:00"), DoseSchedule.dueToday(listOf(t), emptyList(), podne).map { it.slot })

        // Uzeta i preskočena doza su obje riješene; tada se nudi samo prva sutrašnja.
        val rijeseno = listOf(UzimanjeEntity(terapijaId = t.id, clanId = "c", datum = dan, slot = "08:00"),
                              UzimanjeEntity(terapijaId = t.id, clanId = "c", datum = dan, slot = "20:00", preskoceno = true))
        val sutra = DoseSchedule.homeDoses(listOf(t), rijeseno, podne).single()
        assertEquals("08:00", sutra.slot)
        assertTrue(sutra.plannedMillis > dan + 20 * 3_600_000L)
        assertTrue(DoseSchedule.dueToday(listOf(t), rijeseno, podne).isEmpty())

        // Bez upisanih vremena vrijede zadana prema broju doza dnevno.
        assertEquals(listOf("08:00", "14:00", "20:00"), DoseSchedule.effectiveSlots(TerapijaEntity(clanId = "c", naziv = "B", putaDnevno = 3)))
    }

    /** Čip doze: uzeto ↔ poništeno vraća zalihu; preskočeno ne troši; preskočeno → uzeto troši jednom. */
    @Test fun cipDozePrebacujeIPreskace() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        val t = TerapijaEntity(clanId = clan.id, naziv = "Lijek", vremena = "08:00", kolicina = 10.0); repo.upsertTerapija(t)
        suspend fun zaliha() = db.terapijaDao().getById(t.id)!!.kolicina
        suspend fun zapisi() = repo.uzimanjaZaDan(clan.id, dan).first()

        repo.prebaciDozu(t.id, "08:00", dan); assertEquals(9.0, zaliha(), 0.0)
        repo.prebaciDozu(t.id, "08:00", dan); assertEquals(10.0, zaliha(), 0.0); assertTrue(zapisi().isEmpty())

        repo.preskociDozu(t.id, "08:00", dan); assertEquals(10.0, zaliha(), 0.0); assertTrue(zapisi().single().preskoceno)
        repo.prebaciDozu(t.id, "08:00", dan); assertEquals(9.0, zaliha(), 0.0); assertEquals(false, zapisi().single().preskoceno)
        repo.preskociDozu(t.id, "08:00", dan); assertEquals("uzeta pa preskočena vraća zalihu", 10.0, zaliha(), 0.0)
        repo.preskociDozu(t.id, "08:00", dan); assertTrue("ponovno preskakanje poništava", zapisi().isEmpty())
    }

    /** Dokument: isti sadržaj se ne dodaje dvaput; brisanje miče i datoteku s diska. */
    @Test fun dokumentBezDuplikataIBrisanjeDatoteke() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        val pdf = byteArrayOf(1, 2, 3, 4)
        assertTrue(repo.spremiDokument(clan.id, pdf, "application/pdf", "nalaz.pdf"))
        assertEquals("isti sadržaj", false, repo.spremiDokument(clan.id, pdf.copyOf(), "application/pdf", "drugo-ime.pdf"))
        assertTrue("ista veličina, drugi sadržaj", repo.spremiDokument(clan.id, byteArrayOf(1, 2, 3, 5), "application/pdf", "b.pdf"))
        val d = repo.dokumentiZaClana(clan.id).first().first { it.naziv == "nalaz.pdf" }
        assertEquals("ceka", d.obradaFaza); assertEquals(4, d.velicina)
        assertTrue(repo.dokumentDatoteka(d.id).readBytes().contentEquals(pdf))
        repo.deleteDokument(d)
        assertEquals(false, repo.dokumentDatoteka(d.id).exists())
        assertEquals(1, repo.dokumentiZaClana(clan.id).first().size)
    }

    /** Health Connect: jedan zapis po pokazatelju po danu; ručni unos ostaje; nepoznat član ne ruši upis. */
    @Test fun zdravljeSeUpisujeBezDuplikata() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        repo.upsertMjerenje(MjerenjeEntity(clanId = clan.id, tip = "tlak", vrijednost = 130.0, vrijednost2 = 85.0, ts = dan + 1000))
        val sada = dan + 3_600_000L
        assertEquals(2, repo.upisiZdravlje(clan.id, mapOf("koraci" to Ocitanje(1200.0, sada), "tlak" to Ocitanje(120.0, sada, 80.0))))
        assertEquals(1, repo.upisiZdravlje(clan.id, mapOf("koraci" to Ocitanje(4300.0, sada + 60_000))))
        repo.upisiZdravlje(clan.id, mapOf("koraci" to Ocitanje(900.0, sada + 86_400_000L)))
        val m = repo.mjerenjaZaClana(clan.id).first()
        val koraci = m.filter { it.tip == "koraci" }.sortedBy { it.ts }
        assertEquals(listOf(4300.0, 900.0), koraci.map { it.vrijednost })
        assertTrue(koraci.all { it.izvor == "healthconnect" })
        val tlak = m.filter { it.tip == "tlak" }
        assertEquals(2, tlak.size)
        assertEquals(85.0, tlak.single { it.izvor == "rucno" }.vrijednost2!!, 0.0)
        assertEquals(80.0, tlak.single { it.izvor == "healthconnect" }.vrijednost2!!, 0.0)
        assertTrue(db.clanDao().getById(clan.id)!!.zadnjiIngest != null)
        assertEquals(0, repo.upisiZdravlje("nema-ga", mapOf("koraci" to Ocitanje(1.0, sada))))
    }

    /** Koraci: najveće očitanje dana, pa zbroj dana. Ostalo: prosjek dana, pa prosjek dana. */
    @Test fun dnevneVrijednostiIAgregat() {
        fun m(tip: String, v: Double, unatrag: Int, v2: Double? = null) =
            MjerenjeEntity(clanId = "c", tip = tip, vrijednost = v, vrijednost2 = v2, ts = dan - unatrag * 86_400_000L + 3_600_000L)
        val koraci = Pracenje.dnevneVrijednosti("koraci", listOf(m("koraci", 1000.0, 0), m("koraci", 8000.0, 0), m("koraci", 5000.0, 1)))
        assertEquals(listOf(5000.0, 8000.0), koraci.map { it.vrijednost })
        assertEquals(13000.0, Pracenje.agregat("koraci", koraci), 0.0)
        val tlak = Pracenje.dnevneVrijednosti("tlak", listOf(m("tlak", 120.0, 0, 80.0), m("tlak", 140.0, 0, 90.0), m("tlak", 110.0, 1, 70.0)))
        assertEquals(listOf(110.0, 130.0), tlak.map { it.vrijednost }); assertEquals(85.0, tlak.last().vrijednost2!!, 0.0)
        assertEquals(120.0, Pracenje.agregat("tlak", tlak), 0.0)
        assertEquals(0.0, Pracenje.agregat("tlak", emptyList()), 0.0)
    }

    @Test fun napuniINaruceno() = runBlocking {
        val clan = ClanEntity(ime = "Test"); repo.upsertClan(clan)
        val t = TerapijaEntity(clanId = clan.id, naziv = "Lijek", vremena = "08:00", kolicina = 2.0, komPoKutiji = 28.0); repo.upsertTerapija(t)
        repo.oznaciNaruceno(t.id, true)
        assertEquals(false, DoseSchedule.trebaNaruciti(db.terapijaDao().getById(t.id)!!))
        repo.napuniZalihu(t.id)
        val puna = db.terapijaDao().getById(t.id)!!
        assertEquals(30.0, puna.kolicina, 0.0); assertEquals(null, puna.narucenoTs)
    }

    /** Adherencija: tjedna terapija se očekuje samo svoj dan; preskočena doza nije uzeta. */
    @Test fun adherencijaINiz() {
        val dnevna = TerapijaEntity(clanId = "c", naziv = "A", vremena = "08:00")
        fun uzeto(unatrag: Int, preskoceno: Boolean = false) =
            UzimanjeEntity(terapijaId = dnevna.id, clanId = "c", datum = DoseSchedule.dayStart(dan - unatrag * 86_400_000L + 3_600_000L), slot = "08:00", preskoceno = preskoceno)
        val u = listOf(uzeto(1), uzeto(2), uzeto(3, preskoceno = true))
        assertEquals(2, Adherencija.niz(listOf(dnevna), u, dan + 1))
        assertEquals(listOf(0.0, 100.0, 100.0, 0.0), Adherencija.poDanu(listOf(dnevna), u, dan + 1, 4))
        assertEquals(50, Adherencija.prosjek(listOf(dnevna), u, dan + 1, 4))
        // Tjedna terapija bez ijedne doze: šest od sedam dana nema plana i broji se kao ispunjeno.
        val tjedna = TerapijaEntity(clanId = "c", naziv = "T", vremena = "08:00", danUTjednu = 3)
        assertEquals(6, Adherencija.poDanu(listOf(tjedna), emptyList(), dan + 1, 7).count { it == 100.0 })
        assertEquals("ujutro", DoseSchedule.dioDana("08:00")); assertEquals("podne", DoseSchedule.dioDana("14:30")); assertEquals("navecer", DoseSchedule.dioDana("20:00"))
    }

    @Test fun priKrajuPoPraguINarudzbi() {
        val t = TerapijaEntity(clanId = "c", naziv = "A", vremena = "08:00", kolicina = 3.0, zalihaSet = true)
        assertTrue(DoseSchedule.trebaNaruciti(t))
        assertEquals(false, DoseSchedule.trebaNaruciti(t.copy(kolicina = 4.0)))
        assertEquals(true, DoseSchedule.trebaNaruciti(t.copy(kolicina = 4.0, pragDana = 7)))
        assertEquals("već naručeno", false, DoseSchedule.trebaNaruciti(t.copy(narucenoTs = 1L)))
        assertEquals("zaliha se ne vodi", false, DoseSchedule.trebaNaruciti(t.copy(zalihaSet = false)))
    }

    @Test fun rasporedPostujeTjedniDan() {
        val t = TerapijaEntity(clanId = "c", naziv = "Tjedni", vremena = "20:00,08:00", danUTjednu = 2)
        val tjedan = (0..6).map { dan + it * 86_400_000L }
        assertEquals(2, tjedan.sumOf { DoseSchedule.entriesForDay(listOf(t), it).size })
        assertEquals(null, DoseSchedule.parse("08:00,8:00"))                  // duplikat vremena
        assertEquals(listOf("08:00", "20:00"), DoseSchedule.effectiveSlots(t))
    }
}
