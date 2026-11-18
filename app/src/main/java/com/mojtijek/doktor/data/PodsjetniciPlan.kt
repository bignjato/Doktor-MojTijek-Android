package com.mojtijek.doktor.data

import java.util.Calendar

/**
 * Što i kada treba podsjetiti (iOS `NotifManager.osvjeziPodsjetnike`). Čista funkcija: zakazivanje radi `Podsjetnici`.
 * Razlika prema iOS-u: doza koja je već uzeta ili preskočena ne dobiva podsjetnik.
 */
object PodsjetniciPlan {
    /** Vrsta: "doza", "recept", "narudzba", "cjepivo", "pregled". */
    class Stavka(val id: String, val kada: Long, val vrsta: String, val clan: ClanEntity, val terapija: TerapijaEntity? = null,
                 val slot: String? = null, val dogadjaj: DogadjajEntity? = null, val cijepljenje: CijepljenjeEntity? = null)

    /** Doze se planiraju tjedan unaprijed (tjedna terapija!); svaki okinuti podsjetnik i svako otvaranje aplikacije plan obnavlja. */
    const val DANA_UNAPRIJED = 7
    const val NAJVISE = 60

    private fun pomak(ms: Long, dana: Int): Long = Calendar.getInstance().apply { timeInMillis = ms; add(Calendar.DAY_OF_YEAR, dana) }.timeInMillis
    private fun uDevet(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    /** Tjedan dana prije roka, ili na sam rok ako je bliže; u 9 h. */
    private fun tjedanPrije(rok: Long, sada: Long): Long = uDevet(pomak(rok, -7).takeIf { it > sada } ?: rok)

    fun plan(clanovi: List<ClanEntity>, terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, dogadjaji: List<DogadjajEntity>,
             cijepljenja: List<CijepljenjeEntity>, sada: Long): List<Stavka> {
        // Neaktivan član ne dobiva podsjetnike.
        val clan = clanovi.filter { it.aktivan }.associateBy { it.id }
        val aktivne = terapije.filter { it.aktivna && it.clanId in clan }
        val out = ArrayList<Stavka>()

        for (d in 0..DANA_UNAPRIJED) {
            for (doza in DoseSchedule.entriesForDay(aktivne, pomak(sada, d))) {
                if (doza.plannedMillis <= sada || DoseSchedule.zapis(doza, uzimanja) != null) continue
                out += Stavka("doza-${doza.terapija.id}-${doza.plannedMillis}", doza.plannedMillis, "doza", clan.getValue(doza.terapija.clanId),
                              terapija = doza.terapija, slot = doza.slot)
            }
        }
        for (t in aktivne) {
            val c = clan.getValue(t.clanId)
            t.receptDo?.takeIf { it > sada }?.let { out += Stavka("recept-${t.id}", tjedanPrije(it, sada), "recept", c, terapija = t) }
            if (DoseSchedule.trebaNaruciti(t)) {
                val danas = uDevet(sada)
                out += Stavka("narudzba-${t.id}", if (danas > sada) danas else uDevet(pomak(sada, 1)), "narudzba", c, terapija = t)
            }
        }
        for (cj in cijepljenja) {
            val c = clan[cj.clanId] ?: continue
            cj.sljedece?.takeIf { it > sada }?.let { out += Stavka("cjepivo-${cj.id}", tjedanPrije(it, sada), "cjepivo", c, cijepljenje = cj) }
        }
        for (ev in dogadjaji) {
            val c = clan[ev.clanId] ?: continue
            if (ev.status == "obavljeno" || ev.status == "otkazano" || ev.datum <= sada) continue
            var ciljni = pomak(ev.datum, -maxOf(0, ev.podsjetnikDana))
            // Ako je taj dan već prošao (termin je bliži od zadanog broja dana), javi sutra — ali nikad nakon samog termina.
            if (ciljni <= sada) ciljni = pomak(sada, 1)
            if (ciljni > ev.datum) continue
            out += Stavka("pregled-${ev.id}", uDevet(ciljni), "pregled", c, dogadjaj = ev)
        }
        return out.filter { it.kada > sada }.sortedBy { it.kada }.take(NAJVISE)
    }
}
