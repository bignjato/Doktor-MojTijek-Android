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
