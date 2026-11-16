package br.com.skala.app.data

import android.content.Context
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

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

    /** Atualiza nome e celular. O e-mail é o login e não muda por aqui. */
    suspend fun atualizar(usuario: Usuario): Result<Usuario>

    /** Quem estava logado da última vez (para não pedir login ao abrir o app de novo). */
    fun usuarioLogado(): Usuario?

    /** Esquece a sessão salva. */
    fun sair()
}

/**
 * Autenticação local, para testar sem API. Já existem duas contas de teste (cliente e dona do salão).
 * As contas criadas no cadastro, as mudanças de nome/celular e a sessão ficam salvas no celular
 * (a senha só como hash SHA-256). Precisa de [iniciar] (feito na MainActivity) para ler e gravar.
 */
object FakeAuthRepository : AuthRepository {
    private const val ATRASO_MS = 1200L

    private const val K_CONTAS = "contas"
    private const val K_LOGADO = "logado"

    /** `senhaHash`: SHA-256 da senha, em hexadecimal. */
    private data class Conta(val usuario: Usuario, val senhaHash: String)

    private fun hash(senha: String): String =
        MessageDigest.getInstance("SHA-256").digest(senha.toByteArray()).joinToString("") { "%02x".format(it) }

    private val contas = mutableMapOf(
        "cliente@skala.com" to Conta(
            usuario = Usuario(nome = "Maria Clara", email = "cliente@skala.com", celular = "11912345678"),
            senhaHash = hash("skala123"),
        ),
        "dona@skala.com" to Conta(
            usuario = Usuario(nome = "Carla Mendes", email = "dona@skala.com", celular = "11987654321", ehDona = true),
            senhaHash = hash("skala123"),
        ),
    )

    private var armazenamento: Armazenamento? = null
    private var logado: String? = null

    /** Lê as contas e a sessão salvas. Na primeira vez, salva as duas contas de teste. */
    fun iniciar(context: Context) {
        if (armazenamento != null) return
        val a = Armazenamento(context, "skala_contas")
        armazenamento = a
        val salvas = a.ler(K_CONTAS)
        if (salvas == null) {
            salvarContas()
        } else {
            runCatching {
                val lista = JSONArray(salvas)
                val lidas = (0 until lista.length()).associate { i ->
                    val o = lista.getJSONObject(i)
                    val u = Usuario(o.getString("nome"), o.getString("email"), o.getString("celular"), o.optBoolean("ehDona"))
                    u.email to Conta(u, o.getString("senhaHash"))
                }
                contas.clear()
                contas.putAll(lidas)
            }
        }
        logado = a.ler(K_LOGADO)?.takeIf { it in contas }
    }

    private fun salvarContas() {
        val lista = JSONArray().apply {
            contas.values.forEach { c ->
                put(
                    JSONObject()
                        .put("nome", c.usuario.nome).put("email", c.usuario.email)
                        .put("celular", c.usuario.celular).put("ehDona", c.usuario.ehDona)
                        .put("senhaHash", c.senhaHash),
                )
            }
        }
        armazenamento?.gravar(K_CONTAS, lista.toString())
    }

    private fun lembrarSessao(email: String?) {
        logado = email
        armazenamento?.gravar(K_LOGADO, email)
    }

    override fun usuarioLogado(): Usuario? = logado?.let { contas[it]?.usuario }

    override fun sair() = lembrarSessao(null)

    override suspend fun entrar(email: String, senha: String): Result<Usuario> {
        delay(ATRASO_MS)
        val conta = contas[email.lowercase()]
        return if (conta != null && conta.senhaHash == hash(senha)) {
            lembrarSessao(conta.usuario.email)
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
        contas[chave] = Conta(usuario, hash(dados.senha))
        salvarContas()
        lembrarSessao(chave)
        return Result.success(usuario)
    }

    override suspend fun atualizar(usuario: Usuario): Result<Usuario> {
        val chave = usuario.email.lowercase()
        val conta = contas[chave] ?: return Result.failure(AuthErro.CredenciaisInvalidas())
        val atualizado = conta.usuario.copy(nome = usuario.nome, celular = usuario.celular)
        contas[chave] = conta.copy(usuario = atualizado)
        salvarContas()
        return Result.success(atualizado)
    }
}
