package br.com.skala.app.data

import java.time.LocalDate
import java.time.LocalDateTime
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
    /** Conta (uid do Firebase) de quem marcou pelo app. Vazio quando a dona reservou. */
    val clienteUid: String = "",
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

/**
 * Agendamentos, avisos e dias de funcionamento são apagados quando passam [DIAS_GUARDADOS] dias
 * da data deles, para o banco não guardar o que não serve mais.
 */
const val DIAS_GUARDADOS = 60L

/** A partir de quando um registro do dia `data` pode ser apagado. */
fun apagarDepoisDe(data: LocalDate): LocalDateTime = data.plusDays(DIAS_GUARDADOS + 1).atStartOfDay()

/** Um horário já ocupado no dia (só início e fim, sem dados da cliente). */
data class Ocupacao(val inicio: LocalTime, val fim: LocalTime)

/** De quanto em quanto tempo os horários aparecem para a cliente. */
private const val PASSO_MIN = 30L

/**
 * Horários em que cabe um atendimento de `duracaoMin` no `dia`, sem encostar nos horários
 * `ocupados` (respeitando o intervalo entre clientes) nem nos bloqueios. Horários que já passaram
 * ficam de fora.
 */
fun calcularHorariosLivres(
    dia: DiaFuncionamento,
    ocupados: Collection<Ocupacao>,
    duracaoMin: Int,
    agora: LocalDateTime = LocalDateTime.now(),
): List<LocalTime> {
    val data = dia.data
    if (dia.status != StatusDia.ABERTO || data.isBefore(agora.toLocalDate())) return emptyList()
    val intervalo = dia.intervaloMin.toLong()
    val livres = mutableListOf<LocalTime>()
    for (faixa in dia.faixas) {
        var inicio = faixa.inicio
        while (!inicio.plusMinutes(duracaoMin.toLong()).isAfter(faixa.fim)) {
            val fim = inicio.plusMinutes(duracaoMin.toLong())
            val colideAgendamento = ocupados.any { o ->
                inicio.isBefore(o.fim.plusMinutes(intervalo)) && o.inicio.isBefore(fim.plusMinutes(intervalo))
            }
            val colideBloqueio = dia.bloqueios.any { b -> inicio.isBefore(b.fim) && b.inicio.isBefore(fim) }
            val jaPassou = !LocalDateTime.of(data, inicio).isAfter(agora)
            if (!colideAgendamento && !colideBloqueio && !jaPassou) livres += inicio
            inicio = inicio.plusMinutes(PASSO_MIN)
            if (inicio == LocalTime.MIDNIGHT) break
        }
    }
    return livres
}

/** O horário escolhido foi ocupado por outra pessoa (ou deixou de existir) antes de confirmar. */
class HorarioIndisponivel : Exception()

/** Os serviços do salão. Ficam no app (não no banco) porque mudam pouco. */
object Catalogo {
    // Tempos estimados (em minutos) dos serviços de cabelo. A dona pode ajustar aqui.
    // Ordem dos comprimentos: curto, médio, longo, extra longo.
    private fun tempos(
        curto: Int, medio: Int, longo: Int, extraLongo: Int,
        ondulado: Int = 0, cacheado: Int = 0, crespo: Int = 0,
    ) = TemposCabelo(
        porComprimento = mapOf(
            Comprimento.CURTO to curto, Comprimento.MEDIO to medio,
            Comprimento.LONGO to longo, Comprimento.EXTRA_LONGO to extraLongo,
        ),
        extraPorTipo = mapOf(TipoCabelo.ONDULADO to ondulado, TipoCabelo.CACHEADO to cacheado, TipoCabelo.CRESPO to crespo),
    )

    val servicos = listOf(
        Servico("corte", "Corte feminino", 30, "Cabelo", dependeDoCabelo = true,
            tempos = tempos(30, 30, 30, 30, ondulado = 5, cacheado = 15, crespo = 30)),
        Servico("corte_masculino", "Corte masculino", 30, "Cabelo", dependeDoCabelo = true,
            tempos = tempos(30, 30, 30, 30, ondulado = 5, cacheado = 15, crespo = 20)),
        Servico("escova", "Escova", 60, "Cabelo", dependeDoCabelo = true,
            tempos = tempos(30, 50, 60, 80, ondulado = 10, cacheado = 20, crespo = 30)),
        Servico("coloracao", "Coloração", 120, "Cabelo", dependeDoCabelo = true,
            tempos = tempos(90, 120, 150, 180, cacheado = 20, crespo = 30)),
        Servico("hidratacao", "Hidratação", 40, "Cabelo", dependeDoCabelo = true,
            tempos = tempos(20, 30, 40, 60, ondulado = 10, cacheado = 20, crespo = 30)),
        Servico("sobrancelha", "Design de sobrancelha", 30, "Sobrancelha", dependeDoCabelo = false),
        Servico("maquiagem", "Maquiagem", 60, "Maquiagem", dependeDoCabelo = false),
    )

    init {
        // Cada serviço precisa de um id próprio: as telas usam o id para listar e marcar.
        val repetidos = servicos.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(repetidos.isEmpty()) { "Serviços com id repetido: $repetidos" }
    }
}
