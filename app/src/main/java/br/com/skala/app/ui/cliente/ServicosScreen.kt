package br.com.skala.app.ui.cliente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.FaixaPreco
import br.com.skala.app.data.Servico
import br.com.skala.app.data.duracaoTotalMin
import br.com.skala.app.data.textoDuracao
import br.com.skala.app.ui.components.CabecalhoPasso
import br.com.skala.app.ui.components.OpcaoPilula
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao

/** A cliente pode marcar vários serviços para o mesmo horário; o app soma tempo e valor aproximado. */
@Composable
fun ServicosScreen(
    servicos: List<Servico>,
    categoriaInicial: String?,
    selecionadosIniciais: List<String>,
    /** Valor aproximado de um serviço com preço fixo (os de cabelo dependem do formulário). */
    precoFixo: (Servico) -> FaixaPreco?,
    /** Soma aproximada dos marcados, quando já dá para calcular sem o formulário do cabelo. */
    totalAproximado: (List<Servico>) -> FaixaPreco?,
    onVoltar: () -> Unit,
    onContinuar: (List<Servico>) -> Unit,
) {
    val categorias = listOf("Todos") + servicos.map { it.categoria }.distinct()
    var categoria by rememberSaveable { mutableStateOf(categoriaInicial ?: "Todos") }
    var marcadosIds by rememberSaveable { mutableStateOf(selecionadosIniciais) }
    val visiveis = servicos.filter { categoria == "Todos" || it.categoria == categoria }
    // Mantém a ordem da lista do salão, que é a ordem em que os serviços são feitos
    val marcados = servicos.filter { it.id in marcadosIds }

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        CabecalhoPasso(passo = 1, total = 4, onVoltar = onVoltar)
        Spacer(Modifier.height(16.dp))
        TituloDaTela("Escolha os serviços")
        Text("Você pode marcar mais de um para o mesmo horário.", fontSize = 13.sp, color = SkalaColors.Muted)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categorias.forEach { c ->
                OpcaoPilula(c, selecionada = c == categoria, onClick = { categoria = c }, altura = 36.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(visiveis, key = { it.id }) { s ->
                val sel = s.id in marcadosIds
                Surface(
                    onClick = { marcadosIds = if (sel) marcadosIds - s.id else marcadosIds + s.id },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, if (sel) SkalaColors.Rose else SkalaColors.CardBorder),
                    modifier = Modifier.fillMaxWidth().semantics { role = Role.Checkbox; selected = sel },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(s.nome, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                            val preco = when {
                                s.dependeDoCabelo -> "aprox. conforme o cabelo"
                                else -> precoFixo(s)?.let { "aprox. ${it.texto}" } ?: "valor a combinar"
                            }
                            Text("${s.duracaoTexto} · $preco", fontSize = 13.sp, color = SkalaColors.Muted)
                        }
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(if (sel) SkalaColors.Rose else Color.White, RoundedCornerShape(7.dp))
                                .border(1.5.dp, if (sel) SkalaColors.Rose else SkalaColors.CardBorder, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (sel) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val tem = marcados.isNotEmpty()
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (tem) SkalaColors.Wine else SkalaColors.Disabled, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val corTexto = if (tem) SkalaColors.OnWineMuted else SkalaColors.Muted
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (tem) "${marcados.size} ${if (marcados.size == 1) "SERVIÇO" else "SERVIÇOS"} · ${textoDuracao(marcados.duracaoTotalMin).uppercase()}"
                    else "NENHUM SERVIÇO ESCOLHIDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = corTexto,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        !tem -> "—"
                        else -> totalAproximado(marcados)?.let { "aprox. ${it.texto}" } ?: "a calcular"
                    },
                    style = TituloSecao.copy(fontSize = 20.sp),
                    color = if (tem) Color.White else SkalaColors.Muted,
                )
            }
            Text(
                "Valor aproximado. O valor final é sempre confirmado no salão, após o procedimento.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = corTexto,
            )
        }
        Spacer(Modifier.height(10.dp))
        PrimaryButton(
            text = if (tem) "Continuar" else "Escolha um serviço",
            onClick = { onContinuar(marcados) },
            enabled = tem,
        )
    }
}
