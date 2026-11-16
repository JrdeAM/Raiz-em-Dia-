package br.com.skala.app.ui.cliente

import androidx.compose.ui.tooling.preview.Preview
import br.com.skala.app.ui.theme.SkalaTheme
import br.com.skala.app.data.FakeSalaoRepository
import br.com.skala.app.data.Comprimento
import br.com.skala.app.data.TipoCabelo
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
import br.com.skala.app.data.Servico
import br.com.skala.app.data.duracaoPara
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
    observacao: String,
    passo: Int,
    totalPassos: Int,
    remarcando: Boolean,
    onObservacao: (String) -> Unit,
    onEditarCabelo: () -> Unit,
    onAlterarHorario: () -> Unit,
    onVoltar: () -> Unit,
    onConfirmar: () -> Unit,
    /** Nome da cliente quando é a dona reservando. */
    paraCliente: String? = null,
) {
    val minutos = servicos.duracaoPara(cabelo)
    val fim = hora.plusMinutes(minutos.toLong())
    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        CabecalhoPasso(passo = passo, total = totalPassos, onVoltar = onVoltar, paraCliente = paraCliente)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TituloDaTela(
                when {
                    paraCliente != null -> "Horário de $paraCliente"
                    remarcando -> "Confira o novo horário"
                    else -> "Confira seu horário"
                },
            )

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
                            Text(if (paraCliente != null) "Cabelo" else "Seu cabelo", fontSize = 14.sp, color = SkalaColors.Muted, modifier = Modifier.weight(1f))
                            TextButton(onClick = onEditarCabelo) {
                                Text(cabelo.resumo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Outlined.Edit, contentDescription = "Editar dados do cabelo", tint = SkalaColors.RoseLink, modifier = Modifier.size(14.dp))
                            }
                        }
                        HorizontalDivider(color = SkalaColors.Divider)
                    }
                    Text(
                        "Serviços",
                        fontSize = 14.sp,
                        color = SkalaColors.Muted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Column(Modifier.padding(top = 6.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        servicos.forEach { s ->
                            Text(s.nome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                        }
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
                        "O valor é informado no salão, após a avaliação e a finalização do serviço. " +
                            "Cancelamento ou remarcação sem custo até [X] horas antes.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = SkalaColors.Label,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = when {
                paraCliente != null -> "Reservar horário"
                remarcando -> "Confirmar novo horário"
                else -> "Confirmar agendamento"
            },
            onClick = onConfirmar,
        )
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

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Confirmar")
@Composable
private fun ConfirmarScreenPreview() {
    SkalaTheme {
        ConfirmarScreen(
            servicos = FakeSalaoRepository.servicos.filter { it.id == "escova" || it.id == "hidratacao" },
            data = LocalDate.now().plusDays(1),
            hora = LocalTime.of(10, 0),
            cabelo = DadosCabelo(Comprimento.LONGO, TipoCabelo.ONDULADO),
            observacao = "",
            passo = 4,
            totalPassos = 4,
            remarcando = false,
            onObservacao = {},
            onEditarCabelo = {},
            onAlterarHorario = {},
            onVoltar = {},
            onConfirmar = {},
        )
    }
}
