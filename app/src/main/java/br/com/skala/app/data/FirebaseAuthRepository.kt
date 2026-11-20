package br.com.skala.app.data

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Contas de verdade com o Firebase Authentication: e-mail e senha, ou celular com código por SMS.
 * O perfil (nome, celular, e-mail) fica em `usuarios/{uid}` no Firestore. A dona é a conta cujo
 * uid está em `config/salao.donaUid`.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    private var verificacaoId: String? = null
    private var reenvio: PhoneAuthProvider.ForceResendingToken? = null
    private var celularPendente: String? = null

    private fun perfilRef(uid: String) = db.collection(Fs.USUARIOS).document(uid)

    private suspend fun ehDona(uid: String): Boolean = runCatching {
        db.collection(Fs.CONFIG).document(Fs.SALAO).get().await().getString(Fs.DONA_UID) == uid
    }.getOrDefault(false)

    /** null quando a conta existe mas ainda não tem perfil salvo. */
    private suspend fun lerPerfil(user: FirebaseUser): Usuario? {
        val doc = perfilRef(user.uid).get().await()
        if (!doc.exists()) return null
        return Usuario(
            nome = doc.getString("nome").orEmpty(),
            email = doc.getString("email").orEmpty(),
            celular = doc.getString("celular").orEmpty(),
            ehDona = ehDona(user.uid),
            uid = user.uid,
        )
    }

    private suspend fun salvarPerfil(uid: String, nome: String, email: String, celular: String): Usuario {
        perfilRef(uid).set(
            mapOf("nome" to nome, "email" to email, "celular" to celular, "criadoEm" to FieldValue.serverTimestamp()),
        ).await()
        return Usuario(nome = nome, email = email, celular = celular, ehDona = ehDona(uid), uid = uid)
    }

    override suspend fun entrar(email: String, senha: String): Result<Usuario> = tentar {
        val emailNormal = email.trim().lowercase()
        val user = auth.signInWithEmailAndPassword(emailNormal, senha).await().user
            ?: throw AuthErro.CredenciaisInvalidas()
        // Sem perfil (o cadastro caiu no meio): cria um com o que se sabe
        lerPerfil(user) ?: salvarPerfil(user.uid, emailNormal.substringBefore('@'), emailNormal, "")
    }

    override suspend fun cadastrar(dados: DadosCadastro): Result<Usuario> = tentar {
        val email = dados.email.trim().lowercase()
        val user = auth.createUserWithEmailAndPassword(email, dados.senha).await().user
            ?: throw IllegalStateException("Conta criada sem usuário")
        salvarPerfil(user.uid, dados.nome, email, dados.celular)
    }

    override suspend fun enviarCodigoSms(celular: String, activity: Activity): Result<PassoCelular> = tentar {
        if (celular.length < 10) throw AuthErro.CelularInvalido()
        val mesmoNumero = celular == celularPendente
        celularPendente = celular
        val resposta = suspendCancellableCoroutine<Any> { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                // O Android leu o SMS sozinho (ou o número foi verificado sem SMS): entra direto
                override fun onVerificationCompleted(credencial: PhoneAuthCredential) {
                    if (cont.isActive) cont.resume(credencial)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificacaoId = id
                    reenvio = token
                    if (cont.isActive) cont.resume(PassoCelular.CodigoEnviado)
                }
            }
            val opcoes = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+55$celular")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            reenvio?.takeIf { mesmoNumero }?.let(opcoes::setForceResendingToken)
            PhoneAuthProvider.verifyPhoneNumber(opcoes.build())
        }
        if (resposta is PhoneAuthCredential) entrarComCredencial(resposta) else PassoCelular.CodigoEnviado
    }

    override suspend fun confirmarCodigoSms(codigo: String): Result<PassoCelular> = tentar {
        val id = verificacaoId ?: throw AuthErro.CodigoInvalido()
        entrarComCredencial(PhoneAuthProvider.getCredential(id, codigo.trim()))
    }

    private suspend fun entrarComCredencial(credencial: PhoneAuthCredential): PassoCelular {
        val user = auth.signInWithCredential(credencial).await().user ?: throw AuthErro.CodigoInvalido()
        verificacaoId = null
        lerPerfil(user)?.let { return PassoCelular.Entrou(it) }
        val celular = user.phoneNumber?.removePrefix("+55") ?: celularPendente.orEmpty()
        // Quem já criou conta com e-mail usando este celular entra com o mesmo nome e e-mail
        // (os horários são ligados ao celular, então ela vê os mesmos agendamentos).
        val existente = runCatching {
            db.collection(Fs.USUARIOS).whereEqualTo("celular", celular).limit(1).get().await().documents.firstOrNull()
        }.getOrNull()
        if (existente != null) {
            val perfil = salvarPerfil(user.uid, existente.getString("nome").orEmpty(), existente.getString("email").orEmpty(), celular)
            return PassoCelular.Entrou(perfil)
        }
        return PassoCelular.FaltaNome
    }

    override suspend fun concluirCadastroCelular(nome: String): Result<Usuario> = tentar {
        val user = auth.currentUser ?: throw AuthErro.CodigoInvalido()
        val celular = user.phoneNumber?.removePrefix("+55") ?: celularPendente.orEmpty()
        salvarPerfil(user.uid, nome, "", celular).also { celularPendente = null }
    }

    override suspend fun recuperarSenha(email: String): Result<Unit> = tentar {
        auth.sendPasswordResetEmail(email.trim().lowercase()).await()
        Unit
    }

    override suspend fun atualizar(usuario: Usuario): Result<Usuario> = tentar {
        val user = auth.currentUser ?: throw AuthErro.CredenciaisInvalidas()
        // Sem esperar o servidor: o Firestore guarda a mudança e envia quando houver internet.
        perfilRef(user.uid).update(mapOf("nome" to usuario.nome, "celular" to usuario.celular))
            .addOnFailureListener { Log.w(TAG, "Não foi possível salvar o perfil", it) }
        usuario
    }

    override suspend fun usuarioLogado(): Usuario? {
        val user = auth.currentUser ?: return null
        val perfil = runCatching { lerPerfil(user) }
        // Sem internet e sem cópia no celular: pede login de novo, mas não desconecta a conta.
        if (perfil.isFailure) return null
        // Entrou pelo celular e não chegou a informar o nome: começa de novo.
        return perfil.getOrNull() ?: run { auth.signOut(); null }
    }

    override fun sair() {
        auth.signOut()
        verificacaoId = null
        celularPendente = null
    }

    private inline fun <T> tentar(bloco: () -> T): Result<T> =
        runCatching(bloco).fold(onSuccess = { Result.success(it) }, onFailure = { Result.failure(traduzir(it)) })

    private fun traduzir(e: Throwable): Throwable {
        Log.w(TAG, "Falha na autenticação", e)
        return when (e) {
            is AuthErro -> e
            is FirebaseAuthUserCollisionException -> AuthErro.EmailJaCadastrado()
            is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
                "ERROR_INVALID_PHONE_NUMBER" -> AuthErro.CelularInvalido()
                "ERROR_INVALID_VERIFICATION_CODE", "ERROR_INVALID_VERIFICATION_ID", "ERROR_SESSION_EXPIRED" -> AuthErro.CodigoInvalido()
                else -> AuthErro.CredenciaisInvalidas()
            }
            is FirebaseAuthInvalidUserException -> AuthErro.CredenciaisInvalidas()
            is FirebaseTooManyRequestsException -> AuthErro.MuitasTentativas()
            is FirebaseNetworkException -> AuthErro.SemConexao()
            is FirebaseFirestoreException ->
                if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthErro.SemConexao() else e
            else -> e
        }
    }

    private companion object {
        const val TAG = "FirebaseAuth"
    }
}
