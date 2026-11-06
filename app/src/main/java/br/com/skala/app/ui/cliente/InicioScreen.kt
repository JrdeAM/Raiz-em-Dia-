package br.com.skala.app.ui.cliente

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.ui.components.DecorCircle
import br.com.skala.app.ui.components.SkalaLogoHorizontal
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao
import br.com.skala.app.ui.theme.TituloTela

private data class Atalho(val rotulo: String, val icone: ImageVector, val servicoId: String?, val categoria: String?)

private val atalhos = listOf(
    Atalho("Cabelo", Icons.Outlined.ContentCut, null, "Cabelo"),
    Atalho("Coloração", Icons.Outlined.WaterDrop, "coloracao", null),
    Atalho("Sobrancelha", Icons.Outlined.Face, null, "Sobrancelha"),
    Atalho("Maquiagem", Icons.Outlined.Brush, null, "Maquiagem"),
)

@Composable
fun InicioScreen(
    nome: String,
    proximo: Agendamento?,
    onAgendar: () -> Unit,
    onAtalho: (servicoId: String?, categoria: String?) -> Unit,
    onVerHorarios: () -> Unit,
    onPerfil: () -> Unit,
) {
    val primeiroNome = nome.trim().substringBefore(' ')
    Box(Modifier.fillMaxSize().background(SkalaColors.Ivory)) {
        DecorCircle(380.dp, SkalaColors.Blush, Modifier.align(Alignment.TopEnd).offset(x = 150.dp, y = (-200).dp))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SkalaLogoHorizontal()
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onPerfil,
                    shape = CircleShape,
                    color = SkalaColors.Rose,
                    modifier = Modifier.size(44.dp).semantics { contentDescription = "Abrir perfil" },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(primeiroNome.take(1).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Olá, $primeiroNome", style = TituloTela.copy(fontSize = 32.sp), color = SkalaColors.Wine)
            Text("Vamos cuidar de você?", fontSize = 15.sp, color = SkalaColors.Muted)

            Spacer(Modifier.height(18.dp))
            if (proximo != null) {
                CartaoProximo(proximo, onVerHorarios)
            } else {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, SkalaColors.CardBorder)) {
                    Text(
                        "Você ainda não tem horário marcado.",
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        fontSize = 14.sp,
                        color = SkalaColors.Muted,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAgendar,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = SkalaColors.Wine, contentColor = Color.White),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("AGENDAR HORÁRIO", fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.5.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Serviços", style = TituloSecao, color = SkalaColors.Wine)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAgendar) { Text("Ver todos", color = SkalaColors.RoseLink, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(6.dp))
            atalhos.chunked(2).forEach { linha ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    linha.forEach { a ->
                        Surface(
                            onClick = { onAtalho(a.servicoId, a.categoria) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, SkalaColors.CardBorder),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                Modifier.padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    Modifier.size(40.dp).background(SkalaColors.Blush, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(a.icone, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(20.dp))
                                }
                                Text(a.rotulo, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CartaoProximo(ag: Agendamento, onVerHorarios: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SkalaColors.Rose, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("SEU PRÓXIMO HORÁRIO", fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = Color(0xFFF6E3E7))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ag.titulo, style = TituloSecao.copy(fontSize = 26.sp), color = Color.White)
                Text(ag.data.textoLongo(), fontSize = 14.sp, color = Color(0xFFF6E3E7))
            }
            Column(
                Modifier.background(SkalaColors.Ivory, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(ag.inicio.texto(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Wine)
                Text(ag.duracaoTexto, fontSize = 11.sp, color = SkalaColors.Muted)
            }
        }
        TextButton(onClick = onVerHorarios, contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text("Ver meus horários", color = Color.White, fontSize = 13.sp, textDecoration = TextDecoration.Underline)
        }
    }
}
