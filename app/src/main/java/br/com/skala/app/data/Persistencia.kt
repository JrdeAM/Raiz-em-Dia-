package br.com.skala.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Guarda os dados do app no próprio celular (SharedPreferences, em JSON), para não perder
 * nada ao fechar o app. Quando a API existir, os repositórios passam a ler e gravar nela.
 */
internal class Armazenamento(context: Context, nome: String) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(nome, Context.MODE_PRIVATE)

    fun ler(chave: String): String? = prefs.getString(chave, null)

    fun gravar(valores: Map<String, String>) {
        prefs.edit().apply { valores.forEach { (k, v) -> putString(k, v) } }.apply()
    }

    fun gravar(chave: String, valor: String?) {
        prefs.edit().apply { if (valor == null) remove(chave) else putString(chave, valor) }.apply()
    }
}

/** Conversão dos dados do salão para JSON e de volta. Dados corrompidos são ignorados. */
internal object JsonSalao {

    // ---------- listas ----------

    fun <T> lista(itens: Collection<T>, conv: (T) -> JSONObject): String =
        JSONArray().apply { itens.forEach { put(conv(it)) } }.toString()

    fun <T> lerLista(json: String?, conv: (JSONObject) -> T?): List<T>? = json?.let {
        runCatching {
            val a = JSONArray(it)
            (0 until a.length()).mapNotNull { i -> runCatching { conv(a.getJSONObject(i)) }.getOrNull() }
        }.getOrNull()
    }

    // ---------- cabelo ----------

    private fun cabelo(c: DadosCabelo) = JSONObject().apply {
        c.comprimento?.let { put("comprimento", it.name) }
        c.tipo?.let { put("tipo", it.name) }
        c.fezQuimica?.let { put("fezQuimica", it) }
    }

    private fun lerCabelo(o: JSONObject) = DadosCabelo(
        comprimento = o.optString("comprimento").takeIf { it.isNotEmpty() }?.let { Comprimento.valueOf(it) },
        tipo = o.optString("tipo").takeIf { it.isNotEmpty() }?.let { TipoCabelo.valueOf(it) },
        fezQuimica = if (o.has("fezQuimica")) o.getBoolean("fezQuimica") else null,
    )

    // ---------- agendamento ----------

    fun agendamento(a: Agendamento) = JSONObject().apply {
        put("id", a.id)
        put("clienteNome", a.clienteNome)
        put("clienteEmail", a.clienteEmail)
        put("clienteCelular", a.clienteCelular)
        put("servicos", JSONArray(a.servicos.map { it.id }))
        put("data", a.data.toString())
        put("inicio", a.inicio.toString())
        a.cabelo?.let { put("cabelo", cabelo(it)) }
        put("observacao", a.observacao)
        put("status", a.status.name)
        put("reservadoPelaDona", a.reservadoPelaDona)
    }

    /** `servicos`: catálogo atual; serviços que não existem mais são deixados de fora. */
    fun lerAgendamento(o: JSONObject, servicos: List<Servico>): Agendamento? {
        val ids = o.getJSONArray("servicos")
        val lista = (0 until ids.length()).mapNotNull { i -> servicos.firstOrNull { it.id == ids.getString(i) } }
        if (lista.isEmpty()) return null
        return Agendamento(
            id = o.getString("id"),
            clienteNome = o.getString("clienteNome"),
            clienteEmail = o.optString("clienteEmail"),
            servicos = lista,
            data = LocalDate.parse(o.getString("data")),
            inicio = LocalTime.parse(o.getString("inicio")),
            cabelo = o.optJSONObject("cabelo")?.let(::lerCabelo),
            observacao = o.optString("observacao"),
            status = StatusAgendamento.valueOf(o.getString("status")),
            clienteCelular = o.optString("clienteCelular"),
            reservadoPelaDona = o.optBoolean("reservadoPelaDona"),
        )
    }

    // ---------- dia de funcionamento ----------

    fun dia(d: DiaFuncionamento) = JSONObject().apply {
        put("data", d.data.toString())
        put("status", d.status.name)
        put("intervaloMin", d.intervaloMin)
        put("faixas", JSONArray().apply {
            d.faixas.forEach { put(JSONObject().put("inicio", it.inicio.toString()).put("fim", it.fim.toString())) }
        })
        put("bloqueios", JSONArray().apply {
            d.bloqueios.forEach {
                put(JSONObject().put("inicio", it.inicio.toString()).put("fim", it.fim.toString()).put("motivo", it.motivo))
            }
        })
    }

    fun lerDia(o: JSONObject): DiaFuncionamento {
        val faixas = o.getJSONArray("faixas")
        val bloqueios = o.getJSONArray("bloqueios")
        return DiaFuncionamento(
            data = LocalDate.parse(o.getString("data")),
            status = StatusDia.valueOf(o.getString("status")),
            faixas = (0 until faixas.length()).map { i ->
                faixas.getJSONObject(i).let { Faixa(LocalTime.parse(it.getString("inicio")), LocalTime.parse(it.getString("fim"))) }
            },
            intervaloMin = o.optInt("intervaloMin", 15),
            bloqueios = (0 until bloqueios.length()).map { i ->
                bloqueios.getJSONObject(i).let {
                    Bloqueio(LocalTime.parse(it.getString("inicio")), LocalTime.parse(it.getString("fim")), it.optString("motivo"))
                }
            },
        )
    }

    // ---------- salão, clientes e avisos ----------

    fun salao(s: DadosSalao): String =
        JSONObject().put("nome", s.nome).put("endereco", s.endereco).put("whatsapp", s.whatsapp).toString()

    fun lerSalao(json: String?): DadosSalao? = json?.let {
        runCatching {
            val o = JSONObject(it)
            DadosSalao(o.getString("nome"), o.getString("endereco"), o.getString("whatsapp"))
        }.getOrNull()
    }

    fun cliente(c: Cliente) = JSONObject().put("nome", c.nome).put("celular", c.celular).put("email", c.email)

    fun lerCliente(o: JSONObject) = Cliente(o.getString("nome"), o.getString("celular"), o.optString("email"))

    fun aviso(a: AvisoDona) = JSONObject().apply {
        put("id", a.id)
        put("tipo", a.tipo.name)
        put("agendamento", agendamento(a.agendamento))
        a.anterior?.let { put("anterior", agendamento(it)) }
        put("quando", a.quando.toString())
        put("lido", a.lido)
    }

    fun lerAviso(o: JSONObject, servicos: List<Servico>): AvisoDona? {
        val ag = lerAgendamento(o.getJSONObject("agendamento"), servicos) ?: return null
        return AvisoDona(
            id = o.getString("id"),
            tipo = TipoAvisoDona.valueOf(o.getString("tipo")),
            agendamento = ag,
            anterior = o.optJSONObject("anterior")?.let { lerAgendamento(it, servicos) },
            quando = LocalDateTime.parse(o.getString("quando")),
            lido = o.optBoolean("lido"),
        )
    }
}
