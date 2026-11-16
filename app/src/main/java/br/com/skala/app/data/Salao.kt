package br.com.skala.app.data

import java.time.LocalDate
import java.time.LocalTime

data class Servico(
    val id: String,
    val nome: String,
    /** Duração padrão. Nos serviços de cabelo é usada até a cliente responder o formulário. */
    val duracaoMin: Int,
    val categoria: String,
    /** Serviços de cabelo pedem o formulário "Sobre seu cabelo": o tempo depende das respostas. */
    val dependeDoCabelo: Boolean,
    /** Tempo de cada comprimento/tipo de cabelo. Null nos serviços que não dependem do cabelo. */
    val tempos: TemposCabelo? = null,
) {
    val duracaoTexto: String get() = textoDuracao(duracaoMin)

    /** Duração estimada para este cabelo; sem comprimento informado, usa a duração padrão. */
    fun duracaoPara(cabelo: DadosCabelo?): Int {
        val t = tempos ?: return duracaoMin
        val comprimento = cabelo?.comprimento ?: return duracaoMin
        val base = t.porComprimento[comprimento] ?: duracaoMin
        val extraTipo = cabelo.tipo?.let { t.extraPorTipo[it] } ?: 0
        return base + extraTipo
    }

    /** Menor e maior tempo possíveis, para mostrar antes do formulário (ex.: "40 min a 2h25"). */
    val duracaoMinima: Int get() = tempos?.porComprimento?.values?.minOrNull() ?: duracaoMin
    val duracaoMaxima: Int
        get() = tempos?.let { t ->
            (t.porComprimento.values.maxOrNull() ?: duracaoMin) + (t.extraPorTipo.values.maxOrNull() ?: 0)
        } ?: duracaoMin

    val faixaDuracaoTexto: String
        get() = if (duracaoMinima == duracaoMaxima) textoDuracao(duracaoMin)
        else "${textoDuracao(duracaoMinima)} a ${textoDuracao(duracaoMaxima)}"
}

/**
 * Quanto tempo um serviço de cabelo leva: minutos para cada comprimento, mais um acréscimo
 * conforme o tipo (cacheado e crespo costumam levar mais).
 */
data class TemposCabelo(
    val porComprimento: Map<Comprimento, Int>,
    val extraPorTipo: Map<TipoCabelo, Int> = emptyMap(),
)

fun textoDuracao(minutos: Int): String = when {
    minutos < 60 -> "$minutos min"
    minutos % 60 == 0 -> "${minutos / 60}h"
    else -> "${minutos / 60}h${minutos % 60}"
}

/** Resumo de vários serviços marcados para o mesmo horário. */
val List<Servico>.titulo: String get() = joinToString(" + ") { it.nome }
val List<Servico>.duracaoTotalMin: Int get() = sumOf { it.duracaoMin }
/** Tempo total estimado para este cabelo (serviços sem formulário usam a duração padrão). */
fun List<Servico>.duracaoPara(cabelo: DadosCabelo?): Int = sumOf { it.duracaoPara(cabelo) }
val List<Servico>.duracaoMinima: Int get() = sumOf { it.duracaoMinima }
val List<Servico>.temServicoDeCabelo: Boolean get() = any { it.dependeDoCabelo }

enum class Comprimento(val rotulo: String, val ajuda: String) {
    CURTO("Curto", "Acima do ombro"),
    MEDIO("Médio", "Até o ombro"),
    LONGO("Longo", "Abaixo do ombro"),
    EXTRA_LONGO("Extra longo", "Na cintura"),
}

enum class TipoCabelo(val rotulo: String) { LISO("Liso"), ONDULADO("Ondulado"), CACHEADO("Cacheado"), CRESPO("Crespo") }


/** Respostas do formulário "Sobre seu cabelo". Comprimento, tipo e química são obrigatórios. */
data class DadosCabelo(
    val comprimento: Comprimento? = null,
    val tipo: TipoCabelo? = null,
    val fezQuimica: Boolean? = null,
) {
    val completo: Boolean get() = comprimento != null && tipo != null && fezQuimica != null
    val resumo: String get() = listOfNotNull(comprimento?.rotulo, tipo?.rotulo).joinToString(" · ")
}

/** Dados do salão que as clientes veem no perfil. `whatsapp` contém só dígitos. */
data class DadosSalao(
    val nome: String = "Skala Salão de Beleza",
    val endereco: String = "Rua das Camélias, 245 - Jardim Paulista, São Paulo - SP",
    val whatsapp: String = "11934567890",
)

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

/**
 * Cliente do salão. Pode ter conta no app (com e-mail) ou ter sido cadastrada pela dona,
 * só com nome e celular. O celular (só dígitos) identifica a cliente.
 */
data class Cliente(
    val nome: String,
    val celular: String,
    val email: String = "",
)

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
    /** Só dígitos. Vazio em agendamentos antigos. */
    val clienteCelular: String = "",
    /** true quando foi a dona que reservou o horário para a cliente. */
    val reservadoPelaDona: Boolean = false,
) {
    val titulo: String get() = servicos.titulo
    /** Usa as respostas do cabelo salvas no agendamento, então a agenda reserva o tempo certo. */
    val duracaoMin: Int get() = servicos.duracaoPara(cabelo)
    val duracaoTexto: String get() = textoDuracao(duracaoMin)
    val fim: LocalTime get() = inicio.plusMinutes(duracaoMin.toLong())

    /** O agendamento é desta cliente? Compara pelo celular e, se tiver, pelo e-mail. */
    fun eDe(cliente: Cliente): Boolean =
        (cliente.celular.isNotEmpty() && clienteCelular == cliente.celular) ||
            (cliente.email.isNotEmpty() && clienteEmail.equals(cliente.email, ignoreCase = true))
}

/**
 * Agendamentos confirmados que ficariam fora do funcionamento se o dia fosse salvo assim:
 * dia fechado, horário fora de todas as faixas ou em cima de um bloqueio.
 */
fun DiaFuncionamento.agendamentosForaDoHorario(agendamentos: List<Agendamento>): List<Agendamento> =
    agendamentos.filter { a ->
        a.data == data && a.status == StatusAgendamento.CONFIRMADO && (
            status != StatusDia.ABERTO ||
                faixas.none { !a.inicio.isBefore(it.inicio) && !a.fim.isAfter(it.fim) } ||
                bloqueios.any { a.inicio.isBefore(it.fim) && it.inicio.isBefore(a.fim) }
            )
    }.sortedBy { it.inicio }

enum class TipoAvisoDona(val verbo: String) {
    MARCOU("marcou"), DESMARCOU("desmarcou"), REMARCOU("remarcou")
}

/**
 * Aviso para a dona quando uma cliente marca, desmarca ou remarca pelo app.
 * `agendamento` é o horário novo (ou o cancelado, em DESMARCOU); `anterior` só existe em REMARCOU.
 */
data class AvisoDona(
    val id: String,
    val tipo: TipoAvisoDona,
    val agendamento: Agendamento,
    val anterior: Agendamento? = null,
    val quando: java.time.LocalDateTime = java.time.LocalDateTime.now(),
    val lido: Boolean = false,
)
