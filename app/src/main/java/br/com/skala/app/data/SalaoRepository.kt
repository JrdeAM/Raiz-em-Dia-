package br.com.skala.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Contrato que a API real vai implementar depois. */
interface SalaoRepository {
    val servicos: List<Servico>
    val dias: StateFlow<Map<LocalDate, DiaFuncionamento>>
    val agendamentos: StateFlow<List<Agendamento>>

    fun dia(data: LocalDate): DiaFuncionamento
    fun salvarDia(dia: DiaFuncionamento)
    fun horariosLivres(data: LocalDate, duracaoMin: Int): List<LocalTime>
    fun agendar(agendamento: Agendamento): Agendamento
    fun cancelar(id: String)

    /** Valor aproximado de um serviço, ou null enquanto o salão não cadastrou a tabela. */
    fun estimarValor(servico: Servico, cabelo: DadosCabelo?): FaixaPreco?

    /** Soma aproximada dos serviços; null se algum ainda não tiver valor cadastrado. */
    fun estimarTotal(servicos: List<Servico>, cabelo: DadosCabelo?): FaixaPreco? {
        if (servicos.isEmpty()) return null
        val valores = servicos.map { estimarValor(it, cabelo) ?: return null }
        return valores.reduce { a, b -> a + b }
    }
}

/**
 * Dados de mentira, só em memória, para testar o app sem API. A agenda de exemplo é
 * montada a partir de hoje; o que a dona mudar ou a cliente agendar vale até fechar o app.
 */
object FakeSalaoRepository : SalaoRepository {
    private const val PASSO_MIN = 30L // de quanto em quanto tempo os horários aparecem para a cliente

    override val servicos = listOf(
        Servico("corte", "Corte feminino", 60, "Cabelo", dependeDoCabelo = true),
        Servico("corte_masculino", "Corte masculino", 40, "Cabelo", dependeDoCabelo = true),
        Servico("escova", "Escova", 60, "Cabelo", dependeDoCabelo = true),
        Servico("coloracao", "Coloração", 120, "Cabelo", dependeDoCabelo = true),
        Servico("hidratacao", "Hidratação", 60, "Cabelo", dependeDoCabelo = true),
        Servico("sobrancelha", "Design de sobrancelha", 30, "Sobrancelha", dependeDoCabelo = false),
        Servico("maquiagem", "Maquiagem", 60, "Maquiagem", dependeDoCabelo = false),
    )

    init {
        // Cada serviço precisa de um id próprio: as telas usam o id para listar e marcar.
        val repetidos = servicos.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(repetidos.isEmpty()) { "Serviços com id repetido: $repetidos" }
    }

    // Tabela de preços aproximados: a dona precisa preencher. Exemplo:
    // "escova" to mapOf(Comprimento.CURTO to FaixaPreco(50, 60), Comprimento.LONGO to FaixaPreco(80, 100))
    // Enquanto estiver vazia, o app mostra "a combinar". O valor final é sempre confirmado no salão.
    private val tabelaPrecos: Map<String, Map<Comprimento, FaixaPreco>> = emptyMap()
    private val precosFixos: Map<String, FaixaPreco> = emptyMap()

    private val hoje: LocalDate get() = LocalDate.now()

    private val _dias = MutableStateFlow(agendaDeExemplo())
    override val dias: StateFlow<Map<LocalDate, DiaFuncionamento>> = _dias.asStateFlow()

    private val _agendamentos = MutableStateFlow(agendamentosDeExemplo())
    override val agendamentos: StateFlow<List<Agendamento>> = _agendamentos.asStateFlow()

    override fun dia(data: LocalDate): DiaFuncionamento = _dias.value[data] ?: DiaFuncionamento(data)

    override fun salvarDia(dia: DiaFuncionamento) {
        _dias.update { it + (dia.data to dia) }
    }

    override fun horariosLivres(data: LocalDate, duracaoMin: Int): List<LocalTime> {
        val dia = dia(data)
        if (dia.status != StatusDia.ABERTO || data.isBefore(hoje)) return emptyList()
        val agora = LocalDateTime.now()
        val ocupados = _agendamentos.value
            .filter { it.data == data && it.status == StatusAgendamento.CONFIRMADO }
            .map { it.inicio to it.fim }
        val bloqueados = dia.bloqueios.map { it.inicio to it.fim }
        val intervalo = dia.intervaloMin.toLong()

        val livres = mutableListOf<LocalTime>()
        for (faixa in dia.faixas) {
            var inicio = faixa.inicio
            while (!inicio.plusMinutes(duracaoMin.toLong()).isAfter(faixa.fim)) {
                val fim = inicio.plusMinutes(duracaoMin.toLong())
                val colideAgendamento = ocupados.any { (a, b) ->
                    inicio.isBefore(b.plusMinutes(intervalo)) && a.isBefore(fim.plusMinutes(intervalo))
                }
                val colideBloqueio = bloqueados.any { (a, b) -> inicio.isBefore(b) && a.isBefore(fim) }
                val jaPassou = data == hoje && !LocalDateTime.of(data, inicio).isAfter(agora)
                if (!colideAgendamento && !colideBloqueio && !jaPassou) livres += inicio
                inicio = inicio.plusMinutes(PASSO_MIN)
                if (inicio == LocalTime.MIDNIGHT) break
            }
        }
        return livres
    }

    override fun agendar(agendamento: Agendamento): Agendamento {
        val novo = agendamento.copy(id = UUID.randomUUID().toString())
        _agendamentos.update { it + novo }
        return novo
    }

    override fun cancelar(id: String) {
        _agendamentos.update { lista ->
            lista.map { if (it.id == id) it.copy(status = StatusAgendamento.CANCELADO) else it }
        }
    }

    override fun estimarValor(servico: Servico, cabelo: DadosCabelo?): FaixaPreco? =
        if (servico.dependeDoCabelo) cabelo?.comprimento?.let { tabelaPrecos[servico.id]?.get(it) }
        else precosFixos[servico.id]

    // ---------- dados de exemplo ----------

    private fun h(hora: Int, minuto: Int = 0): LocalTime = LocalTime.of(hora, minuto)

    /** Três semanas para trás e duas para frente definidas; o resto fica "a definir". */
    private fun agendaDeExemplo(): Map<LocalDate, DiaFuncionamento> {
        val dias = mutableMapOf<LocalDate, DiaFuncionamento>()
        for (offset in -21L..16L) {
            val data = hoje.plusDays(offset)
            val dia = when (data.dayOfWeek) {
                DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY -> DiaFuncionamento(data, StatusDia.FECHADO)
                DayOfWeek.MONDAY -> DiaFuncionamento(data, StatusDia.ABERTO, listOf(Faixa(h(13), h(20))))
                DayOfWeek.TUESDAY -> DiaFuncionamento(data, StatusDia.ABERTO, listOf(Faixa(h(9), h(18))))
                DayOfWeek.THURSDAY -> DiaFuncionamento(data, StatusDia.ABERTO, listOf(Faixa(h(10), h(12)), Faixa(h(15), h(21))))
                DayOfWeek.FRIDAY -> DiaFuncionamento(data, StatusDia.ABERTO, listOf(Faixa(h(9), h(12)), Faixa(h(14), h(19))))
                DayOfWeek.SATURDAY -> DiaFuncionamento(data, StatusDia.ABERTO, listOf(Faixa(h(8), h(14))))
                else -> DiaFuncionamento(data)
            }
            dias[data] = dia
        }
        // Hoje sempre aberto, com uma pausa e um bloqueio, para a agenda da dona ter o que mostrar
        dias[hoje] = DiaFuncionamento(
            hoje, StatusDia.ABERTO, listOf(Faixa(h(9), h(12)), Faixa(h(14), h(19))),
            bloqueios = listOf(Bloqueio(h(16), h(16, 30), "Compromisso pessoal")),
        )
        return dias
    }

    private fun agendamentosDeExemplo(): List<Agendamento> {
        fun s(id: String) = servicos.first { it.id == id }
        val cliente = "cliente@skala.com"
        val proximoAberto = (1L..7L).map { hoje.plusDays(it) }
            .first { d -> d.dayOfWeek !in setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY) }
        val inicioProximo = when (proximoAberto.dayOfWeek) {
            DayOfWeek.MONDAY -> h(14); DayOfWeek.THURSDAY -> h(15); DayOfWeek.SATURDAY -> h(11)
            else -> h(10)
        }
        val cabeloMaria = DadosCabelo(Comprimento.LONGO, TipoCabelo.ONDULADO, Volume.MEDIO, false)
        return listOf(
            Agendamento("a1", "Maria Clara", cliente, listOf(s("escova"), s("hidratacao")), proximoAberto, inicioProximo, cabeloMaria),
            Agendamento("a2", "Ana Paula", "ana@exemplo.com", listOf(s("corte")), hoje, h(10), DadosCabelo(Comprimento.MEDIO, TipoCabelo.LISO)),
            Agendamento("a3", "Júlia", "julia@exemplo.com", listOf(s("coloracao")), hoje, h(14), DadosCabelo(Comprimento.LONGO, TipoCabelo.CACHEADO)),
            Agendamento("a4", "Fernanda", "fernanda@exemplo.com", listOf(s("sobrancelha")), hoje, h(18)),
            Agendamento("a5", "Maria Clara", cliente, listOf(s("coloracao")), hoje.minusDays(13), h(9), cabeloMaria, status = StatusAgendamento.CONCLUIDO),
            Agendamento("a6", "Maria Clara", cliente, listOf(s("sobrancelha")), hoje.minusDays(27), h(10), status = StatusAgendamento.CONCLUIDO),
        )
    }
}
