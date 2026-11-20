package br.com.skala.app.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.LocalTime

/** Sem internet para uma operação que precisa do servidor (como conferir se o horário está livre). */
class SemConexao : Exception()

/**
 * Dados do salão no Cloud Firestore, compartilhados entre a dona e as clientes. As telas leem os
 * StateFlows, que o Firestore atualiza sozinho quando algo muda (inclusive em outro celular).
 * Gravações simples funcionam sem internet: o Firestore guarda e envia depois.
 * A estrutura das coleções está em [Fs]; as regras de acesso em firestore.rules.
 */
class FirebaseSalaoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : SalaoRepository {

    override val servicos = Catalogo.servicos

    private val _dias = MutableStateFlow<Map<LocalDate, DiaFuncionamento>>(emptyMap())
    override val dias: StateFlow<Map<LocalDate, DiaFuncionamento>> = _dias.asStateFlow()

    private val _agendamentos = MutableStateFlow<List<Agendamento>>(emptyList())
    override val agendamentos: StateFlow<List<Agendamento>> = _agendamentos.asStateFlow()

    private val _dadosSalao = MutableStateFlow(DadosSalao())
    override val dadosSalao: StateFlow<DadosSalao> = _dadosSalao.asStateFlow()

    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    override val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _avisosDona = MutableStateFlow<List<AvisoDona>>(emptyList())
    override val avisosDona: StateFlow<List<AvisoDona>> = _avisosDona.asStateFlow()

    /** Horários ocupados de cada dia, por id do agendamento (sem dados das clientes). */
    private val _ocupacoes = MutableStateFlow<Map<LocalDate, Map<String, Ocupacao>>>(emptyMap())

    private val escutas = mutableListOf<ListenerRegistration>()
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var limpeza: Job? = null
    /** uid e papel de quem está conectado, para não reconectar quando só o nome muda. */
    private var conectado: Pair<String, Boolean>? = null

    private fun agendamentoRef(id: String) = db.collection(Fs.AGENDAMENTOS).document(id)
    private fun ocupacaoRef(data: LocalDate) = db.collection(Fs.OCUPACOES).document(data.toString())
    private fun diaRef(data: LocalDate) = db.collection(Fs.DIAS).document(data.toString())

    // ---------- acompanhar os dados ----------

    override fun conectar(usuario: Usuario?) {
        val chave = usuario?.let { it.uid to it.ehDona }
        if (chave == conectado && (chave == null || escutas.isNotEmpty())) return
        escutas.forEach { it.remove() }
        escutas.clear()
        limpeza?.cancel()
        _dias.value = emptyMap()
        _agendamentos.value = emptyList()
        _dadosSalao.value = DadosSalao()
        _clientes.value = emptyList()
        _avisosDona.value = emptyList()
        _ocupacoes.value = emptyMap()
        conectado = chave
        if (usuario == null) return

        val hoje = LocalDate.now()
        val desde = hoje.minusDays(DIAS_GUARDADOS)

        escutas += db.collection(Fs.CONFIG).document(Fs.SALAO).addSnapshotListener { doc, erro ->
            if (erro != null) return@addSnapshotListener avisarErro("config", erro)
            _dadosSalao.value = Fs.lerSalao(doc?.data) ?: DadosSalao()
        }
        escutarLista(
            db.collection(Fs.DIAS).whereGreaterThanOrEqualTo(FieldPath.documentId(), (if (usuario.ehDona) desde else hoje).toString()),
        ) { docs -> _dias.value = docs.mapNotNull { Fs.lerDia(it.id, it.data) }.associateBy { it.data } }
        escutarLista(
            db.collection(Fs.OCUPACOES).whereGreaterThanOrEqualTo(FieldPath.documentId(), hoje.toString()),
        ) { docs ->
            _ocupacoes.value = docs.mapNotNull { d ->
                runCatching { LocalDate.parse(d.id) to Fs.lerOcupacoes(d.data) }.getOrNull()
            }.toMap()
        }

        if (usuario.ehDona) {
            escutarLista(db.collection(Fs.AGENDAMENTOS)) { docs ->
                _agendamentos.value = docs.mapNotNull { Fs.lerAgendamento(it.id, it.data.orEmpty(), servicos) }
            }
            escutarLista(db.collection(Fs.CLIENTES)) { docs ->
                _clientes.value = docs.mapNotNull { Fs.lerCliente(it.data) }.sortedBy { it.nome.lowercase() }
            }
            escutarLista(db.collection(Fs.AVISOS).orderBy("quando", Query.Direction.DESCENDING).limit(100)) { docs ->
                _avisosDona.value = docs.mapNotNull { Fs.lerAviso(it.id, it.data, servicos) }
            }
            limpeza = escopo.launch { apagarAntigos() }
        } else {
            escutarAgendamentosDaCliente(usuario)
        }
    }

    /**
     * A cliente vê os horários marcados na conta dela, no e-mail dela ou no celular dela (quando ela
     * entrou pelo celular, que é confirmado por SMS). Cada consulta é permitida pelas regras do banco.
     */
    private fun escutarAgendamentosDaCliente(usuario: Usuario) {
        val porConsulta = mutableMapOf<String, List<Agendamento>>()
        fun publicar() {
            _agendamentos.value = porConsulta.values.flatten().distinctBy { it.id }
        }
        val colecao = db.collection(Fs.AGENDAMENTOS)
        val consultas = buildMap {
            put("uid", colecao.whereEqualTo("clienteUid", usuario.uid))
            auth.currentUser?.email?.takeIf { it.isNotEmpty() }?.let { put("email", colecao.whereEqualTo("clienteEmail", it.lowercase())) }
            auth.currentUser?.phoneNumber?.takeIf { it.startsWith("+55") }?.let {
                put("celular", colecao.whereEqualTo("clienteCelular", it.removePrefix("+55")))
            }
        }
        consultas.forEach { (nome, consulta) ->
            escutarLista(consulta) { docs ->
                porConsulta[nome] = docs.mapNotNull { Fs.lerAgendamento(it.id, it.data.orEmpty(), servicos) }
                publicar()
            }
        }
    }

    private fun escutarLista(consulta: Query, aoMudar: (List<DocumentSnapshot>) -> Unit) {
        escutas += consulta.addSnapshotListener { snap, erro ->
            if (erro != null) return@addSnapshotListener avisarErro("consulta", erro)
            snap?.let { aoMudar(it.documents) }
        }
    }

    private fun avisarErro(onde: String, erro: Exception) {
        Log.w(TAG, "Falha ao ler $onde", erro)
    }

    /**
     * Apaga o que passou de [DIAS_GUARDADOS] dias (campo `apagarEm` vencido). Roda quando a dona entra.
     * Se a política de TTL do Firestore estiver ligada, ela faz o mesmo sozinha; aqui é a garantia.
     */
    private suspend fun apagarAntigos() {
        val agora = Timestamp.now()
        for (colecao in listOf(Fs.AGENDAMENTOS, Fs.OCUPACOES, Fs.AVISOS, Fs.DIAS)) {
            runCatching {
                while (true) {
                    val docs = db.collection(colecao).whereLessThan(Fs.APAGAR_EM, agora).limit(LOTE.toLong()).get().await().documents
                    if (docs.isEmpty()) break
                    db.batch().apply { docs.forEach { delete(it.reference) } }.commit().await()
                    Log.i(TAG, "Apagados ${docs.size} documentos antigos de $colecao")
                    if (docs.size < LOTE) break
                }
            }.onFailure { Log.w(TAG, "Não foi possível apagar os antigos de $colecao", it) }
        }
    }

    // ---------- salão e clientes ----------

    override fun salvarDadosSalao(dados: DadosSalao) {
        _dadosSalao.value = dados
        db.collection(Fs.CONFIG).document(Fs.SALAO).set(Fs.salao(dados), SetOptions.merge()).logarFalha("salão")
    }

    override fun cadastrarCliente(cliente: Cliente): Result<Cliente> {
        _clientes.value.firstOrNull { it.celular == cliente.celular }?.let {
            return Result.failure(ClienteJaCadastrada(it))
        }
        _clientes.value = (_clientes.value + cliente).sortedBy { it.nome.lowercase() }
        db.collection(Fs.CLIENTES).document(cliente.celular).set(Fs.cliente(cliente)).logarFalha("cliente")
        return Result.success(cliente)
    }

    override fun registrarClienteDoApp(cliente: Cliente) {
        if (cliente.celular.isEmpty()) return
        db.collection(Fs.CLIENTES).document(cliente.celular).set(Fs.cliente(cliente), SetOptions.merge()).logarFalha("cliente do app")
    }

    // ---------- avisos para a dona ----------

    override fun registrarAvisoDona(tipo: TipoAvisoDona, agendamento: Agendamento, anterior: Agendamento?): AvisoDona {
        val ref = db.collection(Fs.AVISOS).document()
        val aviso = AvisoDona(ref.id, tipo, agendamento, anterior)
        ref.set(Fs.aviso(aviso)).logarFalha("aviso")
        return aviso
    }

    override fun marcarAvisosLidos() {
        val naoLidos = _avisosDona.value.filterNot { it.lido }
        if (naoLidos.isEmpty()) return
        db.batch().apply {
            naoLidos.forEach { update(db.collection(Fs.AVISOS).document(it.id), "lido", true) }
        }.commit().logarFalha("avisos lidos")
    }

    // ---------- dias e horários ----------

    override fun dia(data: LocalDate): DiaFuncionamento = _dias.value[data] ?: DiaFuncionamento(data)

    override fun salvarDia(dia: DiaFuncionamento) {
        _dias.value = _dias.value + (dia.data to dia)
        diaRef(dia.data).set(Fs.dia(dia)).logarFalha("dia")
    }

    override fun horariosLivres(data: LocalDate, duracaoMin: Int, ignorarAgendamentoId: String?): List<LocalTime> {
        val ocupados = _ocupacoes.value[data].orEmpty().filterKeys { it != ignorarAgendamentoId }.values
        return calcularHorariosLivres(dia(data), ocupados, duracaoMin)
    }

    override suspend fun agendar(agendamento: Agendamento, substituirId: String?): Result<Agendamento> {
        val ref = db.collection(Fs.AGENDAMENTOS).document()
        val novo = agendamento.copy(id = ref.id)
        val antigo = substituirId?.let { id -> _agendamentos.value.firstOrNull { it.id == id } }
        return try {
            withTimeout(TEMPO_LIMITE_MS) {
                // Transação: lê o dia e os horários ocupados no servidor e só grava se ainda couber.
                // Duas clientes confirmando o mesmo horário ao mesmo tempo: só uma consegue.
                db.runTransaction { t ->
                    val dia = Fs.lerDia(novo.data.toString(), t.get(diaRef(novo.data)).data) ?: DiaFuncionamento(novo.data)
                    val ocupados = Fs.lerOcupacoes(t.get(ocupacaoRef(novo.data)).data).filterKeys { it != substituirId }.values
                    if (novo.inicio !in calcularHorariosLivres(dia, ocupados, novo.duracaoMin)) throw HorarioIndisponivel()

                    t.set(ref, Fs.agendamento(novo))
                    val horariosDoDia = mutableMapOf<String, Any>(novo.id to Fs.ocupacao(novo))
                    if (antigo != null) {
                        t.update(agendamentoRef(antigo.id), "status", StatusAgendamento.CANCELADO.name)
                        if (antigo.data == novo.data) {
                            horariosDoDia[antigo.id] = FieldValue.delete()
                        } else {
                            t.set(ocupacaoRef(antigo.data), mapOf(Fs.HORARIOS to mapOf(antigo.id to FieldValue.delete())), SetOptions.merge())
                        }
                    }
                    t.set(
                        ocupacaoRef(novo.data),
                        mapOf(Fs.HORARIOS to horariosDoDia, Fs.APAGAR_EM to Fs.apagarEm(novo.data)),
                        SetOptions.merge(),
                    )
                    null
                }.await()
            }
            Result.success(novo)
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível agendar", e)
            Result.failure(
                when {
                    generateSequence<Throwable>(e) { it.cause }.any { it is HorarioIndisponivel } -> HorarioIndisponivel()
                    e is TimeoutCancellationException -> SemConexao()
                    e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> SemConexao()
                    else -> e
                },
            )
        }
    }

    override fun cancelar(id: String) {
        val agendamento = _agendamentos.value.firstOrNull { it.id == id } ?: return
        _agendamentos.value = _agendamentos.value.map { if (it.id == id) it.copy(status = StatusAgendamento.CANCELADO) else it }
        db.batch().apply {
            update(agendamentoRef(id), "status", StatusAgendamento.CANCELADO.name)
            set(ocupacaoRef(agendamento.data), mapOf(Fs.HORARIOS to mapOf(id to FieldValue.delete())), SetOptions.merge())
        }.commit().logarFalha("cancelamento")
    }

    private fun com.google.android.gms.tasks.Task<*>.logarFalha(oque: String) {
        addOnFailureListener { Log.w(TAG, "Não foi possível salvar $oque", it) }
    }

    private companion object {
        const val TAG = "FirebaseSalao"
        const val LOTE = 400
        const val TEMPO_LIMITE_MS = 15_000L
    }
}
