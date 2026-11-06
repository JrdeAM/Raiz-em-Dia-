package br.com.skala.app.ui.cadastro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.R
import br.com.skala.app.data.DadosCadastro
import br.com.skala.app.ui.components.BotaoVoltar
import br.com.skala.app.ui.components.DecorCircle
import br.com.skala.app.ui.components.SkalaLogoHorizontal
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.ErrorBanner
import br.com.skala.app.ui.components.FooterPrompt
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.PasswordField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.TelefoneVisualTransformation
import br.com.skala.app.ui.components.isEmailValido
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.SkalaTheme

@Composable
fun CadastroScreen(
    onCadastrar: (DadosCadastro) -> Unit,
    onVoltar: () -> Unit,
    onEntrar: () -> Unit,
    onAbrirTermos: () -> Unit,
    onAbrirPrivacidade: () -> Unit,
    carregando: Boolean = false,
    erro: String? = null,
) {
    var nome by rememberSaveable { mutableStateOf("") }
    var celular by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmar by rememberSaveable { mutableStateOf("") }
    var aceite by rememberSaveable { mutableStateOf(false) }
    var tentouEnviar by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val nomeOk = nome.isNotBlank()
    val celularOk = celular.length >= 10
    val emailOk = email.isEmailValido()
    val senhaOk = senha.length >= 8
    val confirmarOk = confirmar == senha && confirmar.isNotEmpty()

    val nomeErro = if (tentouEnviar && !nomeOk) stringResource(R.string.erro_nome) else null
    val celularErro = if (tentouEnviar && !celularOk) stringResource(R.string.erro_celular) else null
    val emailErro = if (tentouEnviar && !emailOk) stringResource(R.string.erro_email) else null
    val senhaErro = if (tentouEnviar && !senhaOk) stringResource(R.string.erro_senha_curta) else null
    val confirmarErro = if (tentouEnviar && !confirmarOk) stringResource(R.string.erro_senha_diferente) else null
    val termosErro = tentouEnviar && !aceite

    fun enviar() {
        tentouEnviar = true
        if (nomeOk && celularOk && emailOk && senhaOk && confirmarOk && aceite) {
            focusManager.clearFocus()
            onCadastrar(DadosCadastro(nome.trim(), celular, email.trim(), senha))
        }
    }

    val avancar = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkalaColors.Ivory),
    ) {
        DecorCircle(
            size = 380.dp,
            color = SkalaColors.Blush,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 160.dp, y = (-210).dp),
        )

        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 16.dp),
            ) {
                TopBar(onVoltar = onVoltar)

                Spacer(Modifier.height(24.dp))
                TituloDaTela(stringResource(R.string.cadastro_titulo))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.cadastro_subtitulo), style = MaterialTheme.typography.bodyMedium, color = SkalaColors.Muted)

                Spacer(Modifier.height(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledTextField(
                        label = stringResource(R.string.campo_nome),
                        value = nome,
                        onValueChange = { nome = it },
                        placeholder = stringResource(R.string.campo_nome_placeholder),
                        errorText = nomeErro,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = avancar,
                    )
                    LabeledTextField(
                        label = stringResource(R.string.campo_celular),
                        value = celular,
                        onValueChange = { novo -> celular = novo.filter(Char::isDigit).take(11) },
                        placeholder = stringResource(R.string.campo_celular_placeholder),
                        errorText = celularErro,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = avancar,
                        visualTransformation = TelefoneVisualTransformation(),
                    )
                    LabeledTextField(
                        label = stringResource(R.string.campo_email),
                        value = email,
                        onValueChange = { email = it },
                        placeholder = stringResource(R.string.campo_email_placeholder),
                        errorText = emailErro,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = avancar,
                    )
                    PasswordField(
                        label = stringResource(R.string.campo_senha),
                        value = senha,
                        onValueChange = { senha = it },
                        placeholder = stringResource(R.string.cadastro_senha_placeholder),
                        errorText = senhaErro,
                        imeAction = ImeAction.Next,
                        keyboardActions = avancar,
                    )
                    PasswordField(
                        label = stringResource(R.string.campo_confirmar_senha),
                        value = confirmar,
                        onValueChange = { confirmar = it },
                        placeholder = stringResource(R.string.cadastro_confirmar_placeholder),
                        errorText = confirmarErro,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )

                    TermosCheckbox(
                        aceite = aceite,
                        onAceiteChange = { aceite = it },
                        mostrarErro = termosErro,
                        onAbrirTermos = onAbrirTermos,
                        onAbrirPrivacidade = onAbrirPrivacidade,
                    )

                    if (erro != null) {
                        ErrorBanner(text = erro)
                    }
                    PrimaryButton(text = stringResource(R.string.cadastro_criar), onClick = ::enviar, carregando = carregando)
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(20.dp))
                FooterPrompt(
                    prompt = stringResource(R.string.cadastro_ja_tem_conta),
                    action = stringResource(R.string.cadastro_entrar),
                    onAction = onEntrar,
                )
            }
        }
    }
}

@Composable
private fun TopBar(onVoltar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BotaoVoltar(onClick = onVoltar, descricao = stringResource(R.string.cadastro_voltar))
        SkalaLogoHorizontal(alturaImagem = 44.dp)
        // Equilibra o botão de voltar para a logo ficar centralizada.
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun TermosCheckbox(
    aceite: Boolean,
    onAceiteChange: (Boolean) -> Unit,
    mostrarErro: Boolean,
    onAbrirTermos: () -> Unit,
    onAbrirPrivacidade: () -> Unit,
) {
    val estiloLink = TextLinkStyles(SpanStyle(color = SkalaColors.RoseLink, fontWeight = FontWeight.Bold))
    val texto = buildAnnotatedString {
        append(stringResource(R.string.termos_prefixo))
        withLink(LinkAnnotation.Clickable("termos", estiloLink) { onAbrirTermos() }) {
            append(stringResource(R.string.termos_link))
        }
        append(stringResource(R.string.termos_meio))
        withLink(LinkAnnotation.Clickable("privacidade", estiloLink) { onAbrirPrivacidade() }) {
            append(stringResource(R.string.privacidade_link))
        }
        append(stringResource(R.string.termos_sufixo))
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = aceite, role = Role.Checkbox, onValueChange = onAceiteChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = aceite,
                onCheckedChange = null, // o Row inteiro já alterna o valor
                modifier = Modifier.padding(12.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = SkalaColors.Rose,
                    uncheckedColor = if (mostrarErro) SkalaColors.Error else SkalaColors.Muted,
                ),
            )
            Text(text = texto, fontSize = 14.sp, lineHeight = 20.sp, color = SkalaColors.Label)
        }
        if (mostrarErro) {
            Text(
                text = stringResource(R.string.erro_termos),
                color = SkalaColors.Error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun CadastroScreenPreview() {
    SkalaTheme {
        CadastroScreen(onCadastrar = {}, onVoltar = {}, onEntrar = {}, onAbrirTermos = {}, onAbrirPrivacidade = {})
    }
}
