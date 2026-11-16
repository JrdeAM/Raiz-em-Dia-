package br.com.skala.app.ui.agendamento

import androidx.lifecycle.ViewModel
import br.com.skala.app.avisos.AvisosAgendamento
import br.com.skala.app.avisos.AvisosDoApp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.Cliente
import br.com.skala.app.data.DadosCabelo
import br.com.skala.app.data.FakeSalaoRepository
import br.com.skala.app.data.SalaoRepository
import br.com.skala.app.data.Servico
import br.com.skala.app.data.Usuario
import br.com.skala.app.data.TipoAvisoDona
import br.com.skala.app.data.duracaoPara
import br.com.skala.app.data.temServicoDeCabelo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime

/** O que a cliente já escolheu no fluxo Serviços → Sobre seu cabelo → Horário → Confirmar. */
data class AgendamentoUiState(
    /** Um ou mais serviços, feitos em sequência no mesmo horário. */
    val servicos: List<Servico> = emptyList(),
    /** Filtro já aberto na tela de serviços (quando a cliente vem de um atalho do Início). */
    val categoria: String? = null,
    val cabelo: DadosCabelo = DadosCabelo(),
    val data: LocalDate? = null,
    val hora: LocalTime? = null,
    val observacao: String = "",
    /** Preenchido quando a cliente está remarcando: o horário antigo é cancelado ao confirmar. */
    val remarcandoId: String? = null,
    /** Preenchido quando é a dona reservando o horário para esta cliente. */
    val paraCliente: Cliente? = null,
) {
    /** Tempo total estimado com as respostas do cabelo que já existem. */
    val duracaoMin: Int get() = servicos.duracaoPara(cabelo)
}

class AgendamentoViewModel(
    val repositorio: SalaoRepository = FakeSalaoRepository,
    /** WhatsApp (simulado) para a cliente e lembretes no celular. */
    val avisos: AvisosAgendamento = AvisosDoApp,
) : ViewModel() {

    private val _estado = MutableStateFlow(AgendamentoUiState())
    val estado: StateFlow<AgendamentoUiState> = _estado.asStateFlow()

    fun iniciar(servico: Servico? = null, categoria: String? = null) {
        _estado.value = AgendamentoUiState(servicos = listOfNotNull(servico), categoria = categoria)
    }

    /** A dona começa a reservar um horário para uma cliente. */
    fun iniciarParaCliente(cliente: Cliente) {
        _estado.value = AgendamentoUiState(paraCliente = cliente)
    }

    fun remarcar(agendamento: Agendamento) {
        _estado.value = AgendamentoUiState(
            servicos = agendamento.servicos,
            cabelo = agendamento.cabelo ?: DadosCabelo(),
            observacao = agendamento.observacao,
            remarcandoId = agendamento.id,
        )
    }

    /** "Agendar de novo" a partir de um atendimento antigo: mesmos serviços e mesmo cabelo. */
    fun repetir(agendamento: Agendamento) {
        _estado.value = AgendamentoUiState(servicos = agendamento.servicos, cabelo = agendamento.cabelo ?: DadosCabelo())
    }

    /** A duração muda com os serviços, então o horário escolhido antes deixa de valer. */
    fun escolherServicos(servicos: List<Servico>) = _estado.update { it.copy(servicos = servicos, data = null, hora = null) }
    /** O tempo muda com o cabelo: se o horário já escolhido não couber mais, ele é desmarcado. */
    fun atualizarCabelo(cabelo: DadosCabelo) = _estado.update { e ->
        val novo = e.copy(cabelo = cabelo)
        val data = e.data
        val hora = e.hora
        if (data != null && hora != null && novo.duracaoMin != e.duracaoMin &&
            hora !in repositorio.horariosLivres(data, novo.duracaoMin, e.remarcandoId)
        ) novo.copy(hora = null) else novo
    }
    fun escolherData(data: LocalDate) = _estado.update { it.copy(data = data, hora = null) }
    fun escolherHora(hora: LocalTime) = _estado.update { it.copy(hora = hora) }
    fun atualizarObservacao(texto: String) = _estado.update { it.copy(observacao = texto) }


    /**
     * Grava o agendamento (e cancela o antigo, se for remarcação). Retorna null se faltar algo.
     * Se a dona estiver reservando, o horário fica no nome da cliente escolhida.
     */
    fun confirmar(usuario: Usuario): Agendamento? {
        val e = _estado.value
        val cliente = e.paraCliente ?: Cliente(usuario.nome, usuario.celular, usuario.email)
        if (e.servicos.isEmpty()) return null
        val data = e.data ?: return null
        val hora = e.hora ?: return null
        val salao = repositorio.dadosSalao.value
        val antigo = e.remarcandoId?.let { id -> repositorio.agendamentos.value.firstOrNull { it.id == id } }
        if (antigo != null) {
            repositorio.cancelar(antigo.id)
            avisos.cancelado(antigo, salao, avisarCliente = false)
        }
        val novo = repositorio.agendar(
            Agendamento(
                id = "",
                clienteNome = cliente.nome,
                clienteEmail = cliente.email,
                servicos = e.servicos,
                data = data,
                inicio = hora,
                cabelo = e.cabelo.takeIf { e.servicos.temServicoDeCabelo },
                observacao = e.observacao.trim(),
                clienteCelular = cliente.celular,
                reservadoPelaDona = e.paraCliente != null,
            ),
        )
        avisos.confirmado(novo, salao, remarcado = antigo != null)
        // A dona só é avisada do que a cliente faz pelo app, não do que ela mesma marca
        if (!usuario.ehDona) avisarDona(if (antigo != null) TipoAvisoDona.REMARCOU else TipoAvisoDona.MARCOU, novo, antigo)
        _estado.value = AgendamentoUiState()
        return novo
    }

    /**
     * Cancela o horário, apaga os lembretes e avisa a cliente pelo WhatsApp.
     * `pelaCliente`: true quando foi a própria cliente que desmarcou (aí a dona é avisada).
     */
    fun cancelar(agendamento: Agendamento, pelaCliente: Boolean) {
        repositorio.cancelar(agendamento.id)
        avisos.cancelado(agendamento, repositorio.dadosSalao.value)
        if (pelaCliente) avisarDona(TipoAvisoDona.DESMARCOU, agendamento)
    }

    private fun avisarDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento? = null) {
        repositorio.registrarAvisoDona(tipo, agendamento, anterior)
        repositorio.avisosDona.value.firstOrNull()?.let(avisos::avisarDona)
    }
}
