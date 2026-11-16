package br.com.skala.app.ui.dona

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import java.time.LocalDate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.ui.components.diaSemanaCurto
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.theme.SkalaColors
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM")

/** Avisa a dona que a mudança de horário deixa clientes já agendadas com o salão fechado. */
@Composable
fun AvisoConflitoDialog(
    conflitos: List<Agendamento>,
    onAjustar: () -> Unit,
    onSalvarMesmoAssim: () -> Unit,
) {
    val variosDias = conflitos.map { it.data }.distinct().size > 1
    val quantas = if (conflitos.size == 1) "1 cliente agendada" else "${conflitos.size} clientes agendadas"
    AlertDialog(
        onDismissRequest = onAjustar,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = SkalaColors.Rose) },
        title = { Text("Há clientes nesse horário") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Com essa mudança, $quantas ficaria com o salão fechado. Avise antes para remarcar.",
                    fontSize = 14.sp,
                    color = SkalaColors.Muted,
                )
                Column(
                    Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    conflitos.forEach { a ->
                        val quando = buildString {
                            if (variosDias) append("${a.data.diaSemanaCurto()} ${a.data.format(formatoData)} · ")
                            append("${a.inicio.texto()} às ${a.fim.texto()}")
                        }
                        Column {
                            Text(a.clienteNome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                            Text("$quando · ${a.titulo}", fontSize = 13.sp, color = SkalaColors.Muted)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onAjustar) { Text("Voltar e ajustar", color = SkalaColors.Rose) } },
        dismissButton = { TextButton(onClick = onSalvarMesmoAssim) { Text("Salvar mesmo assim", color = SkalaColors.Wine) } },
        containerColor = SkalaColors.Ivory,
    )
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Aviso de conflito")
@Composable
private fun AvisoConflitoDialogPreview() {
    SkalaTheme {
        AvisoConflitoDialog(
            conflitos = FakeSalaoRepository.agendamentos.value.filter { it.data == LocalDate.now() },
            onAjustar = {},
            onSalvarMesmoAssim = {},
        )
    }
}
