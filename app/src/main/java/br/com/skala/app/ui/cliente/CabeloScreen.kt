package br.com.skala.app.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Comprimento
import br.com.skala.app.data.DadosCabelo
import br.com.skala.app.data.Servico
import br.com.skala.app.data.TipoCabelo
import br.com.skala.app.data.Volume
import br.com.skala.app.ui.components.CabecalhoPasso
import br.com.skala.app.ui.components.OpcaoPilula
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.RotuloSecao
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao

/** Formulário obrigatório quando há serviço de cabelo: o valor depende do comprimento e do tipo. */
@Composable
fun CabeloScreen(
    /** Só os serviços marcados que dependem do cabelo (os outros têm preço fixo). */
    servicosDeCabelo: List<Servico>,
    cabelo: DadosCabelo,
    valorEstimado: String?,
    onAlterar: (DadosCabelo) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        CabecalhoPasso(passo = 2, total = 4, onVoltar = onVoltar)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                TituloDaTela("Conte sobre seu cabelo")
                Text(
                    "O valor de ${servicosDeCabelo.joinToString(" e ") { it.nome.lowercase() }} depende do comprimento e do tipo do seu cabelo.",
                    fontSize = 13.sp,
                    color = SkalaColors.Muted,
                )
            }

            RotuloSecao("Comprimento *")
            Comprimento.entries.chunked(2).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    linha.forEach { c ->
                        val sel = cabelo.comprimento == c
                        Surface(
                            onClick = { onAlterar(cabelo.copy(comprimento = if (sel) null else c)) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (sel) SkalaColors.Blush else Color.White,
                            border = BorderStroke(1.5.dp, if (sel) SkalaColors.Rose else SkalaColors.Border),
                            modifier = Modifier.weight(1f).heightIn(min = 58.dp).semantics { role = Role.RadioButton; selected = sel },
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                DesenhoComprimento(c)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(c.rotulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                                    Text(c.ajuda, fontSize = 11.sp, color = SkalaColors.Muted)
                                }
                            }
                        }
                    }
                }
            }

            RotuloSecao("Tipo de cabelo *")
            LinhaOpcoes(TipoCabelo.entries.map { it.rotulo }, cabelo.tipo?.rotulo) { r ->
                val t = TipoCabelo.entries.first { it.rotulo == r }
                onAlterar(cabelo.copy(tipo = if (cabelo.tipo == t) null else t))
            }

            RotuloSecao("Volume")
            LinhaOpcoes(Volume.entries.map { it.rotulo }, cabelo.volume?.rotulo) { r ->
                val v = Volume.entries.first { it.rotulo == r }
                onAlterar(cabelo.copy(volume = if (cabelo.volume == v) null else v))
            }

            RotuloSecao("Fez química nos últimos 6 meses?")
            LinhaOpcoes(listOf("Sim", "Não"), cabelo.fezQuimica?.let { if (it) "Sim" else "Não" }) { r ->
                val v = r == "Sim"
                onAlterar(cabelo.copy(fezQuimica = if (cabelo.fezQuimica == v) null else v))
            }

            // TODO: abrir a câmera/galeria e enviar a foto para o salão
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SkalaColors.Dashed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SkalaColors.RoseLink),
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar foto do cabelo (opcional)", fontSize = 13.sp)
            }

            val completo = cabelo.completo
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (completo) SkalaColors.Wine else SkalaColors.Disabled, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "TOTAL APROXIMADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = if (completo) SkalaColors.OnWineMuted else SkalaColors.Label,
                    )
                    Text(
                        if (completo) "Confirmado no salão após o procedimento" else "Escolha comprimento e tipo",
                        fontSize = 12.sp,
                        color = if (completo) SkalaColors.OnWineMuted else SkalaColors.Muted,
                    )
                }
                Text(
                    when {
                        !completo -> "—"
                        valorEstimado != null -> valorEstimado
                        else -> "a combinar"
                    },
                    style = TituloSecao,
                    color = if (completo) Color.White else SkalaColors.Muted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = if (cabelo.completo) "Continuar" else "Preencha os campos *",
            onClick = onContinuar,
            enabled = cabelo.completo,
        )
    }
}

@Composable
private fun LinhaOpcoes(opcoes: List<String>, selecionada: String?, onEscolher: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opcoes.forEach { o ->
            OpcaoPilula(o, selecionada = o == selecionada, onClick = { onEscolher(o) }, modifier = Modifier.weight(1f))
        }
    }
}

/** Cabeça com duas linhas descendo até a altura do comprimento escolhido. */
@Composable
private fun DesenhoComprimento(c: Comprimento) {
    val fim = when (c) {
        Comprimento.CURTO -> 0.47f
        Comprimento.MEDIO -> 0.63f
        Comprimento.LONGO -> 0.8f
        Comprimento.EXTRA_LONGO -> 0.94f
    }
    Canvas(Modifier.size(width = 22.dp, height = 30.dp)) {
        val cor = SkalaColors.Rose
        val r = size.width * 0.23f
        drawCircle(cor, radius = r, center = Offset(size.width / 2, r + 2f), style = Stroke(width = 1.4.dp.toPx()))
        val y0 = size.height * 0.3f
        listOf(size.width * 0.23f, size.width * 0.77f).forEach { x ->
            drawLine(cor, Offset(x, y0), Offset(x, size.height * fim), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
