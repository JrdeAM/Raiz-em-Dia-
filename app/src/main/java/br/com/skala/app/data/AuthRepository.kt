package br.com.skala.app.data

import kotlinx.coroutines.delay

data class Usuario(
    val nome: String,
    val email: String,
    val celular: String,
    /** A dona do salão entra pelo mesmo login, mas o app abre o painel dela. */
    val ehDona: Boolean = false,
)

/** Dados enviados quando o cadastro passa na validação. `celular` contém só dígitos. */
data class DadosCadastro(
    val nome: String,
    val celular: String,
    val email: String,
    val senha: String,
)

sealed class AuthErro : Exception() {
    class CredenciaisInvalidas : AuthErro()
    class EmailJaCadastrado : AuthErro()
}

/** Contrato que a API real vai implementar depois. */
interface AuthRepository {
    suspend fun entrar(email: String, senha: String): Result<Usuario>
    suspend fun cadastrar(dados: DadosCadastro): Result<Usuario>
}

/**
 * Autenticação de mentira, só em memória, para testar as telas sem API.
 * Já existem duas contas de teste (cliente e dona do salão); contas criadas no cadastro valem até fechar o app.
 */
object FakeAuthRepository : AuthRepository {
    private const val ATRASO_MS = 1200L

    private data class Conta(val usuario: Usuario, val senha: String)

    private val contas = mutableMapOf(
        "cliente@skala.com" to Conta(
            usuario = Usuario(nome = "Maria Clara", email = "cliente@skala.com", celular = "11912345678"),
            senha = "skala123",
        ),
        "dona@skala.com" to Conta(
            usuario = Usuario(nome = "Dona do salão", email = "dona@skala.com", celular = "11900000000", ehDona = true),
            senha = "skala123",
        ),
    )

    override suspend fun entrar(email: String, senha: String): Result<Usuario> {
        delay(ATRASO_MS)
        val conta = contas[email.lowercase()]
        return if (conta != null && conta.senha == senha) {
            Result.success(conta.usuario)
        } else {
            Result.failure(AuthErro.CredenciaisInvalidas())
        }
    }

    override suspend fun cadastrar(dados: DadosCadastro): Result<Usuario> {
        delay(ATRASO_MS)
        val chave = dados.email.lowercase()
        if (chave in contas) return Result.failure(AuthErro.EmailJaCadastrado())
        val usuario = Usuario(nome = dados.nome, email = chave, celular = dados.celular)
        contas[chave] = Conta(usuario, dados.senha)
        return Result.success(usuario)
    }
}
