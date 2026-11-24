package br.com.skala.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.avisos.MensagemSimulada
import br.com.skala.app.ui.dona.formatarCelular
import br.com.skala.app.ui.theme.SkalaColors

private val BalaoWhatsApp = Color(0xFFDCF8C6)
private val TextoWhatsApp = Color(0xFF111B21)

/**
 * Mostra na tela a mensagem de WhatsApp que seria enviada para a cliente (o envio ainda é simulado).
 * Serve para conferir o texto no emulador; some quando houver um envio de verdade.
 */
@Composable
fun WhatsAppSimuladoDialog(mensagem: MensagemSimulada, onOk: () -> Unit) {
    val numero = mensagem.celular.removePrefix("55")
    AlertDialog(
        onDismissRequest = onOk,
        title = { Text("WhatsApp (simulação)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Para: +55 ${formatarCelular(numero)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SkalaColors.Wine,
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(BalaoWhatsApp, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                ) {
                    Text(mensagem.texto, fontSize = 14.sp, color = TextoWhatsApp)
                }
                Text("Nada foi enviado de verdade: é a mensagem que a cliente receberia.", fontSize = 12.sp, color = SkalaColors.Muted)
            }
        },
        confirmButton = { TextButton(onClick = onOk) { Text("OK", color = SkalaColors.Rose) } },
        containerColor = SkalaColors.Ivory,
    )
}
