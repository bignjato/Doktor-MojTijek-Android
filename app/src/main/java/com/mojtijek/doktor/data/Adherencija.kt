package com.mojtijek.doktor.data

import java.util.Calendar
import kotlin.math.roundToInt

/** Pridržavanje terapije (iOS `Adherencija`): tjedna terapija se očekuje samo svoj dan, preskočena doza nije uzeta. */
object Adherencija {
    private fun danUnatrag(sada: Long, i: Int): Long =
        Calendar.getInstance().apply { timeInMillis = DoseSchedule.dayStart(sada); add(Calendar.DAY_OF_YEAR, -i) }.timeInMillis

    private fun uzetoPoDanu(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>): Map<Long, Int> {
        val aktivne = terapije.filter { it.aktivna }.map { it.id }.toSet()
        return uzimanja.filter { !it.preskoceno && it.terapijaId in aktivne }.groupingBy { DoseSchedule.dayStart(it.datum ?: it.ts) }.eachCount()
    }

    fun imaPlan(terapije: List<TerapijaEntity>): Boolean = terapije.any { it.aktivna }

    /** Postotak po danu, od danas unatrag. Dan bez planirane doze broji se kao ispunjen. */
    fun poDanu(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long, dana: Int): List<Double> {
        val uzeto = uzetoPoDanu(terapije, uzimanja)
        return (0 until dana).map { i ->
            val dan = danUnatrag(sada, i)
            val plan = DoseSchedule.entriesForDay(terapije, dan).size
            if (plan > 0) minOf((uzeto[dan] ?: 0) * 100.0 / plan, 100.0) else 100.0
        }
    }

    /** Niz uzastopnih punih dana od JUČER unatrag (današnji dan može biti nepotpun). */
    fun niz(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long): Int {
        if (!imaPlan(terapije)) return 0
        val uzeto = uzetoPoDanu(terapije, uzimanja)
        var n = 0
        for (i in 1..400) {
            val dan = danUnatrag(sada, i)
            val plan = DoseSchedule.entriesForDay(terapije, dan).size
            if (plan == 0 || (uzeto[dan] ?: 0) >= plan) n++ else break
        }
        return n
    }

    fun prosjek(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long, dana: Int): Int =
        poDanu(terapije, uzimanja, sada, dana).average().roundToInt()
}
