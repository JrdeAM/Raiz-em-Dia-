package br.com.skala.app.avisos

import android.content.Context
import android.util.Log
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.AvisoDona
import br.com.skala.app.data.TipoAvisoDona
import br.com.skala.app.data.Cliente
import br.com.skala.app.data.DadosSalao
import br.com.skala.app.data.StatusAgendamento
import java.time.LocalDateTime

/**
 * O que acontece depois de agendar ou cancelar: mensagem de WhatsApp para a cliente
 * (simulada por enquanto) e lembretes no celular.
 */
interface AvisosAgendamento {
    /** `remarcado`: o horário substitui outro, que já foi cancelado sem mandar mensagem. */
    fun confirmado(agendamento: Agendamento, salao: DadosSalao, remarcado: Boolean = false)

    /** `avisarCliente`: false quando o cancelamento faz parte de uma remarcação. */
    fun cancelado(agendamento: Agendamento, salao: DadosSalao, avisarCliente: Boolean = true)

    /** Uma cliente marcou, desmarcou ou remarcou pelo app: avisa a dona. */
    fun avisarDona(aviso: AvisoDona)
}

/** Versão usada pelo app. Precisa de [iniciar] (feito na MainActivity); antes disso não faz nada. */
object AvisosDoApp : AvisosAgendamento {
    private var context: Context? = null
    var whatsApp: EnviadorWhatsApp = WhatsAppSimulado

    fun iniciar(context: Context) {
        this.context = context.applicationContext
        Lembretes.criarCanal(context)
    }

    override fun confirmado(agendamento: Agendamento, salao: DadosSalao, remarcado: Boolean) {
        val ctx = context ?: return
        if (agendamento.clienteCelular.isNotEmpty()) {
            whatsApp.enviar(celularWhatsApp(agendamento.clienteCelular), MensagensWhatsApp.confirmacao(agendamento, salao, remarcado))
        }
        if (agendamento.reservadoPelaDona) {
            // O celular é o da dona: os lembretes vão para a cliente por WhatsApp (cobre quem não usa o app).
            // Se a cliente usa o app, ela também recebe as notificações quando abrir o app (ver sincronizarCliente).
            if (agendamento.clienteCelular.isNotEmpty()) Lembretes.agendar(ctx, agendamento, salao, porWhatsApp = true)
        } else {
            Lembretes.agendar(ctx, agendamento, salao)
        }
    }

    /**
     * Com a cliente logada: cria as notificações dos horários dela que ainda não têm lembrete neste
     * celular, como os que a dona marcou por ela. Seguro chamar várias vezes.
     */
    fun sincronizarCliente(cliente: Cliente, agendamentos: List<Agendamento>, salao: DadosSalao) {
        val ctx = context ?: return
        // No modo de teste os 4 avisos saem em segundos para qualquer horário; sincronizar aqui
        // encheria a tela com os avisos de todos os horários da cliente. Só o horário recém-confirmado avisa.
        if (LEMBRETES_MODO_TESTE) return
        val agora = LocalDateTime.now()
        agendamentos
            .filter { it.eDe(cliente) && it.status == StatusAgendamento.CONFIRMADO }
            .filter { LocalDateTime.of(it.data, it.inicio).isAfter(agora) }
            .filterNot { Lembretes.temLembretesNoCelular(ctx, it.id) }
            .forEach { Lembretes.agendar(ctx, it, salao) }
    }

    /**
     * O aviso já fica guardado no repositório e aparece no painel da dona. A notificação no celular
     * dela precisa de servidor (o celular da cliente não alcança o da dona), então por enquanto só vai
     * para o Logcat (filtro `AvisoDona`). Com a API, é aqui que entra o push.
     */
    override fun avisarDona(aviso: AvisoDona) {
        Log.i("AvisoDona", "[SIMULADO] push para a dona: ${textoAvisoDona(aviso)}")
    }

    override fun cancelado(agendamento: Agendamento, salao: DadosSalao, avisarCliente: Boolean) {
        val ctx = context ?: return
        Lembretes.cancelar(ctx, agendamento.id)
        if (avisarCliente && agendamento.clienteCelular.isNotEmpty()) {
            whatsApp.enviar(celularWhatsApp(agendamento.clienteCelular), MensagensWhatsApp.cancelamento(agendamento, salao))
        }
    }
}

/** "Maria Clara marcou Escova para sex, 20/11 às 09:00" */
fun textoAvisoDona(aviso: AvisoDona): String {
    val formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")
    fun quando(a: Agendamento) = "${diaDaSemana(a.data)}, ${java.time.LocalDateTime.of(a.data, a.inicio).format(formato)}"
    val a = aviso.agendamento
    return when (aviso.tipo) {
        TipoAvisoDona.MARCOU -> "${a.clienteNome} marcou ${a.titulo} para ${quando(a)}"
        TipoAvisoDona.DESMARCOU -> "${a.clienteNome} desmarcou ${a.titulo} de ${quando(a)}"
        TipoAvisoDona.REMARCOU -> aviso.anterior?.let { "${a.clienteNome} remarcou ${a.titulo} de ${quando(it)} para ${quando(a)}" }
            ?: "${a.clienteNome} remarcou ${a.titulo} para ${quando(a)}"
    }
}
