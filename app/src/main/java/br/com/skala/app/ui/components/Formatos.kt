package br.com.skala.app.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PtBr: Locale = Locale("pt", "BR")

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.texto(): String = format(formatoHora)

fun String.primeiraMaiuscula(): String = replaceFirstChar { it.titlecase(PtBr) }

/** "Sábado, 7 de novembro" */
fun LocalDate.textoLongo(): String =
    "${dayOfWeek.getDisplayName(TextStyle.FULL, PtBr).primeiraMaiuscula().substringBefore("-")}, " +
        "$dayOfMonth de ${month.getDisplayName(TextStyle.FULL, PtBr)}"

/** "Sáb" */
fun LocalDate.diaSemanaCurto(): String =
    dayOfWeek.getDisplayName(TextStyle.SHORT, PtBr).replace(".", "").primeiraMaiuscula()

/** "NOV" */
fun LocalDate.mesCurto(): String = month.getDisplayName(TextStyle.SHORT, PtBr).replace(".", "").uppercase(PtBr)

/** "Novembro 2026" */
fun LocalDate.mesAno(): String = "${month.getDisplayName(TextStyle.FULL, PtBr).primeiraMaiuscula()} $year"
