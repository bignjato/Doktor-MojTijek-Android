package com.mojtijek.doktor.data

/** Agregacija mjerenja za grafove (iOS `VitalDetailView`). */
object Pracenje {
    /** Jedan dan sveden na jedan broj; `vrijednost2` je dijastolički tlak. */
    data class Dan(val dan: Long, val vrijednost: Double, val vrijednost2: Double?)

    /** Koraci: najveće očitanje dana (brojač kroz dan raste). Ostalo: prosjek dana. */
    fun dnevneVrijednosti(tip: String, mjerenja: List<MjerenjeEntity>): List<Dan> =
        mjerenja.groupBy { DoseSchedule.dayStart(it.ts) }.map { (dan, ms) ->
            val v = ms.map { it.vrijednost }
            val d = ms.mapNotNull { it.vrijednost2 }
            Dan(dan, if (tip == "koraci") v.max() else v.average(), if (d.isEmpty()) null else d.average())
        }.sortedBy { it.dan }

    /** Koraci: zbroj dnevnih zbrojeva. Ostalo: prosjek dnevnih prosjeka. */
    fun agregat(tip: String, dani: List<Dan>): Double {
        if (dani.isEmpty()) return 0.0
        val v = dani.map { it.vrijednost }
        return if (tip == "koraci") v.sum() else v.average()
    }
}
