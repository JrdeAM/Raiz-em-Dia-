package br.com.skala.app.data

import java.time.LocalDate
import java.time.LocalTime

data class Servico(
    val id: String,
    val nome: String,
    val duracaoMin: Int,
    val categoria: String,
    /** Serviços de cabelo pedem o formulário "Sobre seu cabelo" e têm valor estimado por ele. */
    val dependeDoCabelo: Boolean,
) {
    val duracaoTexto: String get() = textoDuracao(duracaoMin)
}

fun textoDuracao(minutos: Int): String = when {
    minutos < 60 -> "$minutos min"
    minutos % 60 == 0 -> "${minutos / 60}h"
    else -> "${minutos / 60}h${minutos % 60}"
}

/** Valor aproximado de um serviço (ou da soma deles). O valor final é confirmado no salão. */
data class FaixaPreco(val minimo: Int, val maximo: Int) {
    operator fun plus(outra: FaixaPreco) = FaixaPreco(minimo + outra.minimo, maximo + outra.maximo)
    val texto: String get() = if (minimo == maximo) "R$ $minimo" else "R$ $minimo–$maximo"
}

/** Resumo de vários serviços marcados para o mesmo horário. */
val List<Servico>.titulo: String get() = joinToString(" + ") { it.nome }
val List<Servico>.duracaoTotalMin: Int get() = sumOf { it.duracaoMin }
val List<Servico>.temServicoDeCabelo: Boolean get() = any { it.dependeDoCabelo }

enum class Comprimento(val rotulo: String, val ajuda: String) {
    CURTO("Curto", "Acima do ombro"),
    MEDIO("Médio", "Até o ombro"),
    LONGO("Longo", "Abaixo do ombro"),
    EXTRA_LONGO("Extra longo", "Na cintura"),
}

enum class TipoCabelo(val rotulo: String) { LISO("Liso"), ONDULADO("Ondulado"), CACHEADO("Cacheado"), CRESPO("Crespo") }

enum class Volume(val rotulo: String) { FINO("Fino"), MEDIO("Médio"), GROSSO("Grosso") }

/** Respostas do formulário "Sobre seu cabelo". Comprimento e tipo são obrigatórios. */
data class DadosCabelo(
    val comprimento: Comprimento? = null,
    val tipo: TipoCabelo? = null,
    val volume: Volume? = null,
    val fezQuimica: Boolean? = null,
) {
    val completo: Boolean get() = comprimento != null && tipo != null
    val resumo: String get() = listOfNotNull(comprimento?.rotulo, tipo?.rotulo).joinToString(" · ")
}

/** Uma faixa em que o salão atende, ex.: 09:00 às 12:00. */
data class Faixa(val inicio: LocalTime, val fim: LocalTime)

/** Horário que a dona fechou dentro de uma faixa (compromisso, almoço etc.). */
data class Bloqueio(val inicio: LocalTime, val fim: LocalTime, val motivo: String)

enum class StatusDia { ABERTO, FECHADO, A_DEFINIR }

/**
 * Como o salão funciona num dia específico. Não há horário fixo: a dona define cada dia
 * (aberto ou fechado, faixas de atendimento, intervalo entre clientes e bloqueios).
 */
data class DiaFuncionamento(
    val data: LocalDate,
    val status: StatusDia = StatusDia.A_DEFINIR,
    val faixas: List<Faixa> = emptyList(),
    val intervaloMin: Int = 15,
    val bloqueios: List<Bloqueio> = emptyList(),
)

enum class StatusAgendamento(val rotulo: String) {
    CONFIRMADO("CONFIRMADO"), CONCLUIDO("CONCLUÍDO"), CANCELADO("CANCELADO")
}

data class Agendamento(
    val id: String,
    val clienteNome: String,
    val clienteEmail: String,
    /** Um ou mais serviços feitos em sequência no mesmo horário. */
    val servicos: List<Servico>,
    val data: LocalDate,
    val inicio: LocalTime,
    val cabelo: DadosCabelo? = null,
    val observacao: String = "",
    val status: StatusAgendamento = StatusAgendamento.CONFIRMADO,
) {
    val titulo: String get() = servicos.titulo
    val duracaoMin: Int get() = servicos.duracaoTotalMin
    val duracaoTexto: String get() = textoDuracao(duracaoMin)
    val fim: LocalTime get() = inicio.plusMinutes(duracaoMin.toLong())
}
