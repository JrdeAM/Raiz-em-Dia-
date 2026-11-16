package br.com.skala.app.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.skala.app.R
import br.com.skala.app.data.AuthErro
import br.com.skala.app.data.AuthRepository
import br.com.skala.app.data.DadosCadastro
import br.com.skala.app.data.FakeAuthRepository
import br.com.skala.app.data.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val carregando: Boolean = false,
    @StringRes val erro: Int? = null,
    val usuario: Usuario? = null,
    val contaNova: Boolean = false,
)

/** Estado de login/cadastro compartilhado pelas telas. Troque o repositório pelo da API quando ela existir. */
class AuthViewModel(
    private val repositorio: AuthRepository = FakeAuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    init {
        // Quem já estava logado entra direto ao abrir o app
        repositorio.usuarioLogado()?.let { usuario -> _estado.value = AuthUiState(usuario = usuario) }
    }

    fun entrar(email: String, senha: String) = executar(contaNova = false) { repositorio.entrar(email, senha) }

    fun cadastrar(dados: DadosCadastro) = executar(contaNova = true) { repositorio.cadastrar(dados) }

    fun atualizarDados(nome: String, celular: String) {
        val atual = _estado.value.usuario ?: return
        viewModelScope.launch {
            repositorio.atualizar(atual.copy(nome = nome, celular = celular))
                .onSuccess { usuario -> _estado.update { it.copy(usuario = usuario) } }
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
                    _estado.update { it.copy(carregando = false, usuario = usuario, contaNova = contaNova) }
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
    else -> R.string.erro_generico
}
