package br.com.skala.app.avisos

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.DadosSalao
import br.com.skala.app.ui.dona.formatarCelular
import java.time.format.DateTimeFormatter

/**
 * Único ponto do app que fala com o WhatsApp. Hoje é [WhatsAppSimulado], que só escreve no log.
 * Quando houver uma API de verdade, basta criar outra implementação e trocar em [AvisosAgendamento].
 */
fun interface EnviadorWhatsApp {
    /** `celular` só com dígitos, já com o código do país (ex.: 5511912345678). */
    fun enviar(celular: String, mensagem: String)
}

/** Mensagem que o app enviaria. `celular` com o 55 na frente. */
data class MensagemSimulada(val celular: String, val texto: String)

/**
 * Não envia nada: mostra no Logcat (filtro `WhatsApp`) a mensagem que seria enviada e a deixa em
 * [mensagens], que o app mostra na tela (uma por vez) para dar para conferir no emulador.
 */
object WhatsAppSimulado : EnviadorWhatsApp {
    private const val TAG = "WhatsApp"

    private val _mensagens = MutableStateFlow<List<MensagemSimulada>>(emptyList())
    /** Mensagens "enviadas" que ainda não foram vistas na tela, mais antigas primeiro. */
    val mensagens: StateFlow<List<MensagemSimulada>> = _mensagens.asStateFlow()

    override fun enviar(celular: String, mensagem: String) {
        Log.i(TAG, "[SIMULADO] para $celular")
        mensagem.lines().forEach { Log.i(TAG, it.ifEmpty { " " }) }
        _mensagens.update { it + MensagemSimulada(celular, mensagem) }
    }

    /** Tira da fila a mensagem que acabou de ser vista. */
    fun dispensar() = _mensagens.update { it.drop(1) }
}

/** Celular brasileiro (só dígitos) no formato que o WhatsApp usa: com 55 na frente. */
fun celularWhatsApp(digitos: String): String =
    if (digitos.length in 10..11) "55$digitos" else digitos

/** Textos das mensagens enviadas para a cliente. */
object MensagensWhatsApp {
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
    private val formatoData = DateTimeFormatter.ofPattern("dd/MM")

    private fun quando(a: Agendamento): String =
        "📅 ${diaDaSemana(a.data).replaceFirstChar { it.uppercase() }}, ${a.data.format(formatoData)}\n" +
            "🕙 ${a.inicio.format(formatoHora)} às ${a.fim.format(formatoHora)}"

    /** Linha com o motivo que a dona escreveu (vazio quando ela não escreveu nada). */
    private fun linhaMotivo(motivo: String): String =
        motivo.trim().replace(Regex("\\s+"), " ").takeIf { it.isNotEmpty() }?.let { "\n📝 Motivo: $it" }.orEmpty()

    private fun contato(salao: DadosSalao): String =
        "Precisa remarcar? Fale com a gente: ${formatarCelular(salao.whatsapp)}"

    /** `anterior`: o horário que foi trocado (na remarcação). `motivo`: escrito pela dona. */
    fun confirmacao(
        a: Agendamento,
        salao: DadosSalao,
        remarcado: Boolean,
        motivo: String = "",
        anterior: Agendamento? = null,
    ): String {
        val abertura = when {
            remarcado && anterior != null ->
                "Seu horário no ${salao.nome} foi remarcado (era ${diaDaSemana(anterior.data)}, " +
                    "${anterior.data.format(formatoData)} às ${anterior.inicio.format(formatoHora)})."
            remarcado -> "Seu horário no ${salao.nome} foi remarcado:"
            a.reservadoPelaDona -> "A gente reservou um horário pra você no ${salao.nome}:"
            else -> "Seu horário no ${salao.nome} está confirmado:"
        }
        return """
            |Olá, ${a.clienteNome}! 💇‍♀️
            |$abertura${linhaMotivo(motivo)}
            |
            |${if (remarcado && anterior != null) "Novo horário:\n" else ""}${quando(a)}
            |✂️ ${a.titulo}
            |📍 ${salao.endereco}
            |
            |${contato(salao)}
        """.trimMargin()
    }

    /** `prazo`: "é amanhã", "é hoje", "é daqui a 1 hora"... */
    fun lembrete(a: Agendamento, salao: DadosSalao, prazo: String): String = """
        |Olá, ${a.clienteNome}! ⏰
        |Passando para lembrar: seu horário no ${salao.nome} $prazo.
        |
        |${quando(a)}
        |✂️ ${a.titulo}
        |📍 ${salao.endereco}
        |
        |${contato(salao)}
    """.trimMargin()

    fun cancelamento(a: Agendamento, salao: DadosSalao, motivo: String = ""): String = """
        |Olá, ${a.clienteNome}.
        |Seu horário no ${salao.nome} foi cancelado:${linhaMotivo(motivo)}
        |
        |${quando(a)}
        |✂️ ${a.titulo}
        |
        |Quer marcar outro dia? Fale com a gente: ${formatarCelular(salao.whatsapp)}
    """.trimMargin()
}
