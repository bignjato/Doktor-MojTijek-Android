package com.mojtijek.doktor.data

import java.util.Calendar
import java.util.Locale

/** Ekvivalent iOS DoseSchedule.swift — raspored doza terapije po danima. */
object DoseSchedule {

    fun normalize(value: String): String? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun parse(text: String): List<String>? {
        if (text.isBlank()) return emptyList()
        val parts = text.split(",")
        val values = parts.mapNotNull { normalize(it) }
        if (values.size != parts.size || values.toSet().size != values.size) return null
        return values.sorted()
    }

    /** Vraća true ako se terapija uzima na dani dan (poštuje danUTjednu za tjedne terapije). */
    fun uzimaSe(terapija: TerapijaEntity, dan: Calendar): Boolean {
        if (!terapija.aktivna) return false
        terapija.datumPocetka?.let { start ->
            if (dan.timeInMillis < startOfDay(start)) return false
        }
        terapija.datumKraja?.let { end ->
            if (dan.timeInMillis > endOfDay(end)) return false
        }
        terapija.danUTjednu?.let { weekday ->
            // Calendar.DAY_OF_WEEK: 1=nedjelja..7=subota (isto kao iOS)
            return dan.get(Calendar.DAY_OF_WEEK) == weekday
        }
        return true
    }

    /** Bez upisanih vremena vrijede zadana prema broju doza dnevno (kao iOS). */
    fun effectiveSlots(terapija: TerapijaEntity): List<String> =
        parse(terapija.vremena)?.takeIf { it.isNotEmpty() } ?: zadanaVremena(terapija.putaDnevno)

    fun zadanaVremena(putaDnevno: Int): List<String> = when (maxOf(1, putaDnevno)) {
        1 -> listOf("08:00")
        2 -> listOf("08:00", "20:00")
        3 -> listOf("08:00", "14:00", "20:00")
        4 -> listOf("08:00", "12:00", "16:00", "20:00")
        else -> { val n = minOf(putaDnevno, 6); (0 until n).map { String.format(Locale.US, "%02d:00", 8 + it * (12 / maxOf(1, n - 1))) } }
    }

    private fun startOfDay(ts: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun endOfDay(ts: Long): Long {
        val c = Calendar.getInstance(); c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    fun dayStart(ts: Long = System.currentTimeMillis()): Long = startOfDay(ts)

    /** Planirana doza za jedan dan/slot. */
    data class ScheduledDose(
        val terapija: TerapijaEntity,
        val slot: String,
        val plannedMillis: Long
    )

    fun entriesForDay(terapije: List<TerapijaEntity>, danMillis: Long): List<ScheduledDose> {
        val cal = Calendar.getInstance(); cal.timeInMillis = danMillis
        val doses = mutableListOf<ScheduledDose>()
        for (t in terapije) {
            if (!uzimaSe(t, cal)) continue
            for (slot in effectiveSlots(t)) {
                val parts = slot.split(":")
                val h = parts[0].toInt(); val m = parts[1].toInt()
                val dose = Calendar.getInstance(); dose.timeInMillis = danMillis
                dose.set(Calendar.HOUR_OF_DAY, h); dose.set(Calendar.MINUTE, m); dose.set(Calendar.SECOND, 0); dose.set(Calendar.MILLISECOND, 0)
                doses.add(ScheduledDose(t, slot, dose.timeInMillis))
            }
        }
        return doses.sortedWith(compareBy({ it.plannedMillis }, { it.terapija.naziv }))
    }

    /** Zapis (uzeto ili preskočeno) za dozu, ako postoji. */
    fun zapis(doza: ScheduledDose, uzimanja: List<UzimanjeEntity>): UzimanjeEntity? {
        val dan = dayStart(doza.plannedMillis)
        return uzimanja.firstOrNull { it.terapijaId == doza.terapija.id && it.slot == doza.slot && dayStart(it.datum ?: it.ts) == dan }
    }

    /** Početni ekran: sve nepotvrđene današnje doze; kad ih nema, prva sljedeća u idućih tjedan dana. */
    fun homeDoses(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long): List<ScheduledDose> {
        for (pomak in 0..7) {
            val dan = Calendar.getInstance().apply { timeInMillis = sada; add(Calendar.DAY_OF_YEAR, pomak) }.timeInMillis
            val otvorene = entriesForDay(terapije, dan).filter { zapis(it, uzimanja) == null }
            if (otvorene.isNotEmpty()) return if (pomak == 0) otvorene else otvorene.take(1)
        }
        return emptyList()
    }

    /** „Uzeto sve” nikad ne uključuje buduće doze ni doze drugog dana. */
    fun dueToday(terapije: List<TerapijaEntity>, uzimanja: List<UzimanjeEntity>, sada: Long): List<ScheduledDose> =
        entriesForDay(terapije, sada).filter { zapis(it, uzimanja) == null && it.plannedMillis <= sada }

    /** Pri kraju je (zaliha ≤ prag, zadano 3 dana) i nije već označeno kao naručeno. */
    fun trebaNaruciti(t: TerapijaEntity): Boolean {
        val dana = danaPreostalo(t) ?: return false
        return t.narucenoTs == null && dana <= (t.pragDana ?: 3)
    }

    /** Broj dana zaliha preostalo, po iOS logici: kolicina / (dozaKom * putaDnevno po tjednu). */
    fun danaPreostalo(terapija: TerapijaEntity): Double? {
        if (!terapija.zalihaSet) return null
        val slotsPerDay = if (terapija.danUTjednu != null) {
            terapija.effectiveSlotsCount() / 7.0
        } else {
            terapija.effectiveSlotsCount().toDouble()
        }
        val dailyUse = slotsPerDay * terapija.dozaKom
        if (dailyUse <= 0) return null
        return terapija.kolicina / dailyUse
    }

    private fun TerapijaEntity.effectiveSlotsCount(): Int = effectiveSlots(this).size.coerceAtLeast(1)
}
