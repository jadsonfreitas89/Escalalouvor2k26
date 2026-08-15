package br.com.jadson.escalalouvor2k26.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit

object CultoUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Retorna o título do culto baseado na data.
     * Regras:
     * TERÇA: Culto da Família
     * SEXTA: Cura e Libertação
     * DOMINGO: 1º Ceia, 2º Oferta, 3º+ Louvor e Adoração
     */
    fun getTituloCulto(dataString: String): String {
        val date = try {
            LocalDate.parse(dataString, dateFormatter)
        } catch (e: Exception) {
            return "Culto"
        }

        return when (date.dayOfWeek) {
            DayOfWeek.TUESDAY -> "Culto da Família"
            DayOfWeek.FRIDAY -> "Cura e Libertação"
            DayOfWeek.SUNDAY -> {
                val weekOfMonth = getNthSundayOfMonth(date)
                when (weekOfMonth) {
                    1 -> "CEIA"
                    2 -> "Oferta"
                    else -> "Louvor e Adoração"
                }
            }
            else -> "Culto"
        }
    }

    /**
     * Calcula qual domingo do mês a data representa (1º, 2º, 3º...).
     */
    private fun getNthSundayOfMonth(date: LocalDate): Int {
        var count = 0
        var current = date.with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
        while (!current.isAfter(date)) {
            count++
            if (current == date) break
            current = current.plusWeeks(1)
        }
        return count
    }

    /**
     * Retorna o prazo limite para cadastro de louvores.
     * SEXTA: até Terça da mesma semana.
     * TERÇA: até Quarta da semana anterior.
     * DOMINGO: até Sexta da mesma semana.
     */
    fun getPrazoLouvores(dataString: String): LocalDate? {
        val date = try {
            LocalDate.parse(dataString, dateFormatter)
        } catch (e: Exception) {
            return null
        }

        return when (date.dayOfWeek) {
            DayOfWeek.FRIDAY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY))
            DayOfWeek.TUESDAY -> date.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY))
            DayOfWeek.SUNDAY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
            else -> null
        }
    }
    
    fun isToday(dataString: String): Boolean {
        return try {
            val date = LocalDate.parse(dataString, dateFormatter)
            date.isEqual(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }
}
