package com.mojtijek.doktor.data

import java.io.File

/** Datoteke uz zapise u bazi (privatni prostor aplikacije): sami dokumenti i fotografije lijekova. */
class Datoteke(private val korijen: File) {
    private fun mapa(ime: String) = File(korijen, ime).apply { mkdirs() }
    fun dokument(id: String) = File(mapa("dokumenti"), id)
    fun lijek(terapijaId: String) = File(mapa("lijekovi"), "$terapijaId.jpg")
    fun obrisiSve() { File(korijen, "dokumenti").deleteRecursively(); File(korijen, "lijekovi").deleteRecursively() }
}
