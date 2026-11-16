package br.com.skala.app.ui.dona

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.StatusDia
import br.com.skala.app.ui.components.Cartao
import br.com.skala.app.ui.components.SecondaryButton
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.diaSemanaCurto
import br.com.skala.app.ui.components.mesAno
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val formatoDiaMes = DateTimeFormatter.ofPattern("d/MM")

/**
 * Calendário do mês em que a dona marca cada dia como aberto, fechado ou a definir.
 * Não existe horário fixo: cada dia tem as próprias faixas.
 */
@Composable
fun HorariosScreen(
    dia: (LocalDate) -> DiaFuncionamento,
    onEditarDia: (LocalDate) -> Unit,
    onRepetirSemanas: (LocalDate) -> Unit,
    onFecharDia: (LocalDate) -> Unit,
    /** Dia cujos horários a dona copiou, esperando para ser colado em outra data. */
    diaCopiado: DiaFuncionamento?,
    onCopiarDia: (LocalDate) -> Unit,
    onColarDia: (LocalDate) -> Unit,
    onLimparCopia: () -> Unit,
) {
    val hoje = LocalDate.now()
    var mesBase by rememberSaveable { mutableStateOf(hoje.withDayOfMonth(1).toEpochDay()) }
    var selecionado by rememberSaveable { mutableStateOf(hoje.toEpochDay()) }
    val primeiro = LocalDate.ofEpochDay(mesBase)
    val sel = LocalDate.ofEpochDay(selecionado)
    val diaSel = dia(sel)

    Column(
        Modifier.fillMaxSize().background(SkalaColors.Ivory).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("PAINEL DO SALÃO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = SkalaColors.Muted)
        TituloDaTela("Horários de funcionamento")
        Text(
            "Escolha dia a dia quando o salão abre. As clientes só veem os horários que você liberar.",
            fontSize = 13.sp,
            color = SkalaColors.Muted,
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(primeiro.mesAno(), style = TituloSecao.copy(fontSize = 20.sp), color = SkalaColors.Wine, modifier = Modifier.weight(1f))
            Seta(true) { mesBase = primeiro.minusMonths(1).toEpochDay() }
            Spacer(Modifier.width(4.dp))
            Seta(false) { mesBase = primeiro.plusMonths(1).toEpochDay() }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                Text(it, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        // Domingo é a primeira coluna
        val vazias = primeiro.dayOfWeek.value % 7
        val diasNoMes = primeiro.lengthOfMonth()
        val celulas = List(vazias) { null } + (1..diasNoMes).map { primeiro.withDayOfMonth(it) }
        celulas.chunked(7).forEach { semana ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                semana.forEach { d ->
                    if (d == null) Spacer(Modifier.weight(1f)) else CelulaDia(d, dia(d).status, d == sel, d == hoje, d.isBefore(hoje), Modifier.weight(1f)) {
                        selecionado = d.toEpochDay()
                    }
                }
                repeat(7 - semana.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legenda(SkalaColors.Blush, SkalaColors.RoseLight, "Aberto")
            Legenda(SkalaColors.Closed, SkalaColors.Closed, "Fechado")
            Legenda(Color.Transparent, SkalaColors.Dashed, "A definir")
        }

        Spacer(Modifier.height(12.dp))
        Cartao(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sel.textoLongo(), style = TituloSecao.copy(fontSize = 20.sp), color = SkalaColors.Wine)
                        Text(
                            when (diaSel.status) {
                                StatusDia.ABERTO -> "Aberto · as clientes podem agendar"
                                StatusDia.FECHADO -> "Fechado · nenhum horário disponível"
                                StatusDia.A_DEFINIR -> "A definir · ainda não aparece para as clientes"
                            },
                            fontSize = 13.sp,
                            color = if (diaSel.status == StatusDia.ABERTO) SkalaColors.RoseLink else SkalaColors.Muted,
                        )
                    }
                    if (!sel.isBefore(hoje)) {
                        Surface(onClick = { onEditarDia(sel) }, shape = RoundedCornerShape(50), color = SkalaColors.Rose) {
                            Text("Editar dia", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                        }
                    }
                }
                if (diaSel.status == StatusDia.ABERTO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        diaSel.faixas.forEach { f ->
                            Text(
                                "${f.inicio.texto()}–${f.fim.texto()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SkalaColors.RoseLink,
                                modifier = Modifier.background(SkalaColors.Blush, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }

        // Copiar funciona em qualquer dia definido (até no passado); colar só de hoje em diante.
        val podeCopiar = diaSel.status != StatusDia.A_DEFINIR
        val podeColar = diaCopiado != null && !sel.isBefore(hoje) && diaCopiado.data != sel
        if (podeCopiar || podeColar) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (podeCopiar) SecondaryButton("Copiar horário", onClick = { onCopiarDia(sel) }, icon = Icons.Outlined.ContentCopy, modifier = Modifier.weight(1f))
                if (podeColar) SecondaryButton("Colar aqui", onClick = { onColarDia(sel) }, icon = Icons.Outlined.ContentPaste, modifier = Modifier.weight(1f))
            }
        }
        if (diaCopiado != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).background(SkalaColors.Blush, RoundedCornerShape(14.dp)).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Copiado: ${diaCopiado.data.diaSemanaCurto()}, ${diaCopiado.data.format(formatoDiaMes)} · ${diaCopiado.resumoHorario()}",
                    fontSize = 12.sp,
                    color = SkalaColors.RoseLink,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onLimparCopia) {
                    Icon(Icons.Outlined.Close, contentDescription = "Descartar dia copiado", tint = SkalaColors.RoseLink, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (!sel.isBefore(hoje)) {
            Spacer(Modifier.height(10.dp))
            // O texto de repetir é longo demais para meia largura: cada botão ocupa a linha toda.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(textoRepetir(sel), onClick = { onRepetirSemanas(sel) }, icon = Icons.Outlined.EventRepeat)
                SecondaryButton("Fechar este dia", onClick = { onFecharDia(sel) }, icon = Icons.Outlined.Block)
            }
        }
    }
}

/** "Repetir nas próximas 4 sextas" / "Repetir nos próximos 4 sábados". */
private fun textoRepetir(data: LocalDate): String = when (data.dayOfWeek) {
    DayOfWeek.MONDAY -> "Repetir nas próximas 4 segundas"
    DayOfWeek.TUESDAY -> "Repetir nas próximas 4 terças"
    DayOfWeek.WEDNESDAY -> "Repetir nas próximas 4 quartas"
    DayOfWeek.THURSDAY -> "Repetir nas próximas 4 quintas"
    DayOfWeek.FRIDAY -> "Repetir nas próximas 4 sextas"
    DayOfWeek.SATURDAY -> "Repetir nos próximos 4 sábados"
    DayOfWeek.SUNDAY -> "Repetir nos próximos 4 domingos"
}

/** "09:00–12:00, 14:00–19:00" ou "Fechado". */
private fun DiaFuncionamento.resumoHorario(): String = when (status) {
    StatusDia.ABERTO -> faixas.joinToString(", ") { "${it.inicio.texto()}–${it.fim.texto()}" }
    StatusDia.FECHADO -> "Fechado"
    StatusDia.A_DEFINIR -> "A definir"
}

@Composable
private fun CelulaDia(
    data: LocalDate,
    status: StatusDia,
    selecionado: Boolean,
    hoje: Boolean,
    passado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val aberto = status == StatusDia.ABERTO
    val fechado = status == StatusDia.FECHADO
    val fundo = when {
        selecionado -> SkalaColors.Wine
        passado -> Color.Transparent
        aberto -> SkalaColors.Blush
        fechado -> SkalaColors.Closed
        else -> Color.Transparent
    }
    val borda = when {
        selecionado -> SkalaColors.Wine
        passado -> Color.Transparent
        hoje -> SkalaColors.Wine
        aberto -> SkalaColors.RoseLight
        fechado -> SkalaColors.Closed
        else -> SkalaColors.Dashed
    }
    val cor = when {
        selecionado -> Color.White
        passado -> SkalaColors.Placeholder
        fechado -> SkalaColors.Muted
        else -> SkalaColors.Wine
    }
    val situacao = when { passado -> "já passou"; aberto -> "aberto"; fechado -> "fechado"; else -> "a definir" }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = fundo,
        border = BorderStroke(1.5.dp, borda),
        modifier = modifier.height(44.dp).semantics { contentDescription = "${data.textoLongo()}, $situacao" },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "${data.dayOfMonth}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = cor,
                textDecoration = if (fechado && !selecionado && !passado) TextDecoration.LineThrough else null,
            )
            Box(
                Modifier.size(4.dp).background(
                    when { !aberto || passado -> Color.Transparent; selecionado -> SkalaColors.RoseLight; else -> SkalaColors.Rose },
                    CircleShape,
                ),
            )
        }
    }
}

@Composable
private fun Legenda(fundo: Color, borda: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(shape = RoundedCornerShape(4.dp), color = fundo, border = BorderStroke(1.dp, borda), modifier = Modifier.size(12.dp)) {}
        Text(texto, fontSize = 12.sp, color = SkalaColors.Label)
    }
}

@Composable
private fun Seta(esquerda: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, SkalaColors.Border), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (esquerda) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (esquerda) "Mês anterior" else "Próximo mês",
                tint = SkalaColors.Wine,
            )
        }
    }
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Horários")
@Composable
private fun HorariosScreenPreview() {
    SkalaTheme {
        HorariosScreen(
            dia = FakeSalaoRepository::dia,
            onEditarDia = {},
            onRepetirSemanas = {},
            onFecharDia = {},
            diaCopiado = null,
            onCopiarDia = {},
            onColarDia = {},
            onLimparCopia = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Horários · com dia copiado")
@Composable
private fun HorariosScreenCopiadoPreview() {
    SkalaTheme {
        HorariosScreen(
            dia = FakeSalaoRepository::dia,
            onEditarDia = {},
            onRepetirSemanas = {},
            onFecharDia = {},
            diaCopiado = FakeSalaoRepository.dia(LocalDate.now().minusDays(1)),
            onCopiarDia = {},
            onColarDia = {},
            onLimparCopia = {},
        )
    }
}
