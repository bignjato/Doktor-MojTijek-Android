package com.mojtijek.doktor.data

import java.io.File

/** Datoteke uz zapise u bazi (privatni prostor aplikacije): sami dokumenti i fotografije lijekova. */
class Datoteke(private val korijen: File) {
    private fun mapa(ime: String) = File(korijen, ime).apply { mkdirs() }
    fun dokument(id: String) = File(mapa("dokumenti"), id)
    fun lijek(terapijaId: String) = File(mapa("lijekovi"), "$terapijaId.jpg")

    private val novo = File(korijen, "novo")
    /** Datoteke iz kopije prvo se zapišu sa strane: ako nema mjesta, pada PRIJE nego je baza dirana. */
    fun pripremi(dokumenti: Map<String, ByteArray>, lijekovi: Map<String, ByteArray>) {
        novo.deleteRecursively()
        val n = Datoteke(novo)
        dokumenti.forEach { (id, bajtovi) -> n.dokument(id).writeBytes(bajtovi) }
        lijekovi.forEach { (id, bajtovi) -> n.lijek(id).writeBytes(bajtovi) }
    }
    /** Zamijeni postojeće datoteke pripremljenima (preimenovanje mape, ne kopiranje). */
    fun preuzmiPripremljeno() {
        obrisiSve()
        for (ime in listOf("dokumenti", "lijekovi")) File(novo, ime).takeIf { it.exists() }?.renameTo(File(korijen, ime))
        novo.deleteRecursively()
    }

    fun obrisiSve() { File(korijen, "dokumenti").deleteRecursively(); File(korijen, "lijekovi").deleteRecursively() }
}
