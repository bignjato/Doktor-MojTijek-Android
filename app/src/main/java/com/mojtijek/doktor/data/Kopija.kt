package com.mojtijek.doktor.data

import android.util.Base64
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Sigurnosna kopija u istom JSON formatu kao iOS (ExportService/ImportService, shema 3), da se
 * kopija s jednog telefona može vratiti na drugom. Imena ključeva i pravila provjere su prenesena
 * odande — mijenjaju se samo zajedno s iOS-om.
 *
 * Dani ("yyyy-MM-dd") su kalendarski dani u zoni uređaja, a trenuci ("yyyy-MM-dd HH:mm:ss") UTC.
 * shortcut: kopija s dokumentima slaže se cijela u memoriji (base64), kao na iOS-u; prijeći na
 * strujanje ako se kartoteke s puno PDF-ova pokažu prevelikima.
 */
object Kopija {
    class Nevaljana(val detalj: String) : Exception(detalj)
    data class Sazetak(val clanova: Int, val terapija: Int, val dokumenata: Int, val privitaka: Int, val izvezeno: String?)

    private val TABLICE = listOf("terapije", "mjerenja", "lab", "dogadjaji", "dokumenti", "cijepljenja", "dnevnik",
                                 "ciklusi", "narudzbe", "audit", "uzimanja")

    // ---- datumi ----
    private fun fmt(oblik: String, utc: Boolean) = SimpleDateFormat(oblik, Locale.US).apply {
        isLenient = false; if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }
    private fun dan(ms: Long?): String? = ms?.let { fmt("yyyy-MM-dd", false).format(Date(it)) }
    private fun ts(ms: Long?): String? = ms?.let { fmt("yyyy-MM-dd HH:mm:ss", true).format(Date(it)) }
    private fun datum(o: JSONObject, k: String): Long? {
        val s = str(o, k) ?: return null
        return runCatching { fmt("yyyy-MM-dd HH:mm:ss", true).parse(s)!!.time }.getOrNull()
            ?: runCatching { fmt("yyyy-MM-dd", false).parse(s.take(10))!!.time }.getOrNull()
    }

    // ---- čitanje ----
    private fun str(o: JSONObject, k: String): String? = (o.opt(k) as? String)?.takeIf { it.isNotEmpty() }
    private fun int(o: JSONObject, k: String): Int? = when (val v = o.opt(k)) {
        is Int -> v
        is Long -> v.toInt()
        is Double -> if (v == Math.floor(v) && !v.isInfinite()) v.toInt() else null
        else -> null
    }
    private fun dbl(o: JSONObject, k: String): Double? = (o.opt(k) as? Number)?.toDouble()
    private fun bool(o: JSONObject, k: String, zadano: Boolean): Boolean = int(o, k)?.let { it != 0 } ?: zadano
    private fun redci(o: JSONObject, k: String): List<JSONObject> =
        (o.opt(k) as? JSONArray)?.let { a -> (0 until a.length()).mapNotNull { a.opt(it) as? JSONObject } } ?: emptyList()
    private fun uuid(s: String): String? = runCatching { UUID.fromString(s) }.getOrNull()?.takeIf { s.length == 36 }?.let { s }
    private fun vremenaIzJson(o: JSONObject): List<String>? {
        val s = o.opt("vremena") as? String ?: return emptyList()
        val a = runCatching { JSONArray(s) }.getOrNull() ?: return null
        return (0 until a.length()).map { a.opt(it) as? String ?: return null }
    }
    private fun b(v: Boolean) = if (v) 1 else 0
    /** `put(k, null)` ključ izostavlja — isto što iOS radi s praznim poljima. */
    private fun obj(vararg p: Pair<String, Any?>) = JSONObject().apply { p.forEach { (k, v) -> put(k, v) } }

    /** Provjera prije ikakvog diranja baze. Baca [Nevaljana]. */
    fun provjeri(tekst: String): Pair<JSONObject, Sazetak> {
        val root = runCatching { JSONObject(tekst) }.getOrNull() ?: throw Nevaljana("JSON")
        val clanovi = root.opt("clanovi") as? JSONArray ?: throw Nevaljana("clanovi")
        if (root.has("shema_verzija") && int(root, "shema_verzija") !in 1..3) throw Nevaljana("shema_verzija")
        for (t in TABLICE) if (root.has(t) && root.opt(t) !is JSONArray) throw Nevaljana(t)

        val ids = HashSet<Int>(); val uidovi = HashSet<String>()
        for (i in 0 until clanovi.length()) {
            val c = clanovi.opt(i) as? JSONObject ?: throw Nevaljana("clanovi")
            if (c.has("uid") && (uuid(c.opt("uid") as? String ?: "")?.let { uidovi.add(it.lowercase()) } != true)) throw Nevaljana("clanovi.uid")
            val id = int(c, "id")
            if (id == null || id <= 0 || !ids.add(id) || (c.opt("ime") as? String).isNullOrBlank()) throw Nevaljana("clanovi")
        }
        for (t in TABLICE) {
            val redniId = HashSet<Int>(); val redniUid = HashSet<String>()
            for (r in redci(root, t)) {
                val bezClana = t == "audit" || t == "narudzbe"
                if (!bezClana && r.isNull("clan_id")) throw Nevaljana("$t.clan_id")
                if (!r.isNull("uid") && (uuid(r.opt("uid") as? String ?: "")?.let { redniUid.add(it.lowercase()) } != true)) throw Nevaljana("$t.uid")
                if (t == "dokumenti" && r.has("id") && (int(r, "id")?.let { it > 0 && redniId.add(it) } != true)) throw Nevaljana("dokumenti.id")
                if (!r.isNull("clan_id") && int(r, "clan_id") !in ids) throw Nevaljana("$t.clan_id")
                if (t == "terapije") {
                    val v = vremenaIzJson(r)
                    if (v == null || v.any { DoseSchedule.normalize(it) == null } || v.toSet().size != v.size) throw Nevaljana("terapije.vremena")
                    if (!r.isNull("dan_u_tjednu") && int(r, "dan_u_tjednu") !in 1..7) throw Nevaljana("terapije.dan_u_tjednu")
                }
                if (t == "dokumenti" && !r.isNull("sadrzaj_b64") &&
                    runCatching { Base64.decode(r.opt("sadrzaj_b64") as String, Base64.DEFAULT) }.isFailure) throw Nevaljana("dokumenti.sadrzaj_b64")
            }
        }
        val dok = redci(root, "dokumenti")
        return root to Sazetak(clanovi.length(), redci(root, "terapije").size, dok.size,
                               dok.count { !str(it, "sadrzaj_b64").isNullOrEmpty() }, root.opt("izvezeno") as? String)
    }

    /** @param ukljuciDokumente sami nalazi i fotografije lijekova ulaze u kopiju samo uz korisnikov izbor. */
    suspend fun izvezi(db: MojTijekDatabase, datoteke: Datoteke, ukljuciDokumente: Boolean): String {
        val k = db.kopijaDao()
        val clanovi = k.clanovi()
        val cId = clanovi.withIndex().associate { it.value.id to it.index + 1 }
        val dokumenti = k.dokumenti().filter { it.clanId in cId }
        val dId = dokumenti.withIndex().associate { it.value.id to it.index + 1 }
        fun foto(f: java.io.File) = if (ukljuciDokumente && f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null
        fun <T> niz(x: List<T>, f: (T) -> JSONObject) = JSONArray().apply { x.forEach { put(f(it)) } }

        val stavke = k.stavke().groupBy { it.narudzbaId }
        return obj(
            "shema_verzija" to 3, "izvezeno" to ts(System.currentTimeMillis()), "dokumenti_ukljuceni" to b(ukljuciDokumente),
            "clanovi" to niz(clanovi) { c -> obj(
                "id" to cId[c.id], "uid" to c.id, "ime" to c.ime, "datum_rodjenja" to dan(c.datumRodjenja), "oib" to c.oib,
                "spol" to c.spol, "mbo" to c.mbo, "krvna_grupa" to c.krvnaGrupa, "alergije" to c.alergije,
                "kronicne_bolesti" to c.kronicneBolesti, "lijecnik" to c.lijecnik, "hitni_kontakt" to c.hitniKontakt,
                "hitni_telefon" to c.hitniTelefon, "hitna_napomena" to c.hitnaNapomena, "boja" to c.boja, "aktivan" to b(c.aktivan),
                "zadnji_ingest" to ts(c.zadnjiIngest), "tijelo_analiza" to c.tijeloAnaliza, "visina" to c.visina,
                "lijecnik_email" to c.lijecnikEmail, "redoslijed" to c.redoslijed, "kontracepcija" to c.kontracepcija,
                "zadnji_papa_test" to dan(c.zadnjiPapaTest), "zadnji_ginekoloski" to dan(c.zadnjiGinekoloski),
                "prosjecni_ciklus" to c.prosjecniCiklus, "trudna" to b(c.trudna), "termin_porod" to dan(c.terminPorod),
                "stvoren" to ts(c.stvoren)) },
            "terapije" to niz(k.terapije()) { t -> obj(
                "clan_id" to cId[t.clanId], "uid" to t.id, "naziv" to t.naziv, "jacina" to t.jacina, "oblik" to t.oblik, "atc" to t.atc,
                "doza_kom" to t.dozaKom, "puta_dnevno" to t.putaDnevno,
                "vremena" to JSONArray(DoseSchedule.effectiveSlots(t)).toString(), "dan_u_tjednu" to t.danUTjednu,
                "kom_po_kutiji" to t.komPoKutiji, "kolicina" to t.kolicina, "prag_dana" to t.pragDana, "trajni" to b(t.trajni),
                "razlog" to t.razlog, "napomena" to t.napomena, "opis" to t.opis, "aktivna" to b(t.aktivna),
                "auto_rezim" to b(t.autoRezim), "zaliha_set" to b(t.zalihaSet), "datum_pocetka" to dan(t.datumPocetka),
                "datum_kraja" to dan(t.datumKraja), "datum_punjenja" to dan(t.datumPunjenja), "recept_do" to dan(t.receptDo),
                "naruceno_ts" to ts(t.narucenoTs), "zadnji_decay" to ts(t.zadnjiDecay), "stvoren" to ts(t.stvoren),
                "izgled_opis" to t.izgledOpis, "izgled_foto_b64" to foto(datoteke.lijek(t.id))) },
            "uzimanja" to niz(k.uzimanja()) { u -> obj(
                "clan_id" to cId[u.clanId], "terapija_uid" to u.terapijaId, "ts" to ts(u.ts), "datum" to dan(u.datum), "slot" to u.slot,
                "kolicina" to u.kolicina, "korisnik" to u.korisnik, "preskoceno" to b(u.preskoceno)) },
            "mjerenja" to niz(k.mjerenja()) { m -> obj(
                "clan_id" to cId[m.clanId], "tip" to m.tip, "vrijednost" to m.vrijednost, "vrijednost2" to m.vrijednost2,
                "jedinica" to m.jedinica, "ts" to ts(m.ts), "datum" to dan(m.datum), "napomena" to m.napomena, "izvor" to m.izvor) },
            "dogadjaji" to niz(k.dogadjaji()) { e -> obj(
                "clan_id" to cId[e.clanId], "naslov" to e.naslov, "vrsta" to e.vrsta, "datum" to dan(e.datum), "vrijeme" to e.vrijeme,
                "lokacija" to e.lokacija, "priprema" to e.priprema, "uputnica_potrebna" to b(e.uputnicaPotrebna),
                "uputnica_zatrazena" to b(e.uputnicaZatrazena), "uputnica_izdana" to b(e.uputnicaIzdana), "status" to e.status,
                "podsjetnik_dana" to e.podsjetnikDana, "napomena" to e.napomena, "narudzba_obavljena" to b(e.narudzbaObavljena),
                "vozac" to e.vozac, "vozac_email" to e.vozacEmail, "uputnica_zatrazena_ts" to ts(e.uputnicaZatrazenaTs),
                "vozac_obavijesten" to b(e.vozacObavijesten), "ek_event_id" to e.ekEventId, "dokument_uid" to e.dokumentId) },
            "dokumenti" to niz(dokumenti) { d -> obj(
                "id" to dId[d.id], "uid" to d.id, "clan_id" to cId[d.clanId], "vrsta" to d.vrsta, "naziv" to d.naziv, "datum" to dan(d.datum),
                "ustanova" to d.ustanova, "lijecnik" to d.lijecnik, "orig_ime" to d.origIme, "mime" to d.mime, "velicina" to d.velicina,
                "napomena" to d.napomena, "obrada" to d.obrada, "obrada_faza" to d.obradaFaza, "obrada_info" to d.obradaInfo,
                "objasnjenje" to d.objasnjenje, "poslano_doktoru" to b(d.poslanoDoktoru), "poslano_ts" to ts(d.poslanoTs),
                "stvoren" to ts(d.stvoren), "sadrzaj_b64" to foto(datoteke.dokument(d.id))) },
            "lab" to niz(k.lab()) { l -> obj(
                "clan_id" to cId[l.clanId], "naziv" to l.naziv, "kratica" to l.kratica, "vrijednost" to l.vrijednost, "jedinica" to l.jedinica,
                "ref_lo" to l.refLo, "ref_hi" to l.refHi, "status" to l.status, "datum" to dan(l.datum), "dokument_id" to dId[l.dokumentId]) },
            "cijepljenja" to niz(k.cijepljenja()) { v -> obj(
                "clan_id" to cId[v.clanId], "naziv" to v.naziv, "datum" to dan(v.datum), "sljedece" to dan(v.sljedece), "napomena" to v.napomena) },
            "dnevnik" to niz(k.dnevnik()) { n -> obj(
                "clan_id" to cId[n.clanId], "datum" to dan(n.datum), "raspolozenje" to n.raspolozenje, "tekst" to n.tekst,
                "vrsta" to n.vrsta, "naslov" to n.naslov, "jacina" to n.jacina) },
            "ciklusi" to niz(k.ciklusi()) { m -> obj(
                "clan_id" to cId[m.clanId], "datum_pocetka" to dan(m.datumPocetka), "datum_kraja" to dan(m.datumKraja),
                "trajanje_dana" to m.trajanjeDana, "tok" to m.tok, "raspolozenje" to m.raspolozenje, "simptomi" to m.simptomi,
                "napomena" to m.napomena) },
            "narudzbe" to niz(k.narudzbe()) { n -> obj(
                "id" to n.id, "created" to ts(n.created), "status" to n.status, "primatelj_email" to n.primateljEmail,
                "poslao_tko" to n.poslaoTko, "poslano_ts" to ts(n.poslanoTs), "napomena" to n.napomena, "greska" to n.greska,
                "provedeno" to b(n.provedeno), "provedeno_ts" to ts(n.provedenoTs), "provedeno_od" to n.provedenoOd,
                "pixel_token" to n.pixelToken, "otvoreno_ts" to ts(n.otvorenoTs), "potvrdeno_ts" to ts(n.potvrdenoTs),
                "stavke" to niz(stavke[n.id] ?: emptyList()) { s -> obj(
                    "clan_ime" to s.clanIme, "naziv" to s.naziv, "jacina" to s.jacina, "kolicina_kutija" to s.kolicinaKutija,
                    "dana_preostalo" to s.danaPreostalo, "terapija_ref" to s.terapijaRef) }) },
            "audit" to niz(k.audit()) { a -> obj(
                "ts" to ts(a.ts), "akcija" to a.akcija, "objekt" to a.objekt, "clan_id_uuid" to a.clanId, "detalj" to a.detalj) },
        ).toString(2)
    }

    /**
     * Vraćanje kopije ZAMJENJUJE sve podatke. Kopija se prvo provjeri, datoteke se zapišu sa strane,
     * baza se mijenja u jednoj transakciji, i tek tada nove datoteke zamijene stare.
     */
    suspend fun uvezi(db: MojTijekDatabase, datoteke: Datoteke, tekst: String) {
        val root = provjeri(tekst).first
        val sada = System.currentTimeMillis()
        val clanId = HashMap<Int, String>(); val dokId = HashMap<Int, String>()
        val dokSadrzaj = HashMap<String, ByteArray>(); val lijekSadrzaj = HashMap<String, ByteArray>()
        fun novi() = UUID.randomUUID().toString()
        fun bajtovi(o: JSONObject, kljuc: String) = str(o, kljuc)?.let { runCatching { Base64.decode(it, Base64.DEFAULT) }.getOrNull() }

        val clanovi = redci(root, "clanovi").map { c ->
            ClanEntity(
                id = str(c, "uid") ?: novi(), ime = str(c, "ime") ?: "", datumRodjenja = datum(c, "datum_rodjenja"), oib = str(c, "oib"),
                spol = str(c, "spol"), mbo = str(c, "mbo"), krvnaGrupa = str(c, "krvna_grupa"), alergije = str(c, "alergije"),
                kronicneBolesti = str(c, "kronicne_bolesti"), lijecnik = str(c, "lijecnik"), hitniKontakt = str(c, "hitni_kontakt"),
                hitniTelefon = str(c, "hitni_telefon"), hitnaNapomena = str(c, "hitna_napomena"), boja = str(c, "boja") ?: "#0891b2",
                aktivan = bool(c, "aktivan", true), zadnjiIngest = datum(c, "zadnji_ingest"), tijeloAnaliza = str(c, "tijelo_analiza"),
                visina = dbl(c, "visina"), lijecnikEmail = str(c, "lijecnik_email"), redoslijed = int(c, "redoslijed") ?: 0,
                kontracepcija = str(c, "kontracepcija"), zadnjiPapaTest = datum(c, "zadnji_papa_test"),
                zadnjiGinekoloski = datum(c, "zadnji_ginekoloski"), prosjecniCiklus = int(c, "prosjecni_ciklus"),
                trudna = bool(c, "trudna", false), terminPorod = datum(c, "termin_porod"), stvoren = datum(c, "stvoren") ?: sada
            ).also { k -> int(c, "id")?.let { clanId[it] = k.id } }
        }
        fun clan(o: JSONObject) = int(o, "clan_id")?.let { clanId[it] }

        val dokumenti = redci(root, "dokumenti").mapNotNull { d ->
            val bytes = bajtovi(d, "sadrzaj_b64")
            DokumentEntity(
                id = str(d, "uid") ?: novi(), clanId = clan(d) ?: return@mapNotNull null, vrsta = str(d, "vrsta") ?: "nalaz",
                naziv = str(d, "naziv"), datum = datum(d, "datum"), ustanova = str(d, "ustanova"), lijecnik = str(d, "lijecnik"),
                origIme = str(d, "orig_ime"), mime = str(d, "mime"), velicina = int(d, "velicina") ?: bytes?.size,
                napomena = str(d, "napomena"), obrada = str(d, "obrada"), obradaFaza = str(d, "obrada_faza"),
                obradaInfo = str(d, "obrada_info"), objasnjenje = str(d, "objasnjenje"),
                poslanoDoktoru = bool(d, "poslano_doktoru", false), poslanoTs = datum(d, "poslano_ts"), stvoren = datum(d, "stvoren") ?: sada
            ).also { e -> int(d, "id")?.let { dokId[it] = e.id }; bytes?.let { dokSadrzaj[e.id] = it } }
        }

        val terapije = redci(root, "terapije").mapNotNull { t ->
            TerapijaEntity(
                id = str(t, "uid") ?: novi(), clanId = clan(t) ?: return@mapNotNull null, naziv = str(t, "naziv") ?: "",
                jacina = str(t, "jacina"), oblik = str(t, "oblik") ?: "tableta", atc = str(t, "atc"), dozaKom = dbl(t, "doza_kom") ?: 1.0,
                putaDnevno = int(t, "puta_dnevno") ?: 1, vremena = (vremenaIzJson(t) ?: emptyList()).joinToString(","),
                danUTjednu = int(t, "dan_u_tjednu"), komPoKutiji = dbl(t, "kom_po_kutiji") ?: 30.0, kolicina = dbl(t, "kolicina") ?: 0.0,
                pragDana = int(t, "prag_dana"), trajni = bool(t, "trajni", true), razlog = str(t, "razlog"), napomena = str(t, "napomena"),
                opis = str(t, "opis"), aktivna = bool(t, "aktivna", true), autoRezim = bool(t, "auto_rezim", true),
                zalihaSet = bool(t, "zaliha_set", false), datumPocetka = datum(t, "datum_pocetka"), datumKraja = datum(t, "datum_kraja"),
                datumPunjenja = datum(t, "datum_punjenja"), receptDo = datum(t, "recept_do"), narucenoTs = datum(t, "naruceno_ts"),
                zadnjiDecay = datum(t, "zadnji_decay"), stvoren = datum(t, "stvoren") ?: sada, izgledOpis = str(t, "izgled_opis")
            ).also { e -> bajtovi(t, "izgled_foto_b64")?.let { lijekSadrzaj[e.id] = it } }
        }
        val terapijaPoId = terapije.associateBy { it.id }

        // Zapis bez poznate terapije se preskače — „visjeće” uzimanje kvarilo bi izračun adherencije.
        val uzimanja = redci(root, "uzimanja").mapNotNull { u ->
            val t = terapijaPoId[str(u, "terapija_uid")] ?: return@mapNotNull null
            UzimanjeEntity(terapijaId = t.id, clanId = t.clanId, ts = datum(u, "ts") ?: sada, datum = datum(u, "datum"), slot = str(u, "slot"),
                           kolicina = dbl(u, "kolicina") ?: t.dozaKom, korisnik = str(u, "korisnik"), preskoceno = bool(u, "preskoceno", false))
        }
        val mjerenja = redci(root, "mjerenja").mapNotNull { m ->
            MjerenjeEntity(clanId = clan(m) ?: return@mapNotNull null, tip = str(m, "tip") ?: "", vrijednost = dbl(m, "vrijednost") ?: 0.0,
                           vrijednost2 = dbl(m, "vrijednost2"), jedinica = str(m, "jedinica"), ts = datum(m, "ts") ?: sada,
                           datum = datum(m, "datum"), napomena = str(m, "napomena"), izvor = str(m, "izvor") ?: "rucno")
        }
        val lab = redci(root, "lab").mapNotNull { l ->
            LabNalazEntity(clanId = clan(l) ?: return@mapNotNull null, dokumentId = int(l, "dokument_id")?.let { dokId[it] },
                           datum = datum(l, "datum"), naziv = str(l, "naziv") ?: "", kratica = str(l, "kratica"),
                           vrijednost = dbl(l, "vrijednost"), jedinica = str(l, "jedinica"), refLo = dbl(l, "ref_lo"),
                           refHi = dbl(l, "ref_hi"), status = str(l, "status"))
        }
        val dogadjaji = redci(root, "dogadjaji").mapNotNull { e ->
            DogadjajEntity(clanId = clan(e) ?: return@mapNotNull null, vrsta = str(e, "vrsta") ?: "pregled", naslov = str(e, "naslov") ?: "",
                           datum = datum(e, "datum") ?: sada, vrijeme = str(e, "vrijeme"), lokacija = str(e, "lokacija"),
                           priprema = str(e, "priprema"), uputnicaPotrebna = bool(e, "uputnica_potrebna", false),
                           uputnicaZatrazena = bool(e, "uputnica_zatrazena", false), uputnicaIzdana = bool(e, "uputnica_izdana", false),
                           narudzbaObavljena = bool(e, "narudzba_obavljena", false), status = str(e, "status") ?: "planirano",
                           podsjetnikDana = int(e, "podsjetnik_dana") ?: 3, napomena = str(e, "napomena"), vozac = str(e, "vozac"),
                           vozacEmail = str(e, "vozac_email"), vozacObavijesten = bool(e, "vozac_obavijesten", false),
                           uputnicaZatrazenaTs = datum(e, "uputnica_zatrazena_ts"), dokumentId = str(e, "dokument_uid"),
                           ekEventId = str(e, "ek_event_id"))
        }
        val cijepljenja = redci(root, "cijepljenja").mapNotNull { v ->
            CijepljenjeEntity(clanId = clan(v) ?: return@mapNotNull null, naziv = str(v, "naziv") ?: "", datum = datum(v, "datum"),
                              sljedece = datum(v, "sljedece"), napomena = str(v, "napomena"))
        }
        val dnevnik = redci(root, "dnevnik").mapNotNull { n ->
            DnevnikUnosEntity(clanId = clan(n) ?: return@mapNotNull null, datum = datum(n, "datum"), vrsta = str(n, "vrsta") ?: "biljeska",
                              naslov = str(n, "naslov"), raspolozenje = int(n, "raspolozenje"), jacina = int(n, "jacina"), tekst = str(n, "tekst"))
        }
        val ciklusi = redci(root, "ciklusi").mapNotNull { m ->
            MenstruacijaEntity(clanId = clan(m) ?: return@mapNotNull null, datumPocetka = datum(m, "datum_pocetka") ?: sada,
                               datumKraja = datum(m, "datum_kraja"), trajanjeDana = int(m, "trajanje_dana"), tok = str(m, "tok"),
                               raspolozenje = int(m, "raspolozenje"), simptomi = str(m, "simptomi"), napomena = str(m, "napomena"))
        }
        val stavke = ArrayList<NarudzbaStavkaEntity>()
        val narudzbe = redci(root, "narudzbe").map { o ->
            NarudzbaEntity(id = str(o, "id") ?: novi(), created = datum(o, "created") ?: sada, status = str(o, "status") ?: "nacrt",
                           primateljEmail = str(o, "primatelj_email"), poslaoTko = str(o, "poslao_tko"), poslanoTs = datum(o, "poslano_ts"),
                           napomena = str(o, "napomena"), greska = str(o, "greska"), provedeno = bool(o, "provedeno", false),
                           provedenoTs = datum(o, "provedeno_ts"), provedenoOd = str(o, "provedeno_od"), pixelToken = str(o, "pixel_token"),
                           otvorenoTs = datum(o, "otvoreno_ts"), potvrdenoTs = datum(o, "potvrdeno_ts")
            ).also { n -> redci(o, "stavke").forEach { s ->
                stavke += NarudzbaStavkaEntity(narudzbaId = n.id, clanIme = str(s, "clan_ime"), naziv = str(s, "naziv") ?: "",
                                               jacina = str(s, "jacina"), kolicinaKutija = int(s, "kolicina_kutija") ?: 1,
                                               danaPreostalo = dbl(s, "dana_preostalo"), terapijaRef = str(s, "terapija_ref")) } }
        }
        val audit = redci(root, "audit").map { a ->
            AuditEntity(ts = datum(a, "ts") ?: sada, akcija = str(a, "akcija") ?: "", objekt = str(a, "objekt") ?: "",
                        clanId = str(a, "clan_id_uuid"), detalj = str(a, "detalj"))
        }

        datoteke.pripremi(dokSadrzaj, lijekSadrzaj)
        db.withTransaction {
            val k = db.kopijaDao()
            k.obrisiClanove(); k.obrisiNarudzbe()
            // Povijest promjena se briše samo ako je kopija donosi; starija kopija je nema čime zamijeniti.
            if (root.has("audit")) k.obrisiAudit()
            k.dodajClanove(clanovi); k.dodajDokumente(dokumenti); k.dodajTerapije(terapije); k.dodajUzimanja(uzimanja)
            k.dodajMjerenja(mjerenja); k.dodajLab(lab); k.dodajDogadjaje(dogadjaji); k.dodajCijepljenja(cijepljenja)
            k.dodajDnevnik(dnevnik); k.dodajCikluse(ciklusi); k.dodajNarudzbe(narudzbe); k.dodajStavke(stavke); k.dodajAudit(audit)
        }
        datoteke.preuzmiPripremljeno()
    }
}
