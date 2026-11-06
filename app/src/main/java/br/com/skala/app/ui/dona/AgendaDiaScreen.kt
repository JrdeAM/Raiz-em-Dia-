package br.com.skala.app.ui.dona

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.DiaFuncionamento
import br.com.skala.app.data.StatusAgendamento
import br.com.skala.app.data.StatusDia
import br.com.skala.app.ui.components.SkalaLogo
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.components.textoLongo
import br.com.skala.app.ui.theme.SkalaColors
import java.time.LocalTime

private enum class TipoItem { CLIENTE, LIVRE, PAUSA }

private data class ItemAgenda(val hora: LocalTime, val titulo: String, val detalhe: String, val tipo: TipoItem)

/** Agenda do dia da dona: clientes, horários livres, pausas entre faixas e bloqueios. */
@Composable
fun AgendaDiaScreen(
    dia: DiaFuncionamento,
    agendamentos: List<Agendamento>,
    onAjustarDia: () -> Unit,
) {
    val doDia = agendamentos
        .filter { it.data == dia.data && it.status == StatusAgendamento.CONFIRMADO }
        .sortedBy { it.inicio }
    val itens = montarItens(dia, doDia)
    val livres = itens.count { it.tipo == TipoItem.LIVRE }

    LazyColumn(
        Modifier.fillMaxSize().background(SkalaColors.Ivory),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("PAINEL DO SALÃO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = SkalaColors.Muted)
                    TituloDaTela("Agenda de hoje")
                }
                SkalaLogo(altura = 52.dp, descricao = null)
            }
            Text(dia.data.textoLongo(), fontSize = 14.sp, color = SkalaColors.Muted)
            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth().background(SkalaColors.Wine, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(if (dia.status == StatusDia.ABERTO) SkalaColors.RoseLight else SkalaColors.Placeholder, CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (dia.status) { StatusDia.ABERTO -> "Aberto hoje"; StatusDia.FECHADO -> "Fechado hoje"; StatusDia.A_DEFINIR -> "Horário de hoje a definir" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    if (dia.status == StatusDia.ABERTO) {
                        Text(dia.faixas.joinToString(" · ") { "${it.inicio.texto()} às ${it.fim.texto()}" }, fontSize = 13.sp, color = SkalaColors.OnWineMuted)
                    }
                }
                Surface(onClick = onAjustarDia, shape = RoundedCornerShape(50), color = SkalaColors.Ivory) {
                    Text("Ajustar", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Contador("${doDia.size}", "agendadas", Modifier.weight(1f))
                Contador("$livres", if (livres == 1) "horário livre" else "horários livres", Modifier.weight(1f))
                Contador("${dia.bloqueios.size}", if (dia.bloqueios.size == 1) "bloqueio" else "bloqueios", Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
        }
        items(itens) { item -> LinhaAgenda(item) }
    }
}

@Composable
private fun Contador(numero: String, rotulo: String, modifier: Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, SkalaColors.CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(numero, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Rose)
        Text(rotulo, fontSize = 12.sp, color = SkalaColors.Muted)
    }
}

@Composable
private fun LinhaAgenda(item: ItemAgenda) {
    val (fundo, borda, faixa, cor) = when (item.tipo) {
        TipoItem.CLIENTE -> listOf(Color.White, SkalaColors.CardBorder, SkalaColors.Rose, SkalaColors.Wine)
        TipoItem.LIVRE -> listOf(Color.Transparent, Color(0xFFD9C2C8), SkalaColors.RoseLight, SkalaColors.RoseLink)
        TipoItem.PAUSA -> listOf(SkalaColors.Disabled, SkalaColors.Disabled, SkalaColors.Dashed, SkalaColors.Label)
    }
    Row(Modifier.height(IntrinsicSize.Min)) {
        Text(item.hora.texto(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Label, modifier = Modifier.width(52.dp).padding(top = 12.dp))
        Row(
            Modifier
                .weight(1f)
                .background(fundo, RoundedCornerShape(16.dp))
                .border(1.dp, borda, RoundedCornerShape(16.dp)),
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(faixa, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(item.titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = cor)
                Text(item.detalhe, fontSize = 12.sp, color = SkalaColors.Muted)
            }
        }
    }
}

/** Junta clientes, bloqueios, pausas entre faixas e os intervalos livres que sobram em cada faixa. */
private fun montarItens(dia: DiaFuncionamento, agendamentos: List<Agendamento>): List<ItemAgenda> {
    if (dia.status != StatusDia.ABERTO) return emptyList()
    val itens = mutableListOf<ItemAgenda>()
    agendamentos.forEach {
        itens += ItemAgenda(it.inicio, it.clienteNome, "${it.titulo} · ${it.duracaoTexto}", TipoItem.CLIENTE)
    }
    dia.bloqueios.forEach {
        itens += ItemAgenda(it.inicio, "Bloqueado", "${it.motivo.ifBlank { "Horário fechado" }} · até ${it.fim.texto()}", TipoItem.PAUSA)
    }
    val faixas = dia.faixas.sortedBy { it.inicio }
    faixas.zipWithNext().forEach { (a, b) ->
        if (a.fim.isBefore(b.inicio)) itens += ItemAgenda(a.fim, "Fechado", "Pausa até ${b.inicio.texto()}", TipoItem.PAUSA)
    }
    val ocupados = (agendamentos.map { it.inicio to it.fim } + dia.bloqueios.map { it.inicio to it.fim }).sortedBy { it.first }
    faixas.forEach { faixa ->
        var cursor = faixa.inicio
        ocupados.filter { (ini, fim) -> ini.isBefore(faixa.fim) && fim.isAfter(faixa.inicio) }.forEach { (ini, fim) ->
            if (ini.isAfter(cursor)) itens += ItemAgenda(cursor, "Horário livre", "${cursor.texto()} às ${ini.texto()}", TipoItem.LIVRE)
            if (fim.isAfter(cursor)) cursor = fim
        }
        if (cursor.isBefore(faixa.fim)) itens += ItemAgenda(cursor, "Horário livre", "${cursor.texto()} às ${faixa.fim.texto()}", TipoItem.LIVRE)
    }
    return itens.sortedBy { it.hora }
}
