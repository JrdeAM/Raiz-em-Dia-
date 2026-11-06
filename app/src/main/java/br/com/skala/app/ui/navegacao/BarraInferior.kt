package br.com.skala.app.ui.navegacao

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.ui.theme.SkalaColors

data class ItemBarra(val rota: String, val rotulo: String, val icone: ImageVector)

val itensCliente = listOf(
    ItemBarra(Rotas.INICIO, "Início", Icons.Outlined.Home),
    ItemBarra(Rotas.SERVICOS, "Agendar", Icons.Outlined.CalendarMonth),
    ItemBarra(Rotas.AGENDAMENTOS, "Meus horários", Icons.Outlined.AccessTime),
    ItemBarra(Rotas.PERFIL, "Perfil", Icons.Outlined.Person),
)

val itensDona = listOf(
    ItemBarra(Rotas.DONA_AGENDA, "Agenda", Icons.AutoMirrored.Outlined.List),
    ItemBarra(Rotas.DONA_HORARIOS, "Horários", Icons.Outlined.CalendarMonth),
    ItemBarra(Rotas.DONA_PERFIL, "Perfil", Icons.Outlined.Person),
)

@Composable
fun BarraInferior(itens: List<ItemBarra>, rotaAtual: String?, onNavegar: (String) -> Unit) {
    Column {
        HorizontalDivider(color = SkalaColors.CardBorder)
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            itens.forEach { item ->
                NavigationBarItem(
                    selected = rotaAtual == item.rota,
                    onClick = { if (rotaAtual != item.rota) onNavegar(item.rota) },
                    icon = { Icon(item.icone, contentDescription = null) },
                    label = { Text(item.rotulo, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkalaColors.Rose,
                        selectedTextColor = SkalaColors.Rose,
                        indicatorColor = SkalaColors.Blush,
                        unselectedIconColor = SkalaColors.Muted,
                        unselectedTextColor = SkalaColors.Muted,
                    ),
                )
            }
        }
    }
}
