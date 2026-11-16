package br.com.skala.app.ui.cliente

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.DadosSalao
import br.com.skala.app.data.Usuario
import br.com.skala.app.ui.components.BotaoVoltar
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.RotuloSecao
import br.com.skala.app.ui.components.TelefoneVisualTransformation
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.theme.SkalaColors

/**
 * Dados da conta, usados pela cliente e pela dona. A dona também edita os dados do salão,
 * que aparecem para as clientes no perfil.
 */
@Composable
fun MeusDadosScreen(
    usuario: Usuario,
    salao: DadosSalao,
    onVoltar: () -> Unit,
    /** `salao` vem null para a cliente, que não edita os dados do salão. */
    onSalvar: (nome: String, celular: String, salao: DadosSalao?) -> Unit,
) {
    var nome by rememberSaveable { mutableStateOf(usuario.nome) }
    var celular by rememberSaveable { mutableStateOf(usuario.celular) }
    var nomeSalao by rememberSaveable { mutableStateOf(salao.nome) }
    var endereco by rememberSaveable { mutableStateOf(salao.endereco) }
    var whatsapp by rememberSaveable { mutableStateOf(salao.whatsapp) }
    var tentouSalvar by rememberSaveable { mutableStateOf(false) }

    val nomeOk = nome.isNotBlank()
    val celularOk = celular.length >= 10
    val ehDona = usuario.ehDona
    val nomeSalaoOk = !ehDona || nomeSalao.isNotBlank()
    val whatsappOk = !ehDona || whatsapp.isEmpty() || whatsapp.length >= 10
    val telefone = TelefoneVisualTransformation()

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 22.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BotaoVoltar(onClick = onVoltar, descricao = "Voltar para o perfil")
            Text("PERFIL", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = SkalaColors.Muted)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TituloDaTela("Meus dados")

            RotuloSecao("Seus dados")
            LabeledTextField(
                label = "Nome",
                value = nome,
                onValueChange = { nome = it },
                placeholder = "Seu nome",
                errorText = if (tentouSalvar && !nomeOk) "Informe seu nome" else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            LabeledTextField(
                label = "Celular",
                value = celular,
                onValueChange = { celular = it.filter(Char::isDigit).take(11) },
                placeholder = "(11) 91234-5678",
                errorText = if (tentouSalvar && !celularOk) "Informe o celular com DDD" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                visualTransformation = telefone,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RotuloSecao("E-mail")
                Text(usuario.email, fontSize = 15.sp, color = SkalaColors.Wine)
                Text("É o seu login, por isso não muda por aqui.", fontSize = 12.sp, color = SkalaColors.Muted)
            }

            if (ehDona) {
                Spacer(Modifier.height(4.dp))
                RotuloSecao("Dados do salão")
                Text("Aparecem para as clientes no perfil delas.", fontSize = 12.sp, color = SkalaColors.Muted)
                LabeledTextField(
                    label = "Nome do salão",
                    value = nomeSalao,
                    onValueChange = { nomeSalao = it },
                    placeholder = "Ex.: Skala Salão de Beleza",
                    errorText = if (tentouSalvar && !nomeSalaoOk) "Informe o nome do salão" else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                )
                LabeledTextField(
                    label = "Endereço",
                    value = endereco,
                    onValueChange = { endereco = it },
                    placeholder = "Rua, número, bairro, cidade",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    singleLine = false,
                    minLines = 2,
                )
                LabeledTextField(
                    label = "WhatsApp do salão",
                    value = whatsapp,
                    onValueChange = { whatsapp = it.filter(Char::isDigit).take(11) },
                    placeholder = "(11) 91234-5678",
                    errorText = if (tentouSalvar && !whatsappOk) "Informe o número com DDD" else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    visualTransformation = telefone,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "Salvar dados",
            onClick = {
                tentouSalvar = true
                if (nomeOk && celularOk && nomeSalaoOk && whatsappOk) {
                    onSalvar(nome.trim(), celular, if (ehDona) DadosSalao(nomeSalao.trim(), endereco.trim(), whatsapp) else null)
                }
            },
        )
    }
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Meus dados · dona")
@Composable
private fun MeusDadosDonaPreview() {
    SkalaTheme {
        MeusDadosScreen(usuario = Usuario(nome = "Carla Mendes", email = "dona@skala.com", celular = "11987654321", ehDona = true), salao = FakeSalaoRepository.dadosSalao.value, onVoltar = {}, onSalvar = { _, _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Meus dados · cliente")
@Composable
private fun MeusDadosClientePreview() {
    SkalaTheme {
        MeusDadosScreen(usuario = Usuario(nome = "Maria Clara", email = "cliente@skala.com", celular = "11912345678"), salao = FakeSalaoRepository.dadosSalao.value, onVoltar = {}, onSalvar = { _, _, _ -> })
    }
}
