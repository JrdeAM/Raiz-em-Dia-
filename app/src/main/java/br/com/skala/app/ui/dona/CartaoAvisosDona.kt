package br.com.skala.app.ui.dona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.avisos.textoAvisoDona
import br.com.skala.app.data.AvisoDona
import br.com.skala.app.data.TipoAvisoDona
import br.com.skala.app.ui.components.Cartao
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import java.time.Duration
import java.time.LocalDateTime

private const val MAX_VISIVEIS = 5

/**
 * Avisos novos no topo da agenda da dona: quem marcou, desmarcou ou remarcou pelo app.
 * Tocar num aviso abre o dia do horário; "Marcar como lidos" esconde o cartão.
 */
@Composable
fun CartaoAvisosDona(
    avisos: List<AvisoDona>,
    onAbrir: (AvisoDona) -> Unit,
    onMarcarLidos: () -> Unit,
) {
    Cartao(Modifier.fillMaxWidth(), corFundo = SkalaColors.Blush, borda = SkalaColors.RoseLight) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (avisos.size == 1) "1 aviso novo" else "${avisos.size} avisos novos",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SkalaColors.Wine,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onMarcarLidos) {
                Text("Marcar como lidos", fontSize = 13.sp, color = SkalaColors.RoseLink)
            }
        }
        avisos.take(MAX_VISIVEIS).forEach { aviso ->
            HorizontalDivider(color = SkalaColors.RoseLight.copy(alpha = 0.5f))
            LinhaAviso(aviso, onClick = { onAbrir(aviso) })
        }
        if (avisos.size > MAX_VISIVEIS) {
            Text(
                "e mais ${avisos.size - MAX_VISIVEIS}",
                fontSize = 12.sp,
                color = SkalaColors.Muted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LinhaAviso(aviso: AvisoDona, onClick: () -> Unit) {
    val icone = when (aviso.tipo) {
        TipoAvisoDona.MARCOU -> Icons.Outlined.EventAvailable
        TipoAvisoDona.DESMARCOU -> Icons.Outlined.EventBusy
        TipoAvisoDona.REMARCOU -> Icons.Outlined.EventRepeat
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(SkalaColors.Ivory, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = aviso.tipo.verbo, tint = SkalaColors.Rose, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(textoAvisoDona(aviso), fontSize = 13.sp, lineHeight = 18.sp, color = SkalaColors.Wine)
            Text(haQuantoTempo(aviso.quando), fontSize = 11.sp, color = SkalaColors.Muted)
        }
    }
}

/** "agora", "há 5 min", "há 2 h", "há 3 dias" */
private fun haQuantoTempo(quando: LocalDateTime): String {
    val minutos = Duration.between(quando, LocalDateTime.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        minutos < 60 * 24 -> "há ${minutos / 60} h"
        else -> "há ${minutos / (60 * 24)} ${if (minutos / (60 * 24) == 1L) "dia" else "dias"}"
    }
}

@Preview(showBackground = true, widthDp = 390, name = "Avisos da dona")
@Composable
private fun CartaoAvisosDonaPreview() {
    SkalaTheme {
        CartaoAvisosDona(avisos = FakeSalaoRepository.avisosDona.value, onAbrir = {}, onMarcarLidos = {})
    }
}
