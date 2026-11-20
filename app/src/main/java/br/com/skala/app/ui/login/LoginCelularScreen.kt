package br.com.skala.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.skala.app.R
import br.com.skala.app.ui.auth.EtapaCelular
import br.com.skala.app.ui.components.BotaoVoltar
import br.com.skala.app.ui.components.DecorCircle
import br.com.skala.app.ui.components.ErrorBanner
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.LinkButton
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.TelefoneVisualTransformation
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.dona.formatarCelular
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.SkalaTheme

/**
 * Login pelo celular em três etapas: número → código do SMS → nome (só na primeira vez do número).
 * `codigoTeste`: mostrado quando o app está sem Firebase (nenhum SMS é enviado).
 */
@Composable
fun LoginCelularScreen(
    etapa: EtapaCelular,
    celularDoCodigo: String,
    onEnviarCodigo: (celular: String) -> Unit,
    onConfirmarCodigo: (codigo: String) -> Unit,
    onConcluir: (nome: String) -> Unit,
    onTrocarNumero: () -> Unit,
    onVoltar: () -> Unit,
    carregando: Boolean = false,
    erro: String? = null,
    codigoTeste: String? = null,
) {
    var celular by rememberSaveable { mutableStateOf(celularDoCodigo) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var nome by rememberSaveable { mutableStateOf("") }
    var tentouEnviar by rememberSaveable(etapa) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun enviar() {
        tentouEnviar = true
        when (etapa) {
            EtapaCelular.NUMERO -> if (celular.length >= 10) {
                focusManager.clearFocus()
                codigo = ""
                onEnviarCodigo(celular)
            }
            EtapaCelular.CODIGO -> if (codigo.length == 6) {
                focusManager.clearFocus()
                onConfirmarCodigo(codigo)
            }
            EtapaCelular.NOME -> if (nome.isNotBlank()) {
                focusManager.clearFocus()
                onConcluir(nome.trim())
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SkalaColors.Ivory)) {
        DecorCircle(
            size = 380.dp,
            color = SkalaColors.Blush,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 160.dp, y = (-210).dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 16.dp),
        ) {
            BotaoVoltar(onClick = if (etapa == EtapaCelular.NUMERO) onVoltar else onTrocarNumero)
            Spacer(Modifier.height(24.dp))

            val (titulo, subtitulo) = when (etapa) {
                EtapaCelular.NUMERO -> stringResource(R.string.celular_titulo) to stringResource(R.string.celular_subtitulo)
                EtapaCelular.CODIGO -> stringResource(R.string.celular_codigo_titulo) to
                    stringResource(R.string.celular_codigo_subtitulo, formatarCelular(celularDoCodigo))
                EtapaCelular.NOME -> stringResource(R.string.celular_nome_titulo) to stringResource(R.string.celular_nome_subtitulo)
            }
            TituloDaTela(titulo)
            Spacer(Modifier.height(4.dp))
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = SkalaColors.Muted)
            Spacer(Modifier.height(22.dp))

            when (etapa) {
                EtapaCelular.NUMERO -> LabeledTextField(
                    label = stringResource(R.string.campo_celular),
                    value = celular,
                    onValueChange = { novo -> celular = novo.filter(Char::isDigit).take(11) },
                    placeholder = stringResource(R.string.campo_celular_placeholder),
                    errorText = if (tentouEnviar && celular.length < 10) stringResource(R.string.erro_celular) else null,
                    visualTransformation = TelefoneVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { enviar() }),
                )
                EtapaCelular.CODIGO -> {
                    LabeledTextField(
                        label = stringResource(R.string.celular_campo_codigo),
                        value = codigo,
                        onValueChange = { novo -> codigo = novo.filter(Char::isDigit).take(6) },
                        placeholder = "000000",
                        errorText = if (tentouEnviar && codigo.length != 6) stringResource(R.string.erro_codigo_curto) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { enviar() }),
                    )
                    if (codigoTeste != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.celular_codigo_teste, codigoTeste), style = MaterialTheme.typography.bodySmall, color = SkalaColors.Muted)
                    }
                }
                EtapaCelular.NOME -> LabeledTextField(
                    label = stringResource(R.string.campo_nome),
                    value = nome,
                    onValueChange = { nome = it },
                    placeholder = stringResource(R.string.campo_nome_placeholder),
                    errorText = if (tentouEnviar && nome.isBlank()) stringResource(R.string.erro_nome) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { enviar() }),
                )
            }

            Spacer(Modifier.height(18.dp))
            if (erro != null) {
                ErrorBanner(text = erro)
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(
                text = when (etapa) {
                    EtapaCelular.NUMERO -> stringResource(R.string.celular_enviar_codigo)
                    EtapaCelular.CODIGO -> stringResource(R.string.celular_confirmar)
                    EtapaCelular.NOME -> stringResource(R.string.celular_concluir)
                },
                onClick = ::enviar,
                carregando = carregando,
            )
            if (etapa == EtapaCelular.CODIGO) {
                Spacer(Modifier.height(6.dp))
                LinkButton(
                    text = stringResource(R.string.celular_reenviar),
                    onClick = { if (!carregando) onEnviarCodigo(celularDoCodigo) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                LinkButton(
                    text = stringResource(R.string.celular_trocar_numero),
                    onClick = onTrocarNumero,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginCelularCodigoPreview() {
    SkalaTheme {
        LoginCelularScreen(
            etapa = EtapaCelular.CODIGO,
            celularDoCodigo = "11912345678",
            onEnviarCodigo = {}, onConfirmarCodigo = {}, onConcluir = {}, onTrocarNumero = {}, onVoltar = {},
            codigoTeste = "123456",
        )
    }
}
