package br.com.skala.app.ui.cliente

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.DadosSalao
import br.com.skala.app.data.Usuario
import br.com.skala.app.ui.components.Cartao
import br.com.skala.app.ui.components.DecorCircle
import br.com.skala.app.ui.components.RotuloSecao
import br.com.skala.app.ui.theme.BrandFontFamily
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao

/** Perfil compartilhado: a cliente vê o histórico; a dona vê o atalho para os horários do salão. */
@Composable
fun PerfilScreen(
    usuario: Usuario,
    salao: DadosSalao,
    onHistorico: () -> Unit,
    onSair: () -> Unit,
    onMeusDados: () -> Unit = {},
) {
    val celular = usuario.celular.comoTelefone()

    Box(Modifier.fillMaxSize().background(SkalaColors.Ivory)) {
        DecorCircle(480.dp, SkalaColors.Blush, Modifier.align(Alignment.TopCenter).offset(y = (-300).dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .background(SkalaColors.Rose, CircleShape)
                    .border(4.dp, SkalaColors.Ivory, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(usuario.nome.take(1).uppercase(), fontFamily = BrandFontFamily, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(usuario.nome, style = TituloSecao.copy(fontSize = 26.sp), color = SkalaColors.Wine)
            Text("${usuario.email} · $celular", fontSize = 14.sp, color = SkalaColors.Muted)

            Spacer(Modifier.height(24.dp))
            Cartao(Modifier.fillMaxWidth()) {
                ItemPerfil(Icons.Outlined.Person, "Meus dados", onClick = onMeusDados)
                HorizontalDivider(color = SkalaColors.Divider)
                // Os lembretes são obrigatórios: só informa, não dá para desligar.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Lembretes de horário", fontSize = 15.sp, color = SkalaColors.Wine)
                        Text(
                            if (usuario.ehDona) {
                                "Para você não perder a hora, avisaremos sempre que um cliente marcar, desmarcar ou remarcar um horário."
                            } else {
                                "Para você não perder o seu momento: avisamos 2 dias, 1 dia, 5 horas e 1 hora antes do seu atendimento."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = SkalaColors.Muted,
                        )
                    }
                }
                HorizontalDivider(color = SkalaColors.Divider)
                if (usuario.ehDona) {
                    ItemPerfil(Icons.Outlined.CalendarMonth, "Horários de funcionamento", onClick = onHistorico)
                } else {
                    ItemPerfil(Icons.Outlined.AccessTime, "Histórico de atendimentos", onClick = onHistorico)
                }
                HorizontalDivider(color = SkalaColors.Divider)
                ItemPerfil(Icons.Outlined.Shield, "Termos e privacidade", onClick = {})
            }

            Spacer(Modifier.height(16.dp))
            Cartao(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloSecao(salao.nome.ifBlank { "Salão" })
                    if (salao.endereco.isNotBlank()) Text(salao.endereco, fontSize = 14.sp, color = SkalaColors.Wine)
                    if (salao.whatsapp.isNotBlank()) Text("WhatsApp ${salao.whatsapp.comoTelefone()}", fontSize = 14.sp, color = SkalaColors.Wine)
                    if (usuario.ehDona && (salao.endereco.isBlank() || salao.whatsapp.isBlank())) {
                        Text("Complete endereço e WhatsApp em Meus dados", fontSize = 12.sp, color = SkalaColors.Muted)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onSair,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, SkalaColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = SkalaColors.RoseLink),
            ) {
                Text("Sair da conta", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** "11912345678" → "(11) 91234-5678". */
private fun String.comoTelefone(): String = when (length) {
    11 -> "(${take(2)}) ${substring(2, 7)}-${takeLast(4)}"
    10 -> "(${take(2)}) ${substring(2, 6)}-${takeLast(4)}"
    else -> this
}

@Composable
private fun ItemPerfil(icone: ImageVector, texto: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(texto, fontSize = 15.sp, color = SkalaColors.Wine, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = SkalaColors.Placeholder)
        }
    }
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Perfil · cliente")
@Composable
private fun PerfilClientePreview() {
    SkalaTheme {
        PerfilScreen(usuario = Usuario(nome = "Maria Clara", email = "cliente@skala.com", celular = "11912345678"), salao = FakeSalaoRepository.dadosSalao.value, onHistorico = {}, onSair = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Perfil · dona")
@Composable
private fun PerfilDonaPreview() {
    SkalaTheme {
        PerfilScreen(usuario = Usuario(nome = "Carla Mendes", email = "dona@skala.com", celular = "11987654321", ehDona = true), salao = FakeSalaoRepository.dadosSalao.value, onHistorico = {}, onSair = {})
    }
}
