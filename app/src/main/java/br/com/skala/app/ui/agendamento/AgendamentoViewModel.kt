package br.com.skala.app.ui.agendamento

import androidx.lifecycle.ViewModel
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.DadosCabelo
import br.com.skala.app.data.FaixaPreco
import br.com.skala.app.data.FakeSalaoRepository
import br.com.skala.app.data.SalaoRepository
import br.com.skala.app.data.Servico
import br.com.skala.app.data.Usuario
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
)

class AgendamentoViewModel(
    val repositorio: SalaoRepository = FakeSalaoRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(AgendamentoUiState())
    val estado: StateFlow<AgendamentoUiState> = _estado.asStateFlow()

    fun iniciar(servico: Servico? = null, categoria: String? = null) {
        _estado.value = AgendamentoUiState(servicos = listOfNotNull(servico), categoria = categoria)
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
    fun atualizarCabelo(cabelo: DadosCabelo) = _estado.update { it.copy(cabelo = cabelo) }
    fun escolherData(data: LocalDate) = _estado.update { it.copy(data = data, hora = null) }
    fun escolherHora(hora: LocalTime) = _estado.update { it.copy(hora = hora) }
    fun atualizarObservacao(texto: String) = _estado.update { it.copy(observacao = texto) }

    fun valorDe(servico: Servico): FaixaPreco? = repositorio.estimarValor(servico, _estado.value.cabelo)
    fun valorTotal(): FaixaPreco? = repositorio.estimarTotal(_estado.value.servicos, _estado.value.cabelo)

    /** Grava o agendamento (e cancela o antigo, se for remarcação). Retorna null se faltar algo. */
    fun confirmar(usuario: Usuario): Agendamento? {
        val e = _estado.value
        if (e.servicos.isEmpty()) return null
        val data = e.data ?: return null
        val hora = e.hora ?: return null
        e.remarcandoId?.let { repositorio.cancelar(it) }
        val novo = repositorio.agendar(
            Agendamento(
                id = "",
                clienteNome = usuario.nome,
                clienteEmail = usuario.email,
                servicos = e.servicos,
                data = data,
                inicio = hora,
                cabelo = e.cabelo.takeIf { e.servicos.temServicoDeCabelo },
                observacao = e.observacao.trim(),
            ),
        )
        _estado.value = AgendamentoUiState()
        return novo
    }
}
