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

/**
 * Dados do salão. Há duas versões: [FirebaseSalaoRepository] (banco na nuvem, usado quando o
 * projeto tem o google-services.json) e [FakeSalaoRepository] (só no celular, para testar).
 */
interface SalaoRepository {
    val servicos: List<Servico>
    val dias: StateFlow<Map<LocalDate, DiaFuncionamento>>
    /** Para a dona: todos. Para a cliente: só os dela. */
    val agendamentos: StateFlow<List<Agendamento>>
    val dadosSalao: StateFlow<DadosSalao>
    val clientes: StateFlow<List<Cliente>>

    /**
     * Começa a acompanhar os dados que este usuário pode ver (null = ninguém logado, limpa tudo).
     * Quando é a dona, também apaga o que passou de [DIAS_GUARDADOS] dias.
     */
    fun conectar(usuario: Usuario?)

    /** Cadastra uma cliente nova. Falha com [ClienteJaCadastrada] se o celular já existir. */
    fun cadastrarCliente(cliente: Cliente): Result<Cliente>

    /** Garante que a cliente que entrou pelo app apareça na lista da dona (atualiza se já existir). */
    fun registrarClienteDoApp(cliente: Cliente)

    /** Avisos para a dona (mais novos primeiro). */
    val avisosDona: StateFlow<List<AvisoDona>>
    fun registrarAvisoDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento? = null): AvisoDona
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

    /**
     * Grava o agendamento, conferindo antes se o horário continua livre (outra cliente pode ter
     * marcado no mesmo instante): se não estiver, falha com [HorarioIndisponivel].
     * `substituirId`: remarcação. O horário antigo só é cancelado se o novo for gravado.
     */
    suspend fun agendar(agendamento: Agendamento, substituirId: String? = null): Result<Agendamento>
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
    override val servicos = Catalogo.servicos

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

    override fun registrarAvisoDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento?): AvisoDona {
        val aviso = AvisoDona(UUID.randomUUID().toString(), tipo, agendamento, anterior)
        _avisosDona.update { listOf(aviso) + it }
        persistir()
        return aviso
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

    override fun horariosLivres(data: LocalDate, duracaoMin: Int, ignorarAgendamentoId: String?): List<LocalTime> =
        calcularHorariosLivres(dia(data), ocupados(data, ignorarAgendamentoId), duracaoMin)

    private fun ocupados(data: LocalDate, ignorarAgendamentoId: String?) = _agendamentos.value
        .filter { it.data == data && it.status == StatusAgendamento.CONFIRMADO && it.id != ignorarAgendamentoId }
        .map { Ocupacao(it.inicio, it.fim) }

    override suspend fun agendar(agendamento: Agendamento, substituirId: String?): Result<Agendamento> {
        val livres = horariosLivres(agendamento.data, agendamento.duracaoMin, substituirId)
        if (agendamento.inicio !in livres) return Result.failure(HorarioIndisponivel())
        val novo = agendamento.copy(id = UUID.randomUUID().toString())
        _agendamentos.update { lista ->
            lista.map { if (it.id == substituirId) it.copy(status = StatusAgendamento.CANCELADO) else it } + novo
        }
        persistir()
        return Result.success(novo)
    }

    /** Os dados ficam só neste celular, então não há o que acompanhar. */
    override fun conectar(usuario: Usuario?) = Unit

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
        apagarAntigos()
    }

    /** Mesma regra do banco na nuvem: o que passou de [DIAS_GUARDADOS] dias é apagado. */
    private fun apagarAntigos() {
        val agora = LocalDateTime.now()
        fun venceu(data: LocalDate) = !apagarDepoisDe(data).isAfter(agora)
        _agendamentos.update { lista -> lista.filterNot { venceu(it.data) } }
        _avisosDona.update { lista -> lista.filterNot { venceu(it.quando.toLocalDate()) } }
        _dias.update { mapa -> mapa.filterKeys { !venceu(it) } }
        persistir()
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
