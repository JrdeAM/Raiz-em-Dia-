package br.com.skala.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.R
import br.com.skala.app.ui.components.DecorCircle
import br.com.skala.app.ui.components.ErrorBanner
import br.com.skala.app.ui.components.FooterPrompt
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.LinkButton
import br.com.skala.app.ui.components.OrDivider
import br.com.skala.app.ui.components.PasswordField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.SecondaryButton
import br.com.skala.app.ui.components.SkalaLogoCompleta
import br.com.skala.app.ui.components.isEmailValido
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.ui.theme.TituloTela

@Composable
fun LoginScreen(
    onLogin: (email: String, senha: String) -> Unit,
    onCriarConta: () -> Unit,
    onEsqueciSenha: () -> Unit,
    onEntrarComCelular: () -> Unit,
    carregando: Boolean = false,
    erro: String? = null,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    // Os erros só aparecem depois da primeira tentativa de entrar.
    var tentouEnviar by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val emailErro = if (tentouEnviar && !email.isEmailValido()) stringResource(R.string.erro_email) else null
    val senhaErro = if (tentouEnviar && senha.isBlank()) stringResource(R.string.erro_senha_vazia) else null

    fun enviar() {
        tentouEnviar = true
        if (email.isEmailValido() && senha.isNotBlank()) {
            focusManager.clearFocus()
            onLogin(email.trim(), senha)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkalaColors.Ivory),
    ) {
        // Arco suave atrás da logo
        DecorCircle(
            size = 460.dp,
            color = SkalaColors.Blush,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-250).dp),
        )

        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 12.dp),
            ) {
                SkalaLogoCompleta(modifier = Modifier.align(Alignment.CenterHorizontally))

                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.login_titulo),
                    style = TituloTela.copy(fontSize = 27.sp),
                    color = SkalaColors.Wine,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.login_subtitulo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkalaColors.Muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))
                LabeledTextField(
                    label = stringResource(R.string.campo_email),
                    value = email,
                    onValueChange = { email = it },
                    placeholder = stringResource(R.string.campo_email_placeholder),
                    errorText = emailErro,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                Spacer(Modifier.height(14.dp))
                PasswordField(
                    label = stringResource(R.string.campo_senha),
                    value = senha,
                    onValueChange = { senha = it },
                    placeholder = stringResource(R.string.login_senha_placeholder),
                    errorText = senhaErro,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { enviar() }),
                )
                LinkButton(
                    text = stringResource(R.string.login_esqueci_senha),
                    onClick = onEsqueciSenha,
                    modifier = Modifier.align(Alignment.End),
                )
                if (erro != null) {
                    ErrorBanner(text = erro)
                    Spacer(Modifier.height(12.dp))
                }
                PrimaryButton(text = stringResource(R.string.login_entrar), onClick = ::enviar, carregando = carregando)

                Spacer(Modifier.height(18.dp))
                OrDivider(text = stringResource(R.string.login_ou))
                Spacer(Modifier.height(14.dp))
                SecondaryButton(
                    text = stringResource(R.string.login_com_celular),
                    icon = Icons.Outlined.PhoneAndroid,
                    onClick = onEntrarComCelular,
                )

                // Empurra o rodapé para o fim da tela quando sobra espaço.
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(16.dp))
                FooterPrompt(
                    prompt = stringResource(R.string.login_sem_conta),
                    action = stringResource(R.string.login_criar_conta),
                    onAction = onCriarConta,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    SkalaTheme {
        LoginScreen(onLogin = { _, _ -> }, onCriarConta = {}, onEsqueciSenha = {}, onEntrarComCelular = {})
    }
}
