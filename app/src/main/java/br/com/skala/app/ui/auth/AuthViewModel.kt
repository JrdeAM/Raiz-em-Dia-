package br.com.skala.app.ui.auth

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.skala.app.R
import br.com.skala.app.data.AuthErro
import br.com.skala.app.data.AuthRepository
import br.com.skala.app.data.DadosCadastro
import br.com.skala.app.data.PassoCelular
import br.com.skala.app.data.Repositorios
import br.com.skala.app.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Etapas do login pelo celular: digitar o número, digitar o código do SMS e (se for conta nova) o nome. */
enum class EtapaCelular { NUMERO, CODIGO, NOME }

data class AuthUiState(
    val carregando: Boolean = false,
    @StringRes val erro: Int? = null,
    val usuario: Usuario? = null,
    val contaNova: Boolean = false,
    /** true enquanto confere se já havia alguém logado ao abrir o app. */
    val restaurando: Boolean = false,
    val etapaCelular: EtapaCelular = EtapaCelular.NUMERO,
    /** Número (só dígitos) para onde o código foi enviado. */
    val celularDoCodigo: String = "",
)

/** Estado de login/cadastro compartilhado pelas telas. */
class AuthViewModel(
    private val repositorio: AuthRepository = Repositorios.auth,
) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState(restaurando = true))
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    init {
        // Quem já estava logado entra direto ao abrir o app
        viewModelScope.launch {
            val usuario = runCatching { repositorio.usuarioLogado() }.getOrNull()
            _estado.update { it.copy(usuario = usuario, restaurando = false) }
        }
    }

    fun entrar(email: String, senha: String) = executar(contaNova = false) { repositorio.entrar(email, senha) }

    fun cadastrar(dados: DadosCadastro) = executar(contaNova = true) { repositorio.cadastrar(dados) }

    // ---------- login pelo celular ----------

    fun enviarCodigo(celular: String, activity: Activity) {
        if (_estado.value.carregando) return
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            repositorio.enviarCodigoSms(celular, activity)
                .onSuccess { passo -> aplicar(passo, celular) }
                .onFailure { e -> _estado.update { it.copy(carregando = false, erro = e.mensagemRes()) } }
        }
    }

    fun confirmarCodigo(codigo: String) {
        if (_estado.value.carregando) return
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            repositorio.confirmarCodigoSms(codigo)
                .onSuccess { passo -> aplicar(passo, _estado.value.celularDoCodigo) }
                .onFailure { e -> _estado.update { it.copy(carregando = false, erro = e.mensagemRes()) } }
        }
    }

    fun concluirCadastroCelular(nome: String) = executar(contaNova = true) { repositorio.concluirCadastroCelular(nome) }

    /** Volta para digitar outro número. */
    fun trocarNumero() = _estado.update { it.copy(etapaCelular = EtapaCelular.NUMERO, erro = null) }

    private fun aplicar(passo: PassoCelular, celular: String) = _estado.update {
        when (passo) {
            PassoCelular.CodigoEnviado -> it.copy(carregando = false, etapaCelular = EtapaCelular.CODIGO, celularDoCodigo = celular)
            PassoCelular.FaltaNome -> it.copy(carregando = false, etapaCelular = EtapaCelular.NOME, celularDoCodigo = celular)
            is PassoCelular.Entrou -> it.copy(carregando = false, usuario = passo.usuario, etapaCelular = EtapaCelular.NUMERO)
        }
    }

    // ---------- conta ----------

    /** Manda o e-mail para criar outra senha. `aoTerminar` recebe a mensagem para mostrar. */
    fun recuperarSenha(email: String, aoTerminar: (sucesso: Boolean) -> Unit) {
        viewModelScope.launch {
            val r = repositorio.recuperarSenha(email)
            aoTerminar(r.isSuccess)
        }
    }

    fun atualizarDados(nome: String, celular: String, aoTerminar: (sucesso: Boolean) -> Unit = {}) {
        val atual = _estado.value.usuario ?: return
        viewModelScope.launch {
            repositorio.atualizar(atual.copy(nome = nome, celular = celular))
                .onSuccess { usuario -> _estado.update { it.copy(usuario = usuario) } }
                .also { aoTerminar(it.isSuccess) }
        }
    }

    fun limparErro() = _estado.update { it.copy(erro = null) }

    fun sair() {
        repositorio.sair()
        _estado.value = AuthUiState()
    }

    private fun executar(contaNova: Boolean, acao: suspend () -> Result<Usuario>) {
        if (_estado.value.carregando) return
        _estado.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            acao()
                .onSuccess { usuario ->
                    _estado.update {
                        it.copy(carregando = false, usuario = usuario, contaNova = contaNova, etapaCelular = EtapaCelular.NUMERO)
                    }
                }
                .onFailure { e ->
                    _estado.update { it.copy(carregando = false, erro = e.mensagemRes()) }
                }
        }
    }
}

@StringRes
private fun Throwable.mensagemRes(): Int = when (this) {
    is AuthErro.CredenciaisInvalidas -> R.string.erro_credenciais
    is AuthErro.EmailJaCadastrado -> R.string.erro_email_existente
    is AuthErro.CodigoInvalido -> R.string.erro_codigo_invalido
    is AuthErro.CelularInvalido -> R.string.erro_celular_invalido
    is AuthErro.MuitasTentativas -> R.string.erro_muitas_tentativas
    is AuthErro.SemConexao -> R.string.erro_sem_conexao
    else -> R.string.erro_generico
}
