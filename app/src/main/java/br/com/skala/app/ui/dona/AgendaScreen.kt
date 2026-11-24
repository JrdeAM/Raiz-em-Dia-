package br.com.skala.app.ui.dona

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.AvisoDona
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.StatusAgendamento
import br.com.skala.app.data.StatusDia
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.OpcaoPilula
import br.com.skala.app.ui.components.PtBr
import br.com.skala.app.ui.components.SkalaLogo
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class TipoItem { CLIENTE, LIVRE, PAUSA }

private data class ItemAgenda(
    val hora: LocalTime,
    val titulo: String,
    val detalhe: String,
    val tipo: TipoItem,
    /** Só nos itens de cliente: o horário que abre ao tocar. */
    val agendamento: Agendamento? = null,
)

private enum class ModoAgenda { DIA, SEMANA }

private val formatoDiaMes = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)

/** Semana de domingo a sábado, igual ao calendário de horários. */
private fun LocalDate.inicioDaSemana(): LocalDate = minusDays((dayOfWeek.value % 7).toLong())

/** Agendamentos que contam na agenda daquele dia (cancelados ficam de fora). */
private fun List<Agendamento>.doDia(data: LocalDate) =
    filter { it.data == data && it.status != StatusAgendamento.CANCELADO }.sortedBy { it.inicio }

/**
 * Agenda da dona. No modo Dia mostra clientes, horários livres, pausas e bloqueios do dia
 * escolhido; no modo Semana mostra os 7 dias com as clientes de cada um.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    dia: (LocalDate) -> DiaFuncionamento,
    agendamentos: List<Agendamento>,
    onAjustarDia: (LocalDate) -> Unit,
    /** Clientes que marcaram, desmarcaram ou remarcaram pelo app. */
    avisos: List<AvisoDona> = emptyList(),
    onMarcarAvisosLidos: () -> Unit = {},
    /** Tocar numa cliente na agenda do dia abre o horário, com remarcar e cancelar. */
    /** O texto é o motivo que a dona escreveu (pode ser vazio); vai na mensagem para a cliente. */
    onRemarcar: (Agendamento, String) -> Unit = { _, _ -> },
    onCancelar: (Agendamento, String) -> Unit = { _, _ -> },
) {
    val hoje = LocalDate.now()
    var modo by rememberSaveable { mutableStateOf(ModoAgenda.DIA) }
    var selecionadoEpoch by rememberSaveable { mutableStateOf(hoje.toEpochDay()) }
    var escolhendoData by rememberSaveable { mutableStateOf(false) }
    var abertoId by rememberSaveable { mutableStateOf<String?>(null) }
    val selecionado = LocalDate.ofEpochDay(selecionadoEpoch)
    val inicioSemana = selecionado.inicioDaSemana()
    val semana = (0L..6L).map { inicioSemana.plusDays(it) }

    fun irPara(data: LocalDate) {
        selecionadoEpoch = data.toEpochDay()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(SkalaColors.Ivory),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PAINEL DO SALÃO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = SkalaColors.Muted)
                    TituloDaTela(
                        when {
                            modo == ModoAgenda.SEMANA -> "Agenda da semana"
                            selecionado == hoje -> "Agenda de hoje"
                            else -> "Agenda do dia"
                        },
                    )
                }
                SkalaLogo(altura = 52.dp, descricao = null)
            }
            Spacer(Modifier.height(10.dp))

            val naoLidos = avisos.filter { !it.lido }
            if (naoLidos.isNotEmpty()) {
                CartaoAvisosDona(
                    avisos = naoLidos,
                    onAbrir = { aviso ->
                        modo = ModoAgenda.DIA
                        irPara(aviso.agendamento.data)
                    },
                    onMarcarLidos = onMarcarAvisosLidos,
                )
                Spacer(Modifier.height(12.dp))
            }

            // Dia | Semana
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpcaoPilula("Dia", selecionada = modo == ModoAgenda.DIA, onClick = { modo = ModoAgenda.DIA }, modifier = Modifier.weight(1f))
                OpcaoPilula("Semana", selecionada = modo == ModoAgenda.SEMANA, onClick = { modo = ModoAgenda.SEMANA }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            // ‹  data escolhida  ›   (tocar na data abre o calendário)
            val passo = if (modo == ModoAgenda.DIA) 1L else 7L
            Row(verticalAlignment = Alignment.CenterVertically) {
                SetaData(esquerda = true, descricao = if (modo == ModoAgenda.DIA) "Dia anterior" else "Semana anterior") { irPara(selecionado.minusDays(passo)) }
                Surface(
                    onClick = { escolhendoData = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Escolher data no calendário" },
                ) {
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (modo == ModoAgenda.DIA) selecionado.textoLongo() else textoSemana(inicioSemana),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = SkalaColors.Wine,
                        )
                    }
                }
                SetaData(esquerda = false, descricao = if (modo == ModoAgenda.DIA) "Próximo dia" else "Próxima semana") { irPara(selecionado.plusDays(passo)) }
            }
            val foraDeHoje = if (modo == ModoAgenda.DIA) selecionado != hoje else hoje !in semana
            if (foraDeHoje) {
                // Texto clicável simples (sem TextButton) para ficar colado na data, sem o espaço extra do botão
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (modo == ModoAgenda.DIA) "Programação de hoje" else "Programação desta semana",
                        color = SkalaColors.RoseLink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { irPara(hoje) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (modo == ModoAgenda.DIA) {
            item { Spacer(Modifier.height(4.dp)) }
            conteudoDia(dia(selecionado), agendamentos.doDia(selecionado), hoje, onAjustarDia, onAbrir = { abertoId = it.id })
        } else {
            item {
                val daSemana = semana.sumOf { agendamentos.doDia(it).size }
                val abertos = semana.count { dia(it).status == StatusDia.ABERTO }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Contador("$daSemana", if (daSemana == 1) "cliente na semana" else "clientes na semana", Modifier.weight(1f))
                    Contador("$abertos", if (abertos == 1) "dia aberto" else "dias abertos", Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
            }
            items(semana) { d ->
                CartaoDiaSemana(dia(d), agendamentos.doDia(d), hoje) {
                    irPara(d)
                    modo = ModoAgenda.DIA
                }
            }
        }
    }

    if (escolhendoData) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = selecionado.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { escolhendoData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { irPara(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    escolhendoData = false
                }) { Text("OK", color = SkalaColors.Rose) }
            },
            dismissButton = { TextButton(onClick = { escolhendoData = false }) { Text("Cancelar", color = SkalaColors.Wine) } },
            colors = DatePickerDefaults.colors(containerColor = SkalaColors.Ivory),
        ) {
            DatePicker(
                state = estado,
                colors = DatePickerDefaults.colors(
                    containerColor = SkalaColors.Ivory,
                    selectedDayContainerColor = SkalaColors.Rose,
                    todayDateBorderColor = SkalaColors.Rose,
                    todayContentColor = SkalaColors.Rose,
                ),
            )
        }
    }

    agendamentos.firstOrNull { it.id == abertoId }?.let { a ->
        DetalheAgendamentoDialog(
            agendamento = a,
            onFechar = { abertoId = null },
            onRemarcar = { motivo ->
                abertoId = null
                onRemarcar(a, motivo)
            },
            onCancelar = { motivo ->
                abertoId = null
                onCancelar(a, motivo)
            },
        )
    }
}

/** "1 – 7 de novembro" ou "29 de novembro – 5 de dezembro". */
private fun textoSemana(inicio: LocalDate): String {
    val fim = inicio.plusDays(6)
    return if (inicio.month == fim.month) "${inicio.dayOfMonth} – ${fim.format(formatoDiaMes)}"
    else "${inicio.format(formatoDiaMes)} – ${fim.format(formatoDiaMes)}"
}

/** Itens do modo Dia: cartão de funcionamento, contadores e a linha do tempo. */
private fun androidx.compose.foundation.lazy.LazyListScope.conteudoDia(
    dia: DiaFuncionamento,
    doDia: List<Agendamento>,
    hoje: LocalDate,
    onAjustarDia: (LocalDate) -> Unit,
    onAbrir: (Agendamento) -> Unit,
) {
    val itens = montarItens(dia, doDia)
    val livres = itens.count { it.tipo == TipoItem.LIVRE }
    val ehHoje = dia.data == hoje
    item {
        Row(
            Modifier.fillMaxWidth().background(SkalaColors.Wine, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(if (dia.status == StatusDia.ABERTO) SkalaColors.RoseLight else SkalaColors.Placeholder, CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val quando = if (ehHoje) "hoje" else "neste dia"
                Text(
                    when (dia.status) {
                        StatusDia.ABERTO -> "Aberto $quando"
                        StatusDia.FECHADO -> "Fechado $quando"
                        StatusDia.A_DEFINIR -> "Horário a definir"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                if (dia.status == StatusDia.ABERTO) {
                    Text(dia.faixas.joinToString(" · ") { "${it.inicio.texto()} às ${it.fim.texto()}" }, fontSize = 13.sp, color = SkalaColors.OnWineMuted)
                }
            }
            // Dias que já passaram não podem ser editados (igual à tela de horários)
            if (!dia.data.isBefore(hoje)) {
                Surface(onClick = { onAjustarDia(dia.data) }, shape = RoundedCornerShape(50), color = SkalaColors.Ivory) {
                    Text("Ajustar", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Contador("${doDia.size}", if (doDia.size == 1) "agendada" else "agendadas", Modifier.weight(1f))
            Contador("$livres", if (livres == 1) "horário livre" else "horários livres", Modifier.weight(1f))
            Contador("${dia.bloqueios.size}", if (dia.bloqueios.size == 1) "bloqueio" else "bloqueios", Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
    }
    if (itens.isEmpty()) {
        item {
            Text(
                when (dia.status) {
                    StatusDia.FECHADO -> "Salão fechado neste dia."
                    StatusDia.A_DEFINIR -> "Este dia ainda não tem horário definido."
                    StatusDia.ABERTO -> "Nenhum horário neste dia."
                },
                fontSize = 14.sp,
                color = SkalaColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }
    }
    items(itens) { item -> LinhaAgenda(item, onClick = item.agendamento?.let { a -> { onAbrir(a) } }) }
}

/** Um dia no modo Semana: funcionamento e lista das clientes. Tocar abre o dia. */
@Composable
private fun CartaoDiaSemana(dia: DiaFuncionamento, doDia: List<Agendamento>, hoje: LocalDate, onAbrir: () -> Unit) {
    val ehHoje = dia.data == hoje
    val passado = dia.data.isBefore(hoje)
    Surface(
        onClick = onAbrir,
        shape = RoundedCornerShape(18.dp),
        color = if (dia.status == StatusDia.ABERTO) Color.White else SkalaColors.Disabled,
        border = BorderStroke(if (ehHoje) 1.5.dp else 1.dp, if (ehHoje) SkalaColors.Wine else SkalaColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        dia.data.textoLongo() + if (ehHoje) " · hoje" else "",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (passado) SkalaColors.Muted else SkalaColors.Wine,
                    )
                    Text(
                        when (dia.status) {
                            StatusDia.ABERTO -> dia.faixas.joinToString(" · ") { "${it.inicio.texto()}–${it.fim.texto()}" }
                            StatusDia.FECHADO -> "Fechado"
                            StatusDia.A_DEFINIR -> "Horário a definir"
                        },
                        fontSize = 12.sp,
                        color = SkalaColors.Muted,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SkalaColors.Placeholder)
            }
            if (doDia.isEmpty()) {
                if (dia.status == StatusDia.ABERTO) Text("Nenhuma cliente agendada", fontSize = 13.sp, color = SkalaColors.Placeholder)
            } else {
                doDia.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.inicio.texto(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Label, modifier = Modifier.width(48.dp))
                        Box(Modifier.width(3.dp).height(28.dp).background(SkalaColors.Rose, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(a.clienteNome, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                            Text("${a.titulo} · ${a.duracaoTexto}", fontSize = 12.sp, color = SkalaColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetaData(esquerda: Boolean, descricao: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, SkalaColors.Border), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (esquerda) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = descricao,
                tint = SkalaColors.Wine,
            )
        }
    }
}

@Composable
private fun Contador(numero: String, rotulo: String, modifier: Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, SkalaColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(numero, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Rose)
        Text(rotulo, fontSize = 12.sp, color = SkalaColors.Muted)
    }
}

@Composable
private fun LinhaAgenda(item: ItemAgenda, onClick: (() -> Unit)? = null) {
    val (fundo, borda, faixa, cor) = when (item.tipo) {
        TipoItem.CLIENTE -> listOf(Color.White, SkalaColors.CardBorder, SkalaColors.Rose, SkalaColors.Wine)
        TipoItem.LIVRE -> listOf(Color.Transparent, Color(0xFFD9C2C8), SkalaColors.RoseLight, SkalaColors.RoseLink)
        TipoItem.PAUSA -> listOf(SkalaColors.Disabled, SkalaColors.Disabled, SkalaColors.Dashed, SkalaColors.Label)
    }
    val toque = if (onClick != null) Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick) else Modifier
    Row(Modifier.height(IntrinsicSize.Min).then(toque)) {
        Text(item.hora.texto(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Label, modifier = Modifier.width(52.dp).padding(top = 12.dp))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(fundo, RoundedCornerShape(16.dp))
                    .border(1.dp, borda, RoundedCornerShape(16.dp))
                    .padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Text(item.titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = cor)
                Text(item.detalhe, fontSize = 12.sp, color = SkalaColors.Muted)
            }
            // Mesma barra e mesmo lugar de antes, mas vem depois da moldura no Box: fica por cima e não é cortada.
            Box(Modifier.width(4.dp).fillMaxHeight().background(faixa, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
        }
    }
}

/** Junta clientes, bloqueios, pausas entre faixas e os intervalos livres que sobram em cada faixa. */
private fun montarItens(dia: DiaFuncionamento, agendamentos: List<Agendamento>): List<ItemAgenda> {
    val clientes = agendamentos.map {
        ItemAgenda(it.inicio, it.clienteNome, "${it.titulo} · ${it.duracaoTexto}", TipoItem.CLIENTE, agendamento = it)
    }
    // Dia fechado com clientes marcadas (a dona salvou mesmo assim): elas aparecem para serem remarcadas
    if (dia.status != StatusDia.ABERTO) return clientes.sortedBy { it.hora }
    val itens = clientes.toMutableList()
    dia.bloqueios.forEach {
        itens += ItemAgenda(it.inicio, "Bloqueado", "${it.motivo.ifBlank { "Horário fechado" }} · até ${it.fim.texto()}", TipoItem.PAUSA)
    }
    val faixas = dia.faixas.sortedBy { it.inicio }
    faixas.zipWithNext().forEach { (a, b) ->
        if (a.fim.isBefore(b.inicio)) itens += ItemAgenda(a.fim, "Fechado", "Pausa até ${b.inicio.texto()}", TipoItem.PAUSA)
    }
    val ocupados = (agendamentos.map { it.inicio to it.fim } + dia.bloqueios.map { it.inicio to it.fim }).sortedBy { it.first }
    faixas.forEach { faixa ->
        var cursor = faixa.inicio
        ocupados.filter { (ini, fim) -> ini.isBefore(faixa.fim) && fim.isAfter(faixa.inicio) }.forEach { (ini, fim) ->
            if (ini.isAfter(cursor)) itens += ItemAgenda(cursor, "Horário livre", "${cursor.texto()} às ${ini.texto()}", TipoItem.LIVRE)
            if (fim.isAfter(cursor)) cursor = fim
        }
        if (cursor.isBefore(faixa.fim)) itens += ItemAgenda(cursor, "Horário livre", "${cursor.texto()} às ${faixa.fim.texto()}", TipoItem.LIVRE)
    }
    return itens.sortedBy { it.hora }
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Agenda")
@Composable
private fun AgendaScreenPreview() {
    SkalaTheme {
        AgendaScreen(dia = FakeSalaoRepository::dia, agendamentos = FakeSalaoRepository.agendamentos.value, onAjustarDia = {})
    }
}

/**
 * Detalhes de um horário marcado. Remarcar e cancelar só aparecem para horários confirmados que
 * ainda não passaram. Cancelar pede confirmação.
 */
@Composable
private fun DetalheAgendamentoDialog(
    agendamento: Agendamento,
    onFechar: () -> Unit,
    onRemarcar: (motivo: String) -> Unit,
    onCancelar: (motivo: String) -> Unit,
) {
    var confirmandoCancelamento by rememberSaveable(agendamento.id) { mutableStateOf(false) }
    var motivo by rememberSaveable(agendamento.id) { mutableStateOf("") }
    val campoMotivo: @Composable () -> Unit = {
        LabeledTextField(
            label = "Motivo (opcional)",
            value = motivo,
            onValueChange = { motivo = it },
            placeholder = "Ex.: imprevisto no salão. Vai na mensagem para a cliente.",
            singleLine = false,
            minLines = 2,
        )
    }
    val podeMudar = agendamento.status == StatusAgendamento.CONFIRMADO &&
        LocalDateTime.of(agendamento.data, agendamento.inicio).isAfter(LocalDateTime.now())

    if (confirmandoCancelamento) {
        AlertDialog(
            onDismissRequest = { confirmandoCancelamento = false },
            title = { Text("Cancelar o horário de ${agendamento.clienteNome}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "O horário fica livre para outras clientes e ${agendamento.clienteNome} é avisada da mudança.",
                        fontSize = 14.sp,
                        color = SkalaColors.Muted,
                    )
                    campoMotivo()
                }
            },
            confirmButton = { TextButton(onClick = { onCancelar(motivo.trim()) }) { Text("Cancelar horário", color = SkalaColors.RoseLink) } },
            dismissButton = { TextButton(onClick = { confirmandoCancelamento = false }) { Text("Voltar", color = SkalaColors.Wine) } },
            containerColor = SkalaColors.Ivory,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(agendamento.clienteNome) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${agendamento.data.textoLongo()} · ${agendamento.inicio.texto()} às ${agendamento.fim.texto()}",
                    fontSize = 14.sp,
                    color = SkalaColors.Wine,
                )
                Text("${agendamento.titulo} · ${agendamento.duracaoTexto}", fontSize = 14.sp, color = SkalaColors.Muted)
                if (agendamento.clienteCelular.isNotEmpty()) {
                    Text("Celular: ${formatarCelular(agendamento.clienteCelular)}", fontSize = 14.sp, color = SkalaColors.Muted)
                }
                if (agendamento.observacao.isNotBlank()) {
                    Text("Observação: ${agendamento.observacao}", fontSize = 14.sp, color = SkalaColors.Muted)
                }
                if (podeMudar) campoMotivo()
                if (!podeMudar) {
                    Text("Esse horário já passou ou foi cancelado.", fontSize = 13.sp, color = SkalaColors.Muted)
                }
            }
        },
        confirmButton = {
            if (podeMudar) TextButton(onClick = { onRemarcar(motivo.trim()) }) { Text("Remarcar", color = SkalaColors.Rose) }
        },
        dismissButton = {
            Row {
                if (podeMudar) {
                    TextButton(onClick = { confirmandoCancelamento = true }) { Text("Cancelar horário", color = SkalaColors.RoseLink) }
                }
                TextButton(onClick = onFechar) { Text("Fechar", color = SkalaColors.Wine) }
            }
        },
        containerColor = SkalaColors.Ivory,
    )
}
