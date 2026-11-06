package br.com.skala.app.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.StatusDia
import br.com.skala.app.ui.components.CabecalhoPasso
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.RotuloSecao
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.diaSemanaCurto
import br.com.skala.app.ui.components.mesAno
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao
import br.com.skala.app.ui.theme.TituloTela
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HorarioScreen(
    /** Ex.: "Escova + Hidratação · 2h" (soma de todos os serviços marcados). */
    resumoServicos: String,
    passo: Int,
    totalPassos: Int,
    dataSelecionada: LocalDate?,
    horaSelecionada: LocalTime?,
    statusDia: (LocalDate) -> StatusDia,
    horariosLivres: (LocalDate) -> List<LocalTime>,
    onEscolherData: (LocalDate) -> Unit,
    onEscolherHora: (LocalTime) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    val hoje = LocalDate.now()
    // Semana mostrada na faixa de dias (começa hoje; dá para avançar)
    var inicioSemana by rememberSaveable { mutableStateOf(hoje.toEpochDay()) }
    val dias = (0L..6L).map { LocalDate.ofEpochDay(inicioSemana).plusDays(it) }
    // Sem data escolhida ainda: abre no primeiro dia da semana que tem horário livre
    val padrao = dias.firstOrNull { horariosLivres(it).isNotEmpty() }
    val dia = dataSelecionada ?: padrao
    LaunchedEffect(padrao, dataSelecionada) {
        if (dataSelecionada == null && padrao != null) onEscolherData(padrao)
    }
    val livres = dia?.let(horariosLivres).orEmpty()

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        CabecalhoPasso(passo = passo, total = totalPassos, onVoltar = onVoltar)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp)) {
            TituloDaTela("Escolha o horário")
            Text(resumoServicos, fontSize = 14.sp, color = SkalaColors.Muted)

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dias.first().mesAno(), style = TituloSecao.copy(fontSize = 20.sp), color = SkalaColors.Wine, modifier = Modifier.weight(1f))
                BotaoSeta(esquerda = true, habilitado = inicioSemana > hoje.toEpochDay()) { inicioSemana -= 7 }
                Spacer(Modifier.size(4.dp))
                BotaoSeta(esquerda = false, habilitado = true) { inicioSemana += 7 }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dias.forEach { d ->
                    val aberto = statusDia(d) == StatusDia.ABERTO
                    val sel = d == dia
                    Surface(
                        onClick = { onEscolherData(d) },
                        shape = RoundedCornerShape(16.dp),
                        color = when { sel -> SkalaColors.Wine; aberto -> Color.White; else -> SkalaColors.Disabled },
                        border = BorderStroke(1.dp, if (sel) SkalaColors.Wine else SkalaColors.Border),
                        modifier = Modifier.weight(1f).height(70.dp).semantics {
                            contentDescription = d.textoLongo() + if (aberto) "" else ", fechado"
                        },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            val cor = when { sel -> Color.White; aberto -> SkalaColors.Wine; else -> Color(0xFFA08A92) }
                            Text(d.diaSemanaCurto(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cor)
                            Text("${d.dayOfMonth}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cor)
                            Box(
                                Modifier.size(5.dp).background(
                                    when { !aberto -> Color.Transparent; sel -> SkalaColors.RoseLight; else -> SkalaColors.Rose },
                                    CircleShape,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            when {
                dia == null || statusDia(dia) != StatusDia.ABERTO -> AvisoSemHorario(
                    "O salão não abre neste dia",
                    "Escolha outra data para ver os horários livres.",
                )
                livres.isEmpty() -> AvisoSemHorario("Dia lotado", "Todos os horários deste dia já foram reservados.")
                else -> {
                    listOf(
                        "Manhã" to livres.filter { it.hour < 12 },
                        "Tarde" to livres.filter { it.hour in 12..17 },
                        "Noite" to livres.filter { it.hour >= 18 },
                    ).filter { it.second.isNotEmpty() }.forEach { (nome, horas) ->
                        RotuloSecao(nome)
                        Spacer(Modifier.height(8.dp))
                        horas.chunked(4).forEach { linha ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                linha.forEach { h ->
                                    val sel = h == horaSelecionada
                                    Surface(
                                        onClick = { onEscolherHora(h) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (sel) SkalaColors.Rose else Color.White,
                                        border = BorderStroke(1.dp, if (sel) SkalaColors.Rose else SkalaColors.Border),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(h.texto(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (sel) Color.White else SkalaColors.Wine)
                                        }
                                    }
                                }
                                repeat(4 - linha.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Text(
                "Os horários seguem a agenda que o salão liberou para cada dia.",
                fontSize = 12.sp,
                color = SkalaColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = if (horaSelecionada == null) "Escolha um horário" else "Continuar",
            onClick = onContinuar,
            enabled = dia != null && horaSelecionada != null,
        )
    }
}

@Composable
private fun BotaoSeta(esquerda: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, SkalaColors.Border),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (esquerda) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (esquerda) "Semana anterior" else "Próxima semana",
                tint = if (habilitado) SkalaColors.Wine else SkalaColors.Placeholder,
            )
        }
    }
}

@Composable
private fun AvisoSemHorario(titulo: String, texto: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFD9C2C8), RoundedCornerShape(18.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(titulo, style = TituloTela.copy(fontSize = 22.sp), color = SkalaColors.Wine, textAlign = TextAlign.Center)
        Text(texto, fontSize = 14.sp, color = SkalaColors.Muted, textAlign = TextAlign.Center)
    }
}
