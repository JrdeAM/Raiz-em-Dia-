package br.com.skala.app.ui.dona

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.skala.app.data.Agendamento
import br.com.skala.app.data.Cliente
import br.com.skala.app.data.FakeSalaoRepository
import br.com.skala.app.data.StatusAgendamento
import br.com.skala.app.ui.components.LabeledTextField
import br.com.skala.app.ui.components.PrimaryButton
import br.com.skala.app.ui.components.TituloDaTela
import br.com.skala.app.ui.components.diaSemanaCurto
import br.com.skala.app.ui.components.texto
import br.com.skala.app.ui.theme.SkalaColors
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatoData = DateTimeFormatter.ofPattern("dd/MM")

/** "11912345678" → "(11) 91234-5678" */
fun formatarCelular(digitos: String): String = when (digitos.length) {
    11 -> "(${digitos.take(2)}) ${digitos.substring(2, 7)}-${digitos.substring(7)}"
    10 -> "(${digitos.take(2)}) ${digitos.substring(2, 6)}-${digitos.substring(6)}"
    else -> digitos
}

private fun String.semAcento(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/**
 * Clientes do salão, para a dona. Ela busca pelo nome ou celular, cadastra cliente nova
 * e toca numa cliente para reservar um horário no nome dela.
 */
@Composable
fun ClientesScreen(
    clientes: List<Cliente>,
    agendamentos: List<Agendamento>,
    onNovaCliente: () -> Unit,
    onReservar: (Cliente) -> Unit,
) {
    var busca by rememberSaveable { mutableStateOf("") }
    val termo = busca.trim().semAcento()
    val termoDigitos = busca.filter(Char::isDigit)
    val visiveis = clientes
        .filter { c ->
            termo.isEmpty() || c.nome.semAcento().contains(termo) ||
                (termoDigitos.isNotEmpty() && c.celular.contains(termoDigitos))
        }
        .sortedBy { it.nome.semAcento() }
    val agora = LocalDateTime.now()

    Column(Modifier.fillMaxSize().background(SkalaColors.Ivory).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text("PAINEL DO SALÃO", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = SkalaColors.Muted)
        TituloDaTela("Clientes")
        Text("Toque numa cliente para reservar um horário para ela.", fontSize = 13.sp, color = SkalaColors.Muted)
        Spacer(Modifier.height(12.dp))
        LabeledTextField(
            label = "Buscar",
            value = busca,
            onValueChange = { busca = it },
            placeholder = "Nome ou celular",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SkalaColors.Muted) },
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            if (visiveis.isEmpty()) {
                item {
                    Text(
                        if (clientes.isEmpty()) "Nenhuma cliente cadastrada ainda." else "Nenhuma cliente encontrada.",
                        fontSize = 14.sp,
                        color = SkalaColors.Muted,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(visiveis, key = { it.celular }) { c ->
                val proximo = agendamentos
                    .filter { it.eDe(c) && it.status == StatusAgendamento.CONFIRMADO }
                    .filter { LocalDateTime.of(it.data, it.inicio).isAfter(agora) }
                    .minByOrNull { LocalDateTime.of(it.data, it.inicio) }
                LinhaCliente(c, proximo, onClick = { onReservar(c) })
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = "Cadastrar cliente", onClick = onNovaCliente)
    }
}

@Composable
private fun LinhaCliente(cliente: Cliente, proximo: Agendamento?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SkalaColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(SkalaColors.Blush, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(cliente.nome.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SkalaColors.Rose)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cliente.nome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SkalaColors.Wine)
                Text(formatarCelular(cliente.celular), fontSize = 12.sp, color = SkalaColors.Muted)
                if (proximo != null) {
                    Text(
                        "Próximo: ${proximo.data.diaSemanaCurto()} ${proximo.data.format(formatoData)} às ${proximo.inicio.texto()}",
                        fontSize = 12.sp,
                        color = SkalaColors.RoseLink,
                    )
                }
            }
            Text("Reservar", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SkalaColors.RoseLink)
        }
    }
}

// ---------- previews (dados de teste do FakeSalaoRepository) ----------
@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Clientes")
@Composable
private fun ClientesScreenPreview() {
    br.com.skala.app.ui.theme.SkalaTheme {
        ClientesScreen(
            clientes = FakeSalaoRepository.clientes.value,
            agendamentos = FakeSalaoRepository.agendamentos.value,
            onNovaCliente = {},
            onReservar = {},
        )
    }
}
