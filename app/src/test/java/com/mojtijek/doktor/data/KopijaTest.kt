package com.mojtijek.doktor.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

/** Kopija mora biti izmjenjiva s iOS-om: što iOS izveze, Android mora vratiti i ponovno izvesti bez gubitka. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class KopijaTest {
    private val baze = ArrayList<MojTijekDatabase>()
    private fun baza() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MojTijekDatabase::class.java)
        .allowMainThreadQueries().build().also { baze += it }
    private fun mapa() = Datoteke(Files.createTempDirectory("mt").toFile())

    @After fun zatvori() = baze.forEach { it.close() }

    /** Sve što je u `ocekivano` mora biti i u `stvarno` (rekurzivno); `stvarno` smije imati više. */
    private fun sadrzi(ocekivano: Any?, stvarno: Any?, put: String) {
        when (ocekivano) {
            is JSONObject -> {
                assertTrue("$put nije objekt", stvarno is JSONObject)
                for (k in ocekivano.keys()) if (k != "izvezeno") sadrzi(ocekivano.get(k), (stvarno as JSONObject).opt(k), "$put.$k")
            }
            is JSONArray -> {
                assertTrue("$put nije popis", stvarno is JSONArray)
                assertEquals("$put: broj redaka", ocekivano.length(), (stvarno as JSONArray).length())
                for (i in 0 until ocekivano.length()) sadrzi(ocekivano.get(i), stvarno.opt(i), "$put[$i]")
            }
            else -> assertEquals(put, ocekivano.toString(), stvarno?.toString())
        }
    }

    @Test fun iosKopijaProlaziKrozAndroidBezGubitka() = runBlocking {
        val db = baza(); val dat = mapa()
        Kopija.uvezi(db, dat, IOS)

        // U bazi su vrijednosti u Androidovom obliku…
        val t = db.kopijaDao().terapije().single()
        assertEquals("08:00,20:00", t.vremena)
        assertTrue(t.zalihaSet)
        val u = db.kopijaDao().uzimanja().single()
        assertEquals("dan uzimanja je ponoć u zoni uređaja (po njemu se traži „uzeto danas”)", DoseSchedule.dayStart(u.datum!!), u.datum)
        assertArrayEquals("%PDF-1.4 test".toByteArray(), dat.dokument(db.kopijaDao().dokumenti().single().id).readBytes())
        assertEquals(5, dat.lijek(t.id).length())
        assertEquals(db.kopijaDao().dokumenti().single().id, db.kopijaDao().lab().single().dokumentId)

        // …a ponovni izvoz vraća sve što je iOS poslao.
        val van = Kopija.izvezi(db, dat, ukljuciDokumente = true)
        sadrzi(JSONObject(IOS), JSONObject(van), "kopija")

        // I Androidova kopija se vraća u praznu bazu bez razlike.
        val db2 = baza(); val dat2 = mapa()
        Kopija.uvezi(db2, dat2, van)
        sadrzi(JSONObject(van), JSONObject(Kopija.izvezi(db2, dat2, ukljuciDokumente = true)), "drugi krug")
    }

    @Test fun bezPrivoleDatotekeNeIzlaze() = runBlocking {
        val db = baza(); val dat = mapa()
        Kopija.uvezi(db, dat, IOS)
        val van = Kopija.izvezi(db, dat, ukljuciDokumente = false)
        assertFalse(van.contains("sadrzaj_b64")); assertFalse(van.contains("izgled_foto_b64"))
        assertEquals(0, JSONObject(van).getInt("dokumenti_ukljuceni"))
        assertEquals("Opis nalaza.", JSONObject(van).getJSONArray("dokumenti").getJSONObject(0).getString("objasnjenje"))
    }

    @Test fun nevaljanaKopijaNeDiraPodatke() = runBlocking {
        val db = baza(); val dat = mapa()
        Kopija.uvezi(db, dat, IOS)
        val clan = """{"id":1,"ime":"Test"}"""
        for (losa in listOf(
            "nije json", "{}",
            """{"shema_verzija":4,"clanovi":[$clan]}""",
            """{"clanovi":[{"id":1,"ime":" "}]}""",
            """{"clanovi":[$clan,$clan]}""",
            """{"clanovi":[$clan],"terapije":[{"clan_id":9,"naziv":"X"}]}""",
            """{"clanovi":[$clan],"terapije":[{"naziv":"X"}]}""",
            """{"clanovi":[$clan],"terapije":[{"clan_id":1,"naziv":"X","vremena":"[\"25:00\"]"}]}""",
            """{"clanovi":[$clan],"terapije":[{"clan_id":1,"naziv":"X","dan_u_tjednu":8}]}""",
            """{"clanovi":[$clan],"dokumenti":"x"}""",
        )) {
            try { Kopija.uvezi(db, dat, losa); fail("prošla je nevaljana kopija: $losa") } catch (e: Kopija.Nevaljana) { }
        }
        assertEquals("Tea Test", db.kopijaDao().clanovi().single().ime)
        assertTrue(dat.dokument(db.kopijaDao().dokumenti().single().id).exists())
    }

    private companion object {
        /** Oblik koji piše iOS ExportService (shema 3); podaci su izmišljeni. */
        val IOS = """
        {"shema_verzija":3,"izvezeno":"2026-11-17 10:00:00","dokumenti_ukljuceni":1,
         "clanovi":[{"id":1,"uid":"11111111-2222-3333-4444-555555555555","ime":"Tea Test","datum_rodjenja":"1990-05-12","oib":"00000000001",
           "spol":"žensko","mbo":"000000001","krvna_grupa":"A+","alergije":"pelud","kronicne_bolesti":"nema","lijecnik":"dr. Primjer",
           "hitni_kontakt":"Filip","hitni_telefon":"000 000","hitna_napomena":"nema","boja":"#0891b2","aktivan":1,
           "zadnji_ingest":"2026-01-02 03:04:05","tijelo_analiza":"{\"a\":1}","visina":168.5,"lijecnik_email":"ordinacija@example.org",
           "redoslijed":2,"kontracepcija":"nema","zadnji_papa_test":"2025-03-01","zadnji_ginekoloski":"2025-03-02","prosjecni_ciklus":28,
           "trudna":0,"termin_porod":"2027-01-01","stvoren":"2026-01-01 08:00:00"}],
         "terapije":[{"clan_id":1,"uid":"AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE","naziv":"Lijek Primjer","jacina":"5 mg","oblik":"tableta","atc":"X00",
           "doza_kom":0.5,"puta_dnevno":2,"vremena":"[\"08:00\",\"20:00\"]","dan_u_tjednu":2,"kom_po_kutiji":30,"kolicina":12.5,"prag_dana":7,
           "trajni":1,"razlog":"razlog","napomena":"napomena","opis":"opis","aktivna":1,"auto_rezim":0,"zaliha_set":1,
           "datum_pocetka":"2026-01-01","datum_kraja":"2026-12-31","datum_punjenja":"2026-11-01","recept_do":"2026-12-01",
           "naruceno_ts":"2026-11-10 09:00:00","zadnji_decay":"2026-11-16 00:00:00","stvoren":"2026-01-01 08:00:00",
           "izgled_opis":"bijela okrugla tableta","izgled_foto_b64":"/9j/AQI="}],
         "uzimanja":[{"clan_id":1,"terapija_uid":"AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE","ts":"2026-11-16 07:05:00","datum":"2026-11-16",
           "slot":"08:00","kolicina":0.5,"korisnik":"Tea","preskoceno":0}],
         "mjerenja":[{"clan_id":1,"tip":"tlak","vrijednost":120,"vrijednost2":80,"jedinica":"mmHg","ts":"2026-11-16 07:00:00",
           "datum":"2026-11-16","napomena":"jutro","izvor":"rucno"}],
         "lab":[{"clan_id":1,"naziv":"Pretraga","kratica":"PR","vrijednost":4.2,"jedinica":"mmol/L","ref_lo":3.5,"ref_hi":5.5,"status":"ok",
           "datum":"2026-10-01","dokument_id":1}],
         "dogadjaji":[{"clan_id":1,"naslov":"Kontrola","vrsta":"pregled","datum":"2026-12-01","vrijeme":"10:30","lokacija":"Ustanova",
           "priprema":"natašte","uputnica_potrebna":1,"uputnica_zatrazena":1,"uputnica_izdana":0,"status":"planirano","podsjetnik_dana":3,
           "napomena":"n","narudzba_obavljena":1,"vozac":"Filip","vozac_email":"vozac@example.org",
           "uputnica_zatrazena_ts":"2026-11-15 12:00:00","vozac_obavijesten":1,"ek_event_id":"EK-1",
           "dokument_uid":"99999999-8888-7777-6666-555555555555"}],
         "dokumenti":[{"id":1,"uid":"99999999-8888-7777-6666-555555555555","clan_id":1,"vrsta":"nalaz","naziv":"Nalaz","datum":"2026-10-01",
           "ustanova":"Ustanova","lijecnik":"dr. Primjer","orig_ime":"nalaz.pdf","mime":"application/pdf","velicina":13,"napomena":"n",
           "obrada":"gotovo","obrada_faza":"kraj","obrada_info":"{}","objasnjenje":"Opis nalaza.","poslano_doktoru":1,
           "poslano_ts":"2026-10-02 08:00:00","stvoren":"2026-10-01 09:00:00","sadrzaj_b64":"JVBERi0xLjQgdGVzdA=="}],
         "cijepljenja":[{"clan_id":1,"naziv":"Cjepivo","datum":"2021-05-12","sljedece":"2031-05-12","napomena":"doza 2"}],
         "dnevnik":[{"clan_id":1,"datum":"2026-11-15","raspolozenje":4,"tekst":"tekst","vrsta":"simptom","naslov":"Naslov","jacina":2}],
         "ciklusi":[{"clan_id":1,"datum_pocetka":"2026-11-01","datum_kraja":"2026-11-05","trajanje_dana":5,"tok":"umjeren","raspolozenje":3,
           "simptomi":"grčevi","napomena":"n"}],
         "narudzbe":[{"id":"12121212-3434-5656-7878-909090909090","created":"2026-11-10 08:00:00","status":"poslano",
           "primatelj_email":"ordinacija@example.org","poslao_tko":"Tea","poslano_ts":"2026-11-10 08:01:00","napomena":"n","greska":"g",
           "provedeno":1,"provedeno_ts":"2026-11-11 08:00:00","provedeno_od":"Tea","pixel_token":"tok","otvoreno_ts":"2026-11-10 09:00:00",
           "potvrdeno_ts":"2026-11-10 10:00:00",
           "stavke":[{"clan_ime":"Tea Test","naziv":"Lijek Primjer","jacina":"5 mg","kolicina_kutija":2,"dana_preostalo":3.5,
             "terapija_ref":"AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"}]}],
         "audit":[{"ts":"2026-11-16 07:05:00","akcija":"uzeto","objekt":"doza","clan_id_uuid":"11111111-2222-3333-4444-555555555555",
           "detalj":"Lijek Primjer"}]}
        """.trimIndent()
    }
}
