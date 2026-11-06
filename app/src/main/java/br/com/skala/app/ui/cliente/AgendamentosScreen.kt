package br.com.skala.app.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.StatusAgendamento
import br.com.skala.app.ui.components.Cartao
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.diaSemanaCurto
import br.com.skala.app.ui.components.mesCurto
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao
import java.time.LocalDateTime

@Composable
fun AgendamentosScreen(
    agendamentos: List<Agendamento>,
    onRemarcar: (Agendamento) -> Unit,
    onCancelar: (Agendamento) -> Unit,
    onAgendarDeNovo: (Agendamento) -> Unit,
) {
    var verProximos by rememberSaveable { mutableStateOf(true) }
    var cancelando by remember { mutableStateOf<Agendamento?>(null) }
    val agora = LocalDateTime.now()
    val (proximos, anteriores) = agendamentos.partition {
        it.status == StatusAgendamento.CONFIRMADO && LocalDateTime.of(it.data, it.inicio).isAfter(agora)
    }
    val lista = if (verProximos) proximos.sortedBy { LocalDateTime.of(it.data, it.inicio) }
    else anteriores.sortedByDescending { LocalDateTime.of(it.data, it.inicio) }

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 24.dp)) {
        TituloDaTela("Meus horários")
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().background(SkalaColors.Disabled, RoundedCornerShape(22.dp)).padding(4.dp)) {
            listOf(true to "Próximos", false to "Anteriores").forEach { (valor, rotulo) ->
                Surface(
                    onClick = { verProximos = valor },
                    shape = RoundedCornerShape(18.dp),
                    color = if (verProximos == valor) Color.White else Color.Transparent,
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(rotulo, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (lista.isEmpty()) {
            Text(
                if (verProximos) "Você não tem horários marcados." else "Nenhum atendimento anterior.",
                fontSize = 14.sp,
                color = SkalaColors.Muted,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(lista, key = { it.id }) { a ->
                Cartao(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                Modifier
                                    .size(width = 58.dp, height = 62.dp)
                                    .background(if (verProximos) SkalaColors.Rose else SkalaColors.Disabled, RoundedCornerShape(16.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                val cor = if (verProximos) Color.White else SkalaColors.Label
                                Text(a.data.mesCurto(), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = cor)
                                Text("${a.data.dayOfMonth}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = cor)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.titulo, style = TituloSecao.copy(fontSize = 21.sp), color = SkalaColors.Wine)
                                Text("${a.data.diaSemanaCurto()} · ${a.inicio.texto()} às ${a.fim.texto()}", fontSize = 13.sp, color = SkalaColors.Muted)
                            }
                            val cancelado = a.status == StatusAgendamento.CANCELADO
                            Text(
                                a.status.rotulo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = if (cancelado) SkalaColors.Muted else SkalaColors.RoseLink,
                                modifier = Modifier
                                    .background(if (cancelado) SkalaColors.Disabled else SkalaColors.Blush, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                        if (verProximos) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BotaoContorno("Remarcar", Modifier.weight(1f)) { onRemarcar(a) }
                                BotaoContorno("Cancelar", Modifier.weight(1f), SkalaColors.RoseLink) { cancelando = a }
                            }
                        } else {
                            BotaoContorno("Agendar de novo", Modifier.fillMaxWidth()) { onAgendarDeNovo(a) }
                        }
                    }
                }
            }
        }
    }

    cancelando?.let { a ->
        AlertDialog(
            onDismissRequest = { cancelando = null },
            title = { Text("Cancelar horário?") },
            text = { Text("${a.titulo} em ${a.data.textoLongo()}, às ${a.inicio.texto()}.") },
            confirmButton = {
                TextButton(onClick = { onCancelar(a); cancelando = null }) { Text("Cancelar horário", color = SkalaColors.RoseLink) }
            },
            dismissButton = {
                TextButton(onClick = { cancelando = null }) { Text("Manter", color = SkalaColors.Wine) }
            },
            containerColor = SkalaColors.Ivory,
        )
    }
}

@Composable
private fun BotaoContorno(texto: String, modifier: Modifier, cor: Color = SkalaColors.Wine, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, SkalaColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = cor),
    ) {
        Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
