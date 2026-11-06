package br.com.skala.app.ui.dona

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Bloqueio
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.Faixa
import br.com.skala.app.data.StatusDia
import br.com.skala.app.ui.components.BotaoVoltar
import br.com.skala.app.ui.components.OpcaoPilula
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.RotuloSecao
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloTela
import java.time.LocalTime

/** Qual horário o relógio está editando: início/fim de uma faixa ou de um bloqueio. */
private data class Edicao(val bloqueio: Boolean, val indice: Int, val inicio: Boolean, val atual: LocalTime)

/** A dona define como o salão funciona nesse dia: aberto ou não, faixas, intervalo e bloqueios. */
@Composable
fun EditarDiaScreen(
    diaInicial: DiaFuncionamento,
    onVoltar: () -> Unit,
    onSalvar: (dia: DiaFuncionamento, repetirSemanas: Boolean) -> Unit,
) {
    var dia by remember(diaInicial) {
        mutableStateOf(
            // Dia ainda "a definir": já abre como aberto com uma faixa padrão para ela ajustar
            if (diaInicial.status == StatusDia.A_DEFINIR) {
                diaInicial.copy(status = StatusDia.ABERTO, faixas = listOf(Faixa(LocalTime.of(9, 0), LocalTime.of(18, 0))))
            } else {
                diaInicial
            },
        )
    }
    var edicao by remember { mutableStateOf<Edicao?>(null) }
    val aberto = dia.status == StatusDia.ABERTO
    val faixasValidas = dia.faixas.all { it.inicio.isBefore(it.fim) }

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 22.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotaoVoltar(onClick = onVoltar, descricao = "Voltar para o calendário")
            Text("EDITAR DIA", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = SkalaColors.Muted)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TituloDaTela(dia.data.textoLongo())

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(1.dp, SkalaColors.CardBorder, RoundedCornerShape(18.dp))
                    .toggleable(
                        value = aberto,
                        role = Role.Switch,
                        onValueChange = { abrir ->
                            dia = dia.copy(
                                status = if (abrir) StatusDia.ABERTO else StatusDia.FECHADO,
                                faixas = if (abrir && dia.faixas.isEmpty()) listOf(Faixa(LocalTime.of(9, 0), LocalTime.of(18, 0))) else dia.faixas,
                            )
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Salão aberto neste dia", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                    Text(
                        if (aberto) "As clientes veem os horários livres nas faixas abaixo" else "Folga: nenhum horário disponível",
                        fontSize = 12.sp,
                        color = SkalaColors.Muted,
                    )
                }
                Switch(checked = aberto, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = SkalaColors.Rose))
            }

            if (aberto) {
                RotuloSecao("Faixas de atendimento")
                dia.faixas.forEachIndexed { i, f ->
                    LinhaHorario(
                        inicio = f.inicio,
                        fim = f.fim,
                        onInicio = { edicao = Edicao(false, i, true, f.inicio) },
                        onFim = { edicao = Edicao(false, i, false, f.fim) },
                        onRemover = { dia = dia.copy(faixas = dia.faixas.filterIndexed { j, _ -> j != i }) },
                        descricaoRemover = "Remover faixa",
                    )
                }
                if (!faixasValidas) Text("O horário de início precisa ser antes do fim.", fontSize = 12.sp, color = SkalaColors.Error)
                BotaoTracejado("Adicionar faixa") {
                    val depois = dia.faixas.maxOfOrNull { it.fim } ?: LocalTime.of(9, 0)
                    val fim = if (depois.isBefore(LocalTime.of(21, 0))) depois.plusHours(2) else LocalTime.of(23, 0)
                    dia = dia.copy(faixas = dia.faixas + Faixa(depois, fim))
                }

                RotuloSecao("Intervalo entre atendimentos")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Nenhum", 10 to "10 min", 15 to "15 min", 30 to "30 min").forEach { (min, rotulo) ->
                        OpcaoPilula(rotulo, selecionada = dia.intervaloMin == min, onClick = { dia = dia.copy(intervaloMin = min) }, modifier = Modifier.weight(1f))
                    }
                }

                RotuloSecao("Horários bloqueados")
                dia.bloqueios.forEachIndexed { i, b ->
                    Row(
                        Modifier.fillMaxWidth().background(SkalaColors.Disabled, RoundedCornerShape(16.dp)).padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Block, contentDescription = null, tint = SkalaColors.Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                TextButton(onClick = { edicao = Edicao(true, i, true, b.inicio) }) { Text(b.inicio.texto(), color = SkalaColors.Wine, fontWeight = FontWeight.Medium) }
                                Text("às", fontSize = 13.sp, color = SkalaColors.Muted, modifier = Modifier.align(Alignment.CenterVertically))
                                TextButton(onClick = { edicao = Edicao(true, i, false, b.fim) }) { Text(b.fim.texto(), color = SkalaColors.Wine, fontWeight = FontWeight.Medium) }
                            }
                            Text(b.motivo.ifBlank { "Horário fechado" }, fontSize = 12.sp, color = SkalaColors.Muted, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp))
                        }
                        IconButton(onClick = { dia = dia.copy(bloqueios = dia.bloqueios.filterIndexed { j, _ -> j != i }) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remover bloqueio", tint = SkalaColors.RoseLink)
                        }
                    }
                }
                BotaoTracejado("Bloquear um horário") {
                    val base = dia.faixas.firstOrNull()?.inicio ?: LocalTime.of(12, 0)
                    dia = dia.copy(bloqueios = dia.bloqueios + Bloqueio(base, base.plusMinutes(30), "Compromisso pessoal"))
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFFD9C2C8), RoundedCornerShape(18.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Dia de folga", style = TituloTela.copy(fontSize = 22.sp), color = SkalaColors.Wine)
                    Text(
                        "Nenhum horário aparecerá para as clientes neste dia. Quem já tinha horário deve ser avisada para remarcar.",
                        fontSize = 13.sp,
                        color = SkalaColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = "Salvar dia", onClick = { onSalvar(dia, false) }, enabled = !aberto || (dia.faixas.isNotEmpty() && faixasValidas))
        TextButton(onClick = { onSalvar(dia, true) }, modifier = Modifier.fillMaxWidth(), enabled = !aberto || (dia.faixas.isNotEmpty() && faixasValidas)) {
            Text("Salvar e repetir nas próximas 4 semanas", color = SkalaColors.RoseLink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }

    edicao?.let { e ->
        RelogioDialog(
            titulo = if (e.inicio) "Começa às" else "Termina às",
            inicial = e.atual,
            onCancelar = { edicao = null },
            onConfirmar = { hora ->
                dia = if (e.bloqueio) {
                    dia.copy(bloqueios = dia.bloqueios.mapIndexed { j, b -> if (j != e.indice) b else if (e.inicio) b.copy(inicio = hora) else b.copy(fim = hora) })
                } else {
                    dia.copy(faixas = dia.faixas.mapIndexed { j, f -> if (j != e.indice) f else if (e.inicio) f.copy(inicio = hora) else f.copy(fim = hora) })
                }
                edicao = null
            },
        )
    }
}

@Composable
private fun LinhaHorario(
    inicio: LocalTime,
    fim: LocalTime,
    onInicio: () -> Unit,
    onFim: () -> Unit,
    onRemover: () -> Unit,
    descricaoRemover: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, SkalaColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Das", fontSize = 13.sp, color = SkalaColors.Muted)
        BotaoHora(inicio, onInicio)
        Text("às", fontSize = 13.sp, color = SkalaColors.Muted)
        BotaoHora(fim, onFim)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRemover) { Icon(Icons.Outlined.Close, contentDescription = descricaoRemover, tint = SkalaColors.RoseLink) }
    }
}

@Composable
private fun BotaoHora(hora: LocalTime, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = SkalaColors.Ivory, border = BorderStroke(1.dp, SkalaColors.Border)) {
        Text(hora.texto(), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
    }
}

@Composable
private fun BotaoTracejado(texto: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SkalaColors.Dashed),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkalaColors.RoseLink),
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelogioDialog(titulo: String, inicial: LocalTime, onCancelar: () -> Unit, onConfirmar: (LocalTime) -> Unit) {
    val estado = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            TimePicker(
                state = estado,
                colors = TimePickerDefaults.colors(
                    selectorColor = SkalaColors.Rose,
                    timeSelectorSelectedContainerColor = SkalaColors.Blush,
                    timeSelectorSelectedContentColor = SkalaColors.Wine,
                    clockDialColor = SkalaColors.Blush,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(LocalTime.of(estado.hour, estado.minute)) }) { Text("OK", color = SkalaColors.Rose) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar", color = SkalaColors.Wine) } },
        containerColor = SkalaColors.Ivory,
    )
}
