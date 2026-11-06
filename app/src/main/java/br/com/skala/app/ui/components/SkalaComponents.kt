package br.com.skala.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.R
import br.com.skala.app.ui.theme.Marca
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloTela

private val FieldShape = RoundedCornerShape(14.dp)
private val PillShape = RoundedCornerShape(50)

/** A ilustração da logo (rosto com o cabelo em rosé). */
@Composable
fun SkalaLogo(modifier: Modifier = Modifier, altura: Dp = 158.dp, descricao: String? = "Logo Skala") {
    Image(
        painter = painterResource(R.drawable.skala_logo),
        contentDescription = descricao,
        modifier = modifier.height(altura),
    )
}

/** Logo completa empilhada: ilustração, "SKALA" e "SALÃO DE BELEZA". */
@Composable
fun SkalaLogoCompleta(modifier: Modifier = Modifier, alturaImagem: Dp = 158.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SkalaLogo(altura = alturaImagem)
        Spacer(Modifier.height(10.dp))
        Text("SKALA", style = Marca, color = SkalaColors.Wine)
        Spacer(Modifier.height(6.dp))
        Text("SALÃO DE BELEZA", fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp, color = SkalaColors.Muted)
    }
}

/** Versão horizontal da logo, para topos de tela. */
@Composable
fun SkalaLogoHorizontal(modifier: Modifier = Modifier, alturaImagem: Dp = 40.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SkalaLogo(altura = alturaImagem, descricao = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SKALA", style = Marca.copy(fontSize = 18.sp, letterSpacing = 5.sp), color = SkalaColors.Wine)
            Text("SALÃO DE BELEZA", fontSize = 7.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = SkalaColors.Muted)
        }
    }
}

@Composable
fun TituloDaTela(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = TituloTela, color = SkalaColors.Wine, modifier = modifier)
}

/** Rótulo pequeno em maiúsculas (ex.: "COMPRIMENTO *", "FAIXAS DE ATENDIMENTO"). */
@Composable
fun RotuloSecao(texto: String, modifier: Modifier = Modifier) {
    Text(texto.uppercase(), style = MaterialTheme.typography.labelSmall, color = SkalaColors.Label, modifier = modifier)
}

/** Cartão branco com borda suave. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    corFundo: Color = Color.White,
    borda: Color = SkalaColors.CardBorder,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(corFundo)
            .border(1.dp, borda, RoundedCornerShape(20.dp)),
        content = conteudo,
    )
}

/** Botão redondo de voltar. */
@Composable
fun BotaoVoltar(onClick: () -> Unit, modifier: Modifier = Modifier, descricao: String = "Voltar") {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, SkalaColors.Border),
        modifier = modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = descricao, tint = SkalaColors.Wine, modifier = Modifier.size(20.dp))
        }
    }
}

/** Topo das telas do agendamento: voltar + "PASSO X DE 4". */
@Composable
fun CabecalhoPasso(passo: Int, total: Int, onVoltar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BotaoVoltar(onClick = onVoltar)
        Text("PASSO $passo DE $total", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp, color = SkalaColors.Muted)
    }
}

/** Opção em formato de pílula; selecionada fica cor de vinho. */
@Composable
fun OpcaoPilula(
    texto: String,
    selecionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    altura: Dp = 40.dp,
) {
    Surface(
        onClick = onClick,
        shape = PillShape,
        color = if (selecionada) SkalaColors.Wine else Color.White,
        contentColor = if (selecionada) Color.White else SkalaColors.Wine,
        border = BorderStroke(1.dp, if (selecionada) SkalaColors.Wine else SkalaColors.Border),
        modifier = modifier.heightIn(min = altura),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Campo de texto com o rótulo acima, como no design. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RotuloSecao(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .semantics { contentDescription = label },
            placeholder = { Text(placeholder, color = SkalaColors.Placeholder, fontSize = 15.sp) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = singleLine,
            minLines = minLines,
            shape = FieldShape,
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it) } },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SkalaColors.Rose,
                unfocusedBorderColor = SkalaColors.Border,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                cursorColor = SkalaColors.Rose,
                focusedTextColor = SkalaColors.Wine,
                unfocusedTextColor = SkalaColors.Wine,
            ),
        )
    }
}

/** Campo de senha com botão para mostrar/ocultar. */
@Composable
fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visivel by rememberSaveable { mutableStateOf(false) }
    LabeledTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        errorText = errorText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = keyboardActions,
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visivel = !visivel }) {
                Icon(
                    imageVector = if (visivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(if (visivel) R.string.ocultar_senha else R.string.mostrar_senha),
                    tint = SkalaColors.Muted,
                )
            }
        },
    )
}

/** Botão principal: pílula rosé com texto em maiúsculas espaçadas. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    carregando: Boolean = false,
    cor: Color = SkalaColors.Rose,
) {
    Button(
        onClick = { if (!carregando) onClick() },
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = cor,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE3D3D7),
            disabledContentColor = SkalaColors.Muted,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp, disabledElevation = 0.dp),
    ) {
        if (carregando) {
            val descricao = stringResource(R.string.carregando)
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp).semantics { contentDescription = descricao },
            )
        } else {
            Text(text = text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp)
        }
    }
}

/** Botão secundário: pílula branca com borda nude. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    corTexto: Color = SkalaColors.Wine,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = PillShape,
        border = BorderStroke(1.dp, SkalaColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = corTexto),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** Caixa de erro (ex.: senha incorreta). */
@Composable
fun ErrorBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFDECEF),
        border = BorderStroke(1.dp, Color(0xFFF2C4CF)),
    ) {
        Text(
            text = text,
            color = SkalaColors.Error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** Link em texto, com área de toque de pelo menos 48dp. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, fontWeight: FontWeight = FontWeight.Medium) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = SkalaColors.RoseLink, fontSize = 13.sp, fontWeight = fontWeight)
    }
}

/** Rodapé "pergunta + link". */
@Composable
fun FooterPrompt(prompt: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = prompt, color = SkalaColors.Muted, fontSize = 13.sp)
        TextButton(onClick = onAction) {
            Text(text = action, color = SkalaColors.RoseLink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Divisória com um "ou" no meio. */
@Composable
fun OrDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SkalaColors.Border)
        Text(text = text, style = TituloTela.copy(fontSize = 15.sp), color = SkalaColors.Muted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = SkalaColors.Border)
    }
}

/** Círculo decorativo do fundo. */
@Composable
fun DecorCircle(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size).clip(CircleShape).background(color))
}

fun String.isEmailValido(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(trim()).matches()
