package br.com.skala.app.avisos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.skala.app.MainActivity
import br.com.skala.app.R
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.DadosSalao
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * true: os 4 lembretes aparecem (ou as 4 mensagens de WhatsApp saem) 10, 20, 30 e 40 segundos depois de confirmar (com o app aberto ou em
 * segundo plano), para testar ou gravar uma demonstração. Deixe false no app de verdade.
 */
const val LEMBRETES_MODO_TESTE = false

/** Um aviso no celular, guardado para poder ser reagendado se o celular reiniciar. */
data class Lembrete(
    /** "idDoAgendamento/n", único por lembrete. */
    val chave: String,
    val agendamentoId: String,
    val quandoMs: Long,
    val titulo: String,
    val texto: String,
    /** Preenchido nos lembretes por WhatsApp: na hora, `texto` vai para este número em vez de virar notificação. */
    val whatsAppPara: String = "",
) {
    val notificacaoId: Int get() = chave.hashCode()
    val porWhatsApp: Boolean get() = whatsAppPara.isNotEmpty()

    fun paraJson(): String = JSONObject()
        .put("chave", chave).put("agendamentoId", agendamentoId).put("quandoMs", quandoMs)
        .put("titulo", titulo).put("texto", texto).put("whatsAppPara", whatsAppPara)
        .toString()

    companion object {
        fun deJson(json: String): Lembrete? = runCatching {
            val o = JSONObject(json)
            Lembrete(o.getString("chave"), o.getString("agendamentoId"), o.getLong("quandoMs"), o.getString("titulo"), o.getString("texto"), o.optString("whatsAppPara"))
        }.getOrNull()
    }
}

/** "sexta", "sábado"... como se fala, sem o "-feira". */
fun diaDaSemana(data: LocalDate): String = when (data.dayOfWeek) {
    DayOfWeek.MONDAY -> "segunda"
    DayOfWeek.TUESDAY -> "terça"
    DayOfWeek.WEDNESDAY -> "quarta"
    DayOfWeek.THURSDAY -> "quinta"
    DayOfWeek.FRIDAY -> "sexta"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
    null -> ""
}

/**
 * Os lembretes de um agendamento: 2 dias antes, 1 dia antes, 5 horas antes e 1 hora antes do horário.
 * Os que já passaram (agendamento marcado em cima da hora) ficam de fora.
 * `porWhatsApp`: em vez de notificação no celular, cada lembrete vira uma mensagem de WhatsApp para a cliente.
 */
fun calcularLembretes(
    a: Agendamento,
    salao: DadosSalao,
    agora: LocalDateTime = LocalDateTime.now(),
    porWhatsApp: Boolean = false,
): List<Lembrete> {
    val horario = LocalDateTime.of(a.data, a.inicio)
    val hora = a.inicio.format(DateTimeFormatter.ofPattern("HH:mm"))
    val oQue = "${a.titulo} às $hora"
    // quando, título e texto da notificação, e como a mensagem de WhatsApp fala do dia
    val avisos = listOf(
        listOf(horario.minusDays(2), "Lembrete: seu horário é ${diaDaSemana(a.data)}", "$oQue no ${salao.nome}", "é ${diaDaSemana(a.data)}"),
        listOf(horario.minusDays(1), "Seu horário é amanhã!", oQue, "é amanhã"),
        listOf(horario.minusHours(5), "Hoje tem salão 💇‍♀️", "$oQue. Até já!", "é hoje"),
        listOf(horario.minusHours(1), "Falta 1 hora! ⏰", "$oQue. Te esperamos no ${salao.nome}", "é daqui a 1 hora"),
    )
    val para = if (porWhatsApp) celularWhatsApp(a.clienteCelular) else ""
    return avisos.mapIndexedNotNull { n, aviso ->
        val quando = aviso[0] as LocalDateTime
        val titulo = aviso[1] as String
        val texto = if (porWhatsApp) MensagensWhatsApp.lembrete(a, salao, aviso[3] as String) else aviso[2] as String
        val ms = if (LEMBRETES_MODO_TESTE) {
            System.currentTimeMillis() + (n + 1) * 10_000L
        } else {
            if (!quando.isAfter(agora)) return@mapIndexedNotNull null
            quando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        Lembrete(if (porWhatsApp) "${a.id}/w$n" else "${a.id}/$n", a.id, ms, titulo, texto, para)
    }
}

/** Agenda, cancela e mostra os lembretes no celular. */
object Lembretes {
    private const val CANAL = "lembretes"
    private const val PREFS = "lembretes"
    /**
     * Lembretes que já foram criados alguma vez (chave -> horário em ms). Não é apagado quando o
     * lembrete dispara: assim cada aviso (2 dias, 1 dia, 5 h, 1 h) sai só uma vez por horário,
     * mesmo que a sincronização rode de novo depois.
     */
    private const val PREFS_CRIADOS = "lembretes_criados"
    /** Registros de lembretes que já passaram há mais de 3 dias são descartados. */
    private const val GUARDAR_CRIADOS_MS = 3 * 24 * 60 * 60 * 1000L
    private const val EXTRA_CHAVE = "chave"

    /** Folga que o Android pode usar para economizar bateria. Não exige a permissão de alarme exato. */
    private const val JANELA_MS = 10 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun criados(context: Context) = context.getSharedPreferences(PREFS_CRIADOS, Context.MODE_PRIVATE)

    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Lembretes de horário", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos antes dos seus horários no salão"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** `porWhatsApp`: os lembretes saem como mensagem para a cliente (usado quando a dona marca por ela). */
    fun agendar(context: Context, agendamento: Agendamento, salao: DadosSalao, porWhatsApp: Boolean = false) {
        // troca só os lembretes do mesmo tipo: os de WhatsApp e os do celular são independentes
        apagar(context, todos(context).filter { it.agendamentoId == agendamento.id && it.porWhatsApp == porWhatsApp })
        limparCriadosAntigos(context)
        // cada lembrete só é criado uma vez: o que já foi criado (e talvez já disparou) não volta
        val jaCriados = criados(context).all.keys
        val lista = calcularLembretes(agendamento, salao, porWhatsApp = porWhatsApp).filter { it.chave !in jaCriados }
        prefs(context).edit().apply { lista.forEach { putString(it.chave, it.paraJson()) } }.apply()
        criados(context).edit().apply { lista.forEach { putLong(it.chave, it.quandoMs) } }.apply()
        lista.forEach { programar(context, it) }
    }

    private fun limparCriadosAntigos(context: Context) {
        val limite = System.currentTimeMillis() - GUARDAR_CRIADOS_MS
        val velhos = criados(context).all.filterValues { (it as? Long ?: 0L) < limite }.keys
        if (velhos.isNotEmpty()) criados(context).edit().apply { velhos.forEach { remove(it) } }.apply()
    }

    /** Apaga todos os lembretes do agendamento (do celular e de WhatsApp). */
    fun cancelar(context: Context, agendamentoId: String) {
        apagar(context, todos(context).filter { it.agendamentoId == agendamentoId })
    }

    private fun apagar(context: Context, lista: List<Lembrete>) {
        val alarmes = context.getSystemService(AlarmManager::class.java)
        lista.forEach { l ->
            pendingIntent(context, l.chave, PendingIntent.FLAG_NO_CREATE)?.let(alarmes::cancel)
            if (!l.porWhatsApp) NotificationManagerCompat.from(context).cancel(l.notificacaoId)
        }
        prefs(context).edit().apply { lista.forEach { remove(it.chave) } }.apply()
    }

    /** Depois que o celular reinicia: o Android apaga os alarmes, então eles são criados de novo. */
    fun reagendarTodos(context: Context) {
        val agora = System.currentTimeMillis()
        val (futuros, passados) = todos(context).partition { it.quandoMs > agora }
        prefs(context).edit().apply { passados.forEach { remove(it.chave) } }.apply()
        futuros.forEach { programar(context, it) }
    }

    /**
     * Este agendamento já teve notificações criadas neste celular, mesmo que já tenham disparado?
     * (Não conta as de WhatsApp, cuja chave termina em "/wN".)
     */
    fun temLembretesNoCelular(context: Context, agendamentoId: String): Boolean =
        criados(context).all.keys.any { it.startsWith("$agendamentoId/") && !it.startsWith("$agendamentoId/w") }

    private fun todos(context: Context): List<Lembrete> =
        prefs(context).all.values.mapNotNull { (it as? String)?.let(Lembrete::deJson) }

    private fun programar(context: Context, l: Lembrete) {
        if (LEMBRETES_MODO_TESTE) {
            val app = context.applicationContext
            Handler(Looper.getMainLooper()).postDelayed({ disparar(app, l.chave) }, l.quandoMs - System.currentTimeMillis())
            return
        }
        val pi = pendingIntent(context, l.chave, 0) ?: return
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, l.quandoMs, JANELA_MS, pi)
    }

    /** Uri própria por lembrete, para cada um ter o seu alarme e poder ser cancelado. */
    private fun pendingIntent(context: Context, chave: String, flags: Int): PendingIntent? {
        val intent = Intent(context, LembreteReceiver::class.java)
            .setData(Uri.parse("skala://lembrete/$chave"))
            .putExtra(EXTRA_CHAVE, chave)
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Chegou a hora: mostra a notificação (se ainda valer) e esquece o lembrete. */
    internal fun disparar(context: Context, chave: String) {
        val l = prefs(context).getString(chave, null)?.let(Lembrete::deJson) ?: return // cancelado
        prefs(context).edit().remove(chave).apply()
        if (l.porWhatsApp) AvisosDoApp.whatsApp.enviar(l.whatsAppPara, l.texto) else mostrar(context, l)
    }

    private fun mostrar(context: Context, l: Lembrete) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        criarCanal(context)
        val abrir = PendingIntent.getActivity(
            context, l.notificacaoId,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_ABRIR_AGENDAMENTOS, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(l.titulo)
            .setContentText(l.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(l.texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(l.notificacaoId, notificacao)
    }

    internal fun chaveDo(intent: Intent): String? = intent.getStringExtra(EXTRA_CHAVE)
}

/** Recebe o alarme do Android na hora do lembrete. */
class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Lembretes.chaveDo(intent)?.let { Lembretes.disparar(context, it) }
    }
}

/** Celular ligou de novo: recria os alarmes dos lembretes que ainda vão acontecer. */
class ReinicioReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Lembretes.reagendarTodos(context)
    }
}
