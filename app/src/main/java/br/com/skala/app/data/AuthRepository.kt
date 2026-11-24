package br.com.skala.app.data

import android.app.Activity
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
    /** Identificador da conta (no Firebase, o uid). */
    val uid: String = "",
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
    class CodigoInvalido : AuthErro()
    class CelularInvalido : AuthErro()
    class MuitasTentativas : AuthErro()
    class SemConexao : AuthErro()
}

/** Em que pé está o login pelo celular. */
sealed interface PassoCelular {
    /** O SMS foi enviado: falta a pessoa digitar o código. */
    data object CodigoEnviado : PassoCelular
    /** Entrou (o código foi aceito, ou o Android leu o SMS sozinho). */
    data class Entrou(val usuario: Usuario) : PassoCelular
    /** Número novo, sem cadastro: falta só o nome para criar a conta. */
    data object FaltaNome : PassoCelular
}

/**
 * Login e cadastro. Há duas versões: [FirebaseAuthRepository] (contas de verdade, usado quando o
 * projeto tem o google-services.json) e [FakeAuthRepository] (só no celular, para testar).
 */
interface AuthRepository {
    suspend fun entrar(email: String, senha: String): Result<Usuario>
    suspend fun cadastrar(dados: DadosCadastro): Result<Usuario>

    /** Manda o SMS com o código. `celular`: só dígitos, com DDD. Precisa da tela aberta (`activity`). */
    suspend fun enviarCodigoSms(celular: String, activity: Activity): Result<PassoCelular>

    /** Confere o código do SMS. Resulta em [PassoCelular.Entrou] ou [PassoCelular.FaltaNome]. */
    suspend fun confirmarCodigoSms(codigo: String): Result<PassoCelular>

    /** Termina o cadastro de quem entrou pelo celular pela primeira vez. */
    suspend fun concluirCadastroCelular(nome: String): Result<Usuario>

    /** Manda o e-mail para criar uma senha nova. */
    suspend fun recuperarSenha(email: String): Result<Unit>

    /** Atualiza nome e celular. O e-mail é o login e não muda por aqui. */
    suspend fun atualizar(usuario: Usuario): Result<Usuario>

    /** Quem estava logado da última vez (para não pedir login ao abrir o app de novo). */
    suspend fun usuarioLogado(): Usuario?

    /** Esquece a sessão salva. */
    fun sair()
}

/**
 * Autenticação local, para testar sem API. Já existem duas contas de teste (cliente e dona do salão).
 * As contas criadas no cadastro, as mudanças de nome/celular e a sessão ficam salvas no celular
 * (a senha só como hash SHA-256). Precisa de [iniciar] (feito na MainActivity) para ler e gravar.
 * No login pelo celular nenhum SMS é enviado: o código é sempre [CODIGO_TESTE].
 */
object FakeAuthRepository : AuthRepository {
    private const val ATRASO_MS = 1200L
    const val CODIGO_TESTE = "123456"

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
                    chaveDe(u) to Conta(u, o.getString("senhaHash"))
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

    override suspend fun usuarioLogado(): Usuario? = logado?.let { contas[it]?.usuario }

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

    /** Contas criadas pelo celular não têm e-mail: a chave delas é "tel:" + celular. */
    private fun chaveDe(usuario: Usuario) = usuario.email.lowercase().ifEmpty { "tel:${usuario.celular}" }

    private var celularPendente: String? = null

    override suspend fun enviarCodigoSms(celular: String, activity: Activity): Result<PassoCelular> {
        delay(ATRASO_MS)
        if (celular.length < 10) return Result.failure(AuthErro.CelularInvalido())
        celularPendente = celular
        return Result.success(PassoCelular.CodigoEnviado)
    }

    override suspend fun confirmarCodigoSms(codigo: String): Result<PassoCelular> {
        delay(ATRASO_MS)
        val celular = celularPendente ?: return Result.failure(AuthErro.CodigoInvalido())
        if (codigo != CODIGO_TESTE) return Result.failure(AuthErro.CodigoInvalido())
        val conta = contas.entries.firstOrNull { it.value.usuario.celular == celular }
            ?: return Result.success(PassoCelular.FaltaNome)
        lembrarSessao(conta.key)
        return Result.success(PassoCelular.Entrou(conta.value.usuario))
    }

    override suspend fun concluirCadastroCelular(nome: String): Result<Usuario> {
        val celular = celularPendente ?: return Result.failure(AuthErro.CodigoInvalido())
        val usuario = Usuario(nome = nome, email = "", celular = celular)
        val chave = chaveDe(usuario)
        contas[chave] = Conta(usuario, senhaHash = "")
        salvarContas()
        lembrarSessao(chave)
        celularPendente = null
        return Result.success(usuario)
    }

    override suspend fun recuperarSenha(email: String): Result<Unit> {
        delay(ATRASO_MS)
        return Result.success(Unit)
    }

    override suspend fun atualizar(usuario: Usuario): Result<Usuario> {
        val chave = chaveDe(usuario)
        val conta = contas[chave] ?: return Result.failure(AuthErro.CredenciaisInvalidas())
        val atualizado = conta.usuario.copy(nome = usuario.nome, celular = usuario.celular)
        contas[chave] = conta.copy(usuario = atualizado)
        salvarContas()
        return Result.success(atualizado)
    }
}
