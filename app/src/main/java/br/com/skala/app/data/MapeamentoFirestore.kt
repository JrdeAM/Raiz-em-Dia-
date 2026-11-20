package br.com.skala.app.data

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Nomes das coleções do Firestore e conversão dos dados do app para documentos e de volta.
 * Datas ficam como texto "2026-11-20" e horas como "09:30", que é fácil de ler no console.
 *
 * Estrutura do banco:
 *  - usuarios/{uid}            nome, email, celular
 *  - config/salao              nome, endereco, whatsapp, donaUid (uid da conta da dona)
 *  - clientes/{celular}        nome, celular, email                  (só a dona lê)
 *  - dias/{data}               status, faixas, intervaloMin, bloqueios, apagarEm
 *  - agendamentos/{id}         dados do horário e da cliente, apagarEm
 *  - ocupacoes/{data}          horarios: {id: {inicio, fim}}, apagarEm  (sem dados pessoais:
 *                              é o que a cliente lê para saber quais horários estão livres)
 *  - avisos/{id}               avisos para a dona, apagarEm          (só a dona lê)
 *
 * `apagarEm`: data em que o documento passa de [DIAS_GUARDADOS] dias e pode ser apagado.
 * Com a política de TTL do Firestore ligada nesse campo, o próprio Firebase apaga (ver FIREBASE.md).
 */
internal object Fs {
    const val USUARIOS = "usuarios"
    const val CONFIG = "config"
    const val SALAO = "salao"
    const val CLIENTES = "clientes"
    const val DIAS = "dias"
    const val AGENDAMENTOS = "agendamentos"
    const val OCUPACOES = "ocupacoes"
    const val AVISOS = "avisos"

    const val APAGAR_EM = "apagarEm"
    const val HORARIOS = "horarios"
    const val DONA_UID = "donaUid"

    private val fuso: ZoneId get() = ZoneId.systemDefault()

    fun timestamp(momento: LocalDateTime): Timestamp {
        val instante = momento.atZone(fuso).toInstant()
        return Timestamp(instante.epochSecond, instante.nano)
    }

    fun dataHora(ts: Timestamp): LocalDateTime =
        Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong()).atZone(fuso).toLocalDateTime()

    fun apagarEm(data: LocalDate): Timestamp = timestamp(apagarDepoisDe(data))

    // ---------- leitura segura de campos ----------

    private fun Map<String, Any?>.texto(campo: String): String = this[campo] as? String ?: ""
    private fun Map<String, Any?>.inteiro(campo: String, padrao: Int): Int = (this[campo] as? Number)?.toInt() ?: padrao

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.mapa(campo: String): Map<String, Any?>? = this[campo] as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.listaDeMapas(campo: String): List<Map<String, Any?>> =
        (this[campo] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()

    // ---------- agendamento ----------

    private fun cabelo(c: DadosCabelo): Map<String, Any?> = mapOf(
        "comprimento" to c.comprimento?.name,
        "tipo" to c.tipo?.name,
        "fezQuimica" to c.fezQuimica,
    )

    private fun lerCabelo(m: Map<String, Any?>) = DadosCabelo(
        comprimento = m.texto("comprimento").takeIf { it.isNotEmpty() }?.let { runCatching { Comprimento.valueOf(it) }.getOrNull() },
        tipo = m.texto("tipo").takeIf { it.isNotEmpty() }?.let { runCatching { TipoCabelo.valueOf(it) }.getOrNull() },
        fezQuimica = m["fezQuimica"] as? Boolean,
    )

    fun agendamento(a: Agendamento): Map<String, Any?> = mapOf(
        "id" to a.id,
        "clienteNome" to a.clienteNome,
        "clienteEmail" to a.clienteEmail.lowercase(),
        "clienteCelular" to a.clienteCelular,
        "clienteUid" to a.clienteUid,
        "servicos" to a.servicos.map { it.id },
        "data" to a.data.toString(),
        "inicio" to a.inicio.toString(),
        "fim" to a.fim.toString(),
        "cabelo" to a.cabelo?.let(::cabelo),
        "observacao" to a.observacao,
        "status" to a.status.name,
        "reservadoPelaDona" to a.reservadoPelaDona,
        APAGAR_EM to apagarEm(a.data),
    )

    /** `servicos`: catálogo atual; serviços que não existem mais são deixados de fora. */
    fun lerAgendamento(id: String, m: Map<String, Any?>, servicos: List<Servico>): Agendamento? = runCatching {
        val ids = (m["servicos"] as? List<*>).orEmpty().filterIsInstance<String>()
        val lista = ids.mapNotNull { sid -> servicos.firstOrNull { it.id == sid } }
        if (lista.isEmpty()) return null
        Agendamento(
            id = id,
            clienteNome = m.texto("clienteNome"),
            clienteEmail = m.texto("clienteEmail"),
            servicos = lista,
            data = LocalDate.parse(m.texto("data")),
            inicio = LocalTime.parse(m.texto("inicio")),
            cabelo = m.mapa("cabelo")?.let(::lerCabelo),
            observacao = m.texto("observacao"),
            status = StatusAgendamento.valueOf(m.texto("status")),
            clienteCelular = m.texto("clienteCelular"),
            reservadoPelaDona = m["reservadoPelaDona"] as? Boolean ?: false,
            clienteUid = m.texto("clienteUid"),
        )
    }.getOrNull()

    fun ocupacao(a: Agendamento): Map<String, Any?> = mapOf("inicio" to a.inicio.toString(), "fim" to a.fim.toString())

    /** Lê o campo `horarios` de um documento de ocupacoes: id do agendamento → início e fim. */
    fun lerOcupacoes(m: Map<String, Any?>?): Map<String, Ocupacao> {
        val horarios = m?.mapa(HORARIOS) ?: return emptyMap()
        return horarios.mapNotNull { (id, valor) ->
            @Suppress("UNCHECKED_CAST")
            val h = valor as? Map<String, Any?> ?: return@mapNotNull null
            runCatching { id to Ocupacao(LocalTime.parse(h.texto("inicio")), LocalTime.parse(h.texto("fim"))) }.getOrNull()
        }.toMap()
    }

    // ---------- dia de funcionamento ----------

    fun dia(d: DiaFuncionamento): Map<String, Any?> = mapOf(
        "data" to d.data.toString(),
        "status" to d.status.name,
        "intervaloMin" to d.intervaloMin,
        "faixas" to d.faixas.map { mapOf("inicio" to it.inicio.toString(), "fim" to it.fim.toString()) },
        "bloqueios" to d.bloqueios.map { mapOf("inicio" to it.inicio.toString(), "fim" to it.fim.toString(), "motivo" to it.motivo) },
        APAGAR_EM to apagarEm(d.data),
    )

    fun lerDia(id: String, m: Map<String, Any?>?): DiaFuncionamento? = runCatching {
        m ?: return null
        DiaFuncionamento(
            data = LocalDate.parse(id),
            status = StatusDia.valueOf(m.texto("status")),
            faixas = m.listaDeMapas("faixas").map { Faixa(LocalTime.parse(it.texto("inicio")), LocalTime.parse(it.texto("fim"))) },
            intervaloMin = m.inteiro("intervaloMin", 15),
            bloqueios = m.listaDeMapas("bloqueios").map {
                Bloqueio(LocalTime.parse(it.texto("inicio")), LocalTime.parse(it.texto("fim")), it.texto("motivo"))
            },
        )
    }.getOrNull()

    // ---------- salão, clientes e avisos ----------

    fun salao(s: DadosSalao): Map<String, Any?> = mapOf("nome" to s.nome, "endereco" to s.endereco, "whatsapp" to s.whatsapp)

    fun lerSalao(m: Map<String, Any?>?): DadosSalao? {
        if (m == null || m.texto("nome").isEmpty()) return null
        return DadosSalao(m.texto("nome"), m.texto("endereco"), m.texto("whatsapp"))
    }

    fun cliente(c: Cliente): Map<String, Any?> = mapOf("nome" to c.nome, "celular" to c.celular, "email" to c.email.lowercase())

    fun lerCliente(m: Map<String, Any?>?): Cliente? {
        if (m == null || m.texto("celular").isEmpty()) return null
        return Cliente(m.texto("nome"), m.texto("celular"), m.texto("email"))
    }

    fun aviso(a: AvisoDona): Map<String, Any?> = mapOf(
        "tipo" to a.tipo.name,
        "agendamento" to agendamento(a.agendamento),
        "anterior" to a.anterior?.let(::agendamento),
        "quando" to timestamp(a.quando),
        "lido" to a.lido,
        APAGAR_EM to apagarEm(a.quando.toLocalDate()),
    )

    fun lerAviso(id: String, m: Map<String, Any?>?, servicos: List<Servico>): AvisoDona? = runCatching {
        m ?: return null
        val ag = m.mapa("agendamento")?.let { lerAgendamento(it.texto("id"), it, servicos) } ?: return null
        AvisoDona(
            id = id,
            tipo = TipoAvisoDona.valueOf(m.texto("tipo")),
            agendamento = ag,
            anterior = m.mapa("anterior")?.let { lerAgendamento(it.texto("id"), it, servicos) },
            quando = (m["quando"] as? Timestamp)?.let(::dataHora) ?: LocalDateTime.now(),
            lido = m["lido"] as? Boolean ?: false,
        )
    }.getOrNull()
}
