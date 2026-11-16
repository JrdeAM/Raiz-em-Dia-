package br.com.skala.app.data

import android.content.Context
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
    val dadosSalao: StateFlow<DadosSalao>
    val clientes: StateFlow<List<Cliente>>

    /** Cadastra uma cliente nova. Falha com [ClienteJaCadastrada] se o celular já existir. */
    fun cadastrarCliente(cliente: Cliente): Result<Cliente>

    /** Garante que a cliente que entrou pelo app apareça na lista da dona (atualiza se já existir). */
    fun registrarClienteDoApp(cliente: Cliente)

    /** Avisos para a dona (mais novos primeiro). Com a API, viriam do servidor. */
    val avisosDona: StateFlow<List<AvisoDona>>
    fun registrarAvisoDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento? = null)
    fun marcarAvisosLidos()

    fun salvarDadosSalao(dados: DadosSalao)

    fun dia(data: LocalDate): DiaFuncionamento
    fun salvarDia(dia: DiaFuncionamento)
    /**
     * Horários em que cabe um atendimento de `duracaoMin` naquele dia.
     * `ignorarAgendamentoId`: na remarcação, o horário antigo da própria cliente não conta como
     * ocupado (ele é cancelado quando o novo for confirmado).
     */
    fun horariosLivres(data: LocalDate, duracaoMin: Int, ignorarAgendamentoId: String? = null): List<LocalTime>
    fun agendar(agendamento: Agendamento): Agendamento
    fun cancelar(id: String)
}

class ClienteJaCadastrada(val existente: Cliente) : Exception()

/**
 * Dados do salão guardados no próprio celular, enquanto não existe a API. Na primeira vez que
 * o app abre, começa com dados de exemplo (agenda montada a partir de hoje); depois disso, tudo o
 * que a dona muda e a cliente agenda fica salvo e continua lá ao fechar e abrir o app.
 * Precisa de [iniciar] (feito na MainActivity) para ler e gravar no celular.
 */
object FakeSalaoRepository : SalaoRepository {
    private const val PASSO_MIN = 30L // de quanto em quanto tempo os horários aparecem para a cliente

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

    override val servicos = listOf(
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

    // O app não mostra valores: o preço é informado no salão, após a avaliação e o serviço.

    private val hoje: LocalDate get() = LocalDate.now()

    private val _dias = MutableStateFlow(agendaDeExemplo())
    override val dias: StateFlow<Map<LocalDate, DiaFuncionamento>> = _dias.asStateFlow()

    private val _agendamentos = MutableStateFlow(agendamentosDeExemplo())
    override val agendamentos: StateFlow<List<Agendamento>> = _agendamentos.asStateFlow()

    private val _dadosSalao = MutableStateFlow(DadosSalao())
    override val dadosSalao: StateFlow<DadosSalao> = _dadosSalao.asStateFlow()

    override fun salvarDadosSalao(dados: DadosSalao) {
        _dadosSalao.value = dados
        persistir()
    }

    private val _clientes = MutableStateFlow(clientesDeExemplo())
    override val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    override fun cadastrarCliente(cliente: Cliente): Result<Cliente> {
        _clientes.value.firstOrNull { it.celular == cliente.celular }?.let {
            return Result.failure(ClienteJaCadastrada(it))
        }
        _clientes.update { it + cliente }
        persistir()
        return Result.success(cliente)
    }

    override fun registrarClienteDoApp(cliente: Cliente) {
        _clientes.update { lista ->
            val i = lista.indexOfFirst {
                (cliente.email.isNotEmpty() && it.email.equals(cliente.email, ignoreCase = true)) || it.celular == cliente.celular
            }
            if (i < 0) lista + cliente else lista.toMutableList().also { it[i] = cliente }
        }
        persistir()
    }

    private val _avisosDona = MutableStateFlow(avisosDeExemplo())
    override val avisosDona: StateFlow<List<AvisoDona>> = _avisosDona.asStateFlow()

    override fun registrarAvisoDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento?) {
        _avisosDona.update { listOf(AvisoDona(UUID.randomUUID().toString(), tipo, agendamento, anterior)) + it }
        persistir()
    }

    override fun marcarAvisosLidos() {
        _avisosDona.update { lista -> lista.map { it.copy(lido = true) } }
        persistir()
    }

    override fun dia(data: LocalDate): DiaFuncionamento = _dias.value[data] ?: DiaFuncionamento(data)

    override fun salvarDia(dia: DiaFuncionamento) {
        _dias.update { it + (dia.data to dia) }
        persistir()
    }

    override fun horariosLivres(data: LocalDate, duracaoMin: Int, ignorarAgendamentoId: String?): List<LocalTime> {
        val dia = dia(data)
        if (dia.status != StatusDia.ABERTO || data.isBefore(hoje)) return emptyList()
        val agora = LocalDateTime.now()
        val ocupados = _agendamentos.value
            .filter { it.data == data && it.status == StatusAgendamento.CONFIRMADO && it.id != ignorarAgendamentoId }
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
        persistir()
        return novo
    }

    override fun cancelar(id: String) {
        _agendamentos.update { lista ->
            lista.map { if (it.id == id) it.copy(status = StatusAgendamento.CANCELADO) else it }
        }
        persistir()
    }

    // ---------- salvar no celular ----------

    private const val K_DIAS = "dias"
    private const val K_AGENDAMENTOS = "agendamentos"
    private const val K_SALAO = "salao"
    private const val K_CLIENTES = "clientes"
    private const val K_AVISOS = "avisos"

    private var armazenamento: Armazenamento? = null

    /**
     * Liga o repositório ao armazenamento do celular. Se já houver dados salvos, eles substituem
     * os de exemplo; se for a primeira vez, os dados de exemplo são salvos como ponto de partida.
     */
    fun iniciar(context: Context) {
        if (armazenamento != null) return
        val a = Armazenamento(context, "skala_salao")
        armazenamento = a
        if (a.ler(K_AGENDAMENTOS) == null) {
            persistir()
            return
        }
        JsonSalao.lerLista(a.ler(K_DIAS), JsonSalao::lerDia)?.let { lista -> _dias.value = lista.associateBy { it.data } }
        JsonSalao.lerLista(a.ler(K_AGENDAMENTOS)) { JsonSalao.lerAgendamento(it, servicos) }?.let { _agendamentos.value = it }
        JsonSalao.lerSalao(a.ler(K_SALAO))?.let { _dadosSalao.value = it }
        JsonSalao.lerLista(a.ler(K_CLIENTES), JsonSalao::lerCliente)?.let { _clientes.value = it }
        JsonSalao.lerLista(a.ler(K_AVISOS)) { JsonSalao.lerAviso(it, servicos) }?.let { _avisosDona.value = it }
    }

    private fun persistir() {
        val a = armazenamento ?: return
        a.gravar(
            mapOf(
                K_DIAS to JsonSalao.lista(_dias.value.values, JsonSalao::dia),
                K_AGENDAMENTOS to JsonSalao.lista(_agendamentos.value, JsonSalao::agendamento),
                K_SALAO to JsonSalao.salao(_dadosSalao.value),
                K_CLIENTES to JsonSalao.lista(_clientes.value, JsonSalao::cliente),
                K_AVISOS to JsonSalao.lista(_avisosDona.value, JsonSalao::aviso),
            ),
        )
    }

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
            bloqueios = listOf(Bloqueio(h(17), h(17, 30), "Compromisso pessoal")),
        )
        return dias
    }

    /** Um aviso de exemplo: o horário da Maria Clara, que ela marcou pelo app. */
    private fun avisosDeExemplo(): List<AvisoDona> =
        _agendamentos.value.filter { it.id == "a1" }.map {
            AvisoDona("aviso1", TipoAvisoDona.MARCOU, it, quando = LocalDateTime.now().minusHours(2))
        }

    private fun clientesDeExemplo(): List<Cliente> = listOf(
        Cliente("Maria Clara", "11912345678", "cliente@skala.com"),
        Cliente("Ana Paula", "11955551234", "ana@exemplo.com"),
        Cliente("Júlia", "11955552345", "julia@exemplo.com"),
        Cliente("Fernanda", "11955553456", "fernanda@exemplo.com"),
    )

    private fun agendamentosDeExemplo(): List<Agendamento> {
        fun s(id: String) = servicos.first { it.id == id }
        val cliente = "cliente@skala.com"
        val proximoAberto = (1L..7L).map { hoje.plusDays(it) }
            .first { d -> d.dayOfWeek !in setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY) }
        val inicioProximo = when (proximoAberto.dayOfWeek) {
            DayOfWeek.MONDAY -> h(14); DayOfWeek.THURSDAY -> h(15); DayOfWeek.SATURDAY -> h(11)
            else -> h(9)
        }
        val cabeloMaria = DadosCabelo(Comprimento.LONGO, TipoCabelo.ONDULADO, fezQuimica = false)
        return listOf(
            Agendamento("a1", "Maria Clara", cliente, listOf(s("escova"), s("hidratacao")), proximoAberto, inicioProximo, cabeloMaria, clienteCelular = "11912345678"),
            Agendamento("a2", "Ana Paula", "ana@exemplo.com", listOf(s("corte")), hoje, h(10), DadosCabelo(Comprimento.MEDIO, TipoCabelo.LISO), clienteCelular = "11955551234"),
            Agendamento("a3", "Júlia", "julia@exemplo.com", listOf(s("coloracao")), hoje, h(14), DadosCabelo(Comprimento.LONGO, TipoCabelo.CACHEADO), clienteCelular = "11955552345"),
            Agendamento("a4", "Fernanda", "fernanda@exemplo.com", listOf(s("sobrancelha")), hoje, h(18), clienteCelular = "11955553456"),
            Agendamento("a5", "Maria Clara", cliente, listOf(s("coloracao")), hoje.minusDays(13), h(9), cabeloMaria, status = StatusAgendamento.CONCLUIDO, clienteCelular = "11912345678"),
            Agendamento("a6", "Maria Clara", cliente, listOf(s("sobrancelha")), hoje.minusDays(27), h(10), status = StatusAgendamento.CONCLUIDO, clienteCelular = "11912345678"),
        )
    }
}
