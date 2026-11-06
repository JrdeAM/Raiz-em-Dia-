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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onHistorico: () -> Unit,
    onSair: () -> Unit,
) {
    var lembretes by rememberSaveable { mutableStateOf(true) }
    val celular = usuario.celular.let { c ->
        if (c.length == 11) "(${c.take(2)}) ${c.substring(2, 7)}-${c.takeLast(4)}" else c
    }

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
                ItemPerfil(Icons.Outlined.Person, "Meus dados", onClick = {})
                HorizontalDivider(color = SkalaColors.Divider)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = lembretes, role = Role.Switch, onValueChange = { lembretes = it })
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Lembretes de horário", fontSize = 15.sp, color = SkalaColors.Wine)
                        Text("Aviso no dia anterior ao atendimento", fontSize = 12.sp, color = SkalaColors.Muted)
                    }
                    Switch(
                        checked = lembretes,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = SkalaColors.Rose),
                    )
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
                    RotuloSecao("Skala Salão de Beleza")
                    Text("[Endereço do salão]", fontSize = 14.sp, color = SkalaColors.Wine)
                    Text("[WhatsApp do salão]", fontSize = 14.sp, color = SkalaColors.Wine)
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
