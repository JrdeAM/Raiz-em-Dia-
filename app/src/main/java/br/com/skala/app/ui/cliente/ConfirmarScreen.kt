package br.com.skala.app.ui.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.DadosCabelo
import br.com.skala.app.data.FaixaPreco
import br.com.skala.app.data.Servico
import br.com.skala.app.data.duracaoTotalMin
import br.com.skala.app.data.temServicoDeCabelo
import br.com.skala.app.data.textoDuracao
import br.com.skala.app.data.titulo
import br.com.skala.app.ui.components.CabecalhoPasso
import br.com.skala.app.ui.components.Cartao
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.SkalaLogo
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import br.com.skala.app.ui.theme.TituloSecao
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ConfirmarScreen(
    servicos: List<Servico>,
    data: LocalDate,
    hora: LocalTime,
    cabelo: DadosCabelo,
    valorDe: (Servico) -> FaixaPreco?,
    valorTotal: FaixaPreco?,
    observacao: String,
    passo: Int,
    totalPassos: Int,
    remarcando: Boolean,
    onObservacao: (String) -> Unit,
    onEditarCabelo: () -> Unit,
    onAlterarHorario: () -> Unit,
    onVoltar: () -> Unit,
    onConfirmar: () -> Unit,
) {
    val minutos = servicos.duracaoTotalMin
    val fim = hora.plusMinutes(minutos.toLong())
    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        CabecalhoPasso(passo = passo, total = totalPassos, onVoltar = onVoltar)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TituloDaTela(if (remarcando) "Confira o novo horário" else "Confira seu horário")

            Cartao(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(SkalaColors.Blush).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkalaLogo(altura = 52.dp, descricao = null)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(servicos.titulo, style = TituloSecao.copy(fontSize = 22.sp), color = SkalaColors.Wine)
                        Text("Skala Salão de Beleza", fontSize = 13.sp, color = SkalaColors.Muted)
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    LinhaResumo("Data", data.textoLongo())
                    LinhaResumo("Horário", "${hora.texto()} às ${fim.texto()} (${textoDuracao(minutos)})")
                    if (servicos.temServicoDeCabelo) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Seu cabelo", fontSize = 14.sp, color = SkalaColors.Muted, modifier = Modifier.weight(1f))
                            TextButton(onClick = onEditarCabelo) {
                                Text(cabelo.resumo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Outlined.Edit, contentDescription = "Editar dados do cabelo", tint = SkalaColors.RoseLink, modifier = Modifier.size(14.dp))
                            }
                        }
                        HorizontalDivider(color = SkalaColors.Divider)
                    }
                    // Valor aproximado de cada serviço e a soma
                    Column(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        servicos.forEach { s ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(s.nome, fontSize = 14.sp, color = SkalaColors.Muted, modifier = Modifier.weight(1f))
                                Text(valorDe(s)?.let { "aprox. ${it.texto}" } ?: "a combinar", fontSize = 14.sp, color = SkalaColors.Wine)
                            }
                        }
                    }
                    HorizontalDivider(color = SkalaColors.Border, modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total aproximado", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Wine, modifier = Modifier.weight(1f))
                        Text(valorTotal?.texto ?: "a combinar", style = TituloSecao.copy(fontSize = 20.sp), color = SkalaColors.Wine)
                    }
                }
            }

            LabeledTextField(
                label = "Observação (opcional)",
                value = observacao,
                onValueChange = onObservacao,
                placeholder = "Ex.: quero levar uma foto de referência…",
                singleLine = false,
                minLines = 3,
            )

            Cartao(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = SkalaColors.Rose, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Os valores são aproximados. O valor final é sempre confirmado no salão, após o procedimento. " +
                            "Cancelamento ou remarcação sem custo até [X] horas antes.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = SkalaColors.Label,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = if (remarcando) "Confirmar novo horário" else "Confirmar agendamento", onClick = onConfirmar)
        TextButton(onClick = onAlterarHorario, modifier = Modifier.fillMaxWidth()) {
            Text("Alterar horário", color = SkalaColors.RoseLink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, fontSize = 14.sp, color = SkalaColors.Muted, modifier = Modifier.weight(1f))
        Text(valor, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
    }
    HorizontalDivider(color = SkalaColors.Divider)
}
