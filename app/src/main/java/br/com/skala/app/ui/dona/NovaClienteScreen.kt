package br.com.skala.app.ui.dona

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Cliente
import br.com.skala.app.ui.components.BotaoVoltar
import br.com.skala.app.ui.components.ErrorBanner
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.SecondaryButton
import br.com.skala.app.ui.components.TelefoneVisualTransformation
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.isEmailValido
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.SkalaTheme

/**
 * A dona cadastra uma cliente que não tem conta no app (ex.: marcou pelo WhatsApp).
 * Só nome e celular são obrigatórios.
 */
@Composable
fun NovaClienteScreen(
    /** Mensagem vinda do repositório, ex.: celular já cadastrado. */
    erro: String?,
    onVoltar: () -> Unit,
    onSalvar: (cliente: Cliente, reservarDepois: Boolean) -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var celular by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }

    val nomeOk = nome.isNotBlank()
    val celularOk = celular.length >= 10
    val emailOk = email.isBlank() || email.isEmailValido()

    fun salvar(reservar: Boolean) {
        tentouSalvar = true
        if (nomeOk && celularOk && emailOk) {
            onSalvar(Cliente(nome.trim(), celular, email.trim().lowercase()), reservar)
        }
    }

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 22.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotaoVoltar(onClick = onVoltar, descricao = "Voltar para clientes")
            Text("CLIENTES", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = SkalaColors.Muted)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TituloDaTela("Nova cliente")
            Text("Para clientes que marcam com você por telefone ou WhatsApp.", fontSize = 13.sp, color = SkalaColors.Muted)
            erro?.let { ErrorBanner(it) }
            LabeledTextField(
                label = "Nome",
                value = nome,
                onValueChange = { nome = it },
                placeholder = "Nome da cliente",
                errorText = if (tentouSalvar && !nomeOk) "Informe o nome" else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            LabeledTextField(
                label = "Celular",
                value = celular,
                onValueChange = { celular = it.filter(Char::isDigit).take(11) },
                placeholder = "(11) 91234-5678",
                errorText = if (tentouSalvar && !celularOk) "Informe o celular com DDD" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                visualTransformation = TelefoneVisualTransformation(),
            )
            LabeledTextField(
                label = "E-mail (opcional)",
                value = email,
                onValueChange = { email = it },
                placeholder = "cliente@exemplo.com",
                errorText = if (tentouSalvar && !emailOk) "E-mail inválido" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            )
            Text(
                "Se ela criar conta no app com o mesmo celular ou e-mail, vai ver os horários que você reservou.",
                fontSize = 12.sp,
                color = SkalaColors.Muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = "Salvar e reservar horário", onClick = { salvar(reservar = true) })
        Spacer(Modifier.height(8.dp))
        SecondaryButton(text = "Só salvar", onClick = { salvar(reservar = false) })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Nova cliente")
@Composable
private fun NovaClienteScreenPreview() {
    SkalaTheme {
        NovaClienteScreen(erro = null, onVoltar = {}, onSalvar = { _, _ -> })
    }
}
