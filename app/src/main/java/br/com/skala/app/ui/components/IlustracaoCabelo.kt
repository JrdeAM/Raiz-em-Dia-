package br.com.skala.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.skala.app.data.Comprimento
import br.com.skala.app.ui.theme.SkalaColors

private val Pele = Color(0xFFF3D7CA)
private val PeleSombra = Color(0xFFE8C3B3)
private val CabeloNeutro = Color(0xFF7A5560)
private val CabeloNeutroBrilho = Color(0xFF9A7480)
private val CabeloSelecionadoBrilho = Color(0xFFC07A8C)
private val RoupaNeutra = Color(0xFFEBDAD6)

/**
 * Busto feminino com o cabelo caindo até o comprimento: queixo (curto), ombro (médio),
 * peito (longo) ou cintura (extra longo). Selecionado, o cabelo fica rosé e a roupa clara.
 */
@Composable
fun IlustracaoComprimento(
    comprimento: Comprimento,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    largura: Dp = 34.dp,
    altura: Dp = 46.dp,
) {
    val cabelo = if (selecionado) SkalaColors.Rose else CabeloNeutro
    val brilho = if (selecionado) CabeloSelecionadoBrilho else CabeloNeutroBrilho
    val roupa = if (selecionado) Color.White else RoupaNeutra
    Canvas(modifier.size(largura, altura)) {
        desenharBusto(comprimento, cabelo, brilho, roupa)
    }
}

private fun DrawScope.desenharBusto(c: Comprimento, cabelo: Color, brilho: Color, roupa: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2
    val r = h * 0.145f          // raio do rosto
    val cy = h * 0.25f          // centro da cabeça
    val topoOmbro = cy + r * 1.75f
    val fimCabelo = when (c) {
        Comprimento.CURTO -> cy + r * 1.25f        // queixo
        Comprimento.MEDIO -> topoOmbro + r * 0.35f // ombro
        Comprimento.LONGO -> h * 0.78f             // peito
        Comprimento.EXTRA_LONGO -> h * 0.985f      // cintura
    }

    // 1) ombros e colo
    val ombros = Path().apply {
        moveTo(w * 0.03f, h)
        cubicTo(w * 0.03f, topoOmbro + (h - topoOmbro) * 0.2f, w * 0.2f, topoOmbro, cx, topoOmbro)
        cubicTo(w * 0.8f, topoOmbro, w * 0.97f, topoOmbro + (h - topoOmbro) * 0.2f, w * 0.97f, h)
        close()
    }
    drawPath(ombros, roupa)
    // decote em V
    val decote = Path().apply {
        moveTo(cx - r * 0.55f, topoOmbro - 1f)
        lineTo(cx, topoOmbro + r * 0.75f)
        lineTo(cx + r * 0.55f, topoOmbro - 1f)
        close()
    }
    drawPath(decote, Pele)

    // 2) cabelo de trás: cai pelos lados até o comprimento escolhido
    val externo = r * 1.32f
    val pontaExterna = r * (if (c == Comprimento.CURTO) 1.38f else 1.55f)
    val interno = r * 0.6f
    val atras = Path().apply {
        moveTo(cx - externo, cy)
        arcTo(Rect(cx - externo, cy - r * 1.42f, cx + externo, cy + r * 1.22f), 180f, 180f, false)
        quadraticBezierTo(cx + externo * 1.02f, (cy + fimCabelo) / 2, cx + pontaExterna, fimCabelo - r * 0.35f)
        quadraticBezierTo(cx + pontaExterna * 0.98f, fimCabelo, cx + r * 1.05f, fimCabelo)
        quadraticBezierTo(cx + interno * 1.1f, fimCabelo, cx + interno, fimCabelo - r * 0.45f)
        lineTo(cx + interno, cy + r * 0.6f)
        lineTo(cx - interno, cy + r * 0.6f)
        lineTo(cx - interno, fimCabelo - r * 0.45f)
        quadraticBezierTo(cx - interno * 1.1f, fimCabelo, cx - r * 1.05f, fimCabelo)
        quadraticBezierTo(cx - pontaExterna * 0.98f, fimCabelo, cx - pontaExterna, fimCabelo - r * 0.35f)
        quadraticBezierTo(cx - externo * 1.02f, (cy + fimCabelo) / 2, cx - externo, cy)
        close()
    }
    drawPath(atras, cabelo)

    // 3) pescoço e rosto
    drawRoundRect(
        PeleSombra,
        topLeft = Offset(cx - r * 0.34f, cy + r * 0.7f),
        size = Size(r * 0.68f, topoOmbro - (cy + r * 0.7f) + r * 0.2f),
        cornerRadius = CornerRadius(r * 0.2f),
    )
    drawOval(Pele, topLeft = Offset(cx - r * 0.92f, cy - r * 0.95f), size = Size(r * 1.84f, r * 2.12f))

    // 4) franja de lado por cima da testa
    val franja = Path().apply {
        moveTo(cx - r * 1.0f, cy + r * 0.05f)
        arcTo(Rect(cx - r * 1.02f, cy - r * 1.12f, cx + r * 1.02f, cy + r * 0.95f), 180f, 180f, false)
        lineTo(cx + r * 1.0f, cy + r * 0.15f)
        quadraticBezierTo(cx + r * 0.55f, cy - r * 0.55f, cx - r * 0.15f, cy - r * 0.3f)
        quadraticBezierTo(cx - r * 0.65f, cy - r * 0.1f, cx - r * 1.0f, cy + r * 0.05f)
        close()
    }
    drawPath(franja, cabelo)
    // brilho no alto da cabeça
    val reflexo = Path().apply {
        moveTo(cx - r * 0.75f, cy - r * 0.6f)
        quadraticBezierTo(cx - r * 0.2f, cy - r * 1.15f, cx + r * 0.45f, cy - r * 0.95f)
        quadraticBezierTo(cx - r * 0.15f, cy - r * 0.9f, cx - r * 0.75f, cy - r * 0.6f)
        close()
    }
    drawPath(reflexo, brilho)

    // 5) bochechas rosadas
    val bochecha = SkalaColors.RoseLight.copy(alpha = 0.55f)
    drawCircle(bochecha, radius = r * 0.17f, center = Offset(cx - r * 0.48f, cy + r * 0.45f))
    drawCircle(bochecha, radius = r * 0.17f, center = Offset(cx + r * 0.48f, cy + r * 0.45f))
}

@Preview(showBackground = true, widthDp = 220, heightDp = 70, name = "Comprimentos")
@Composable
private fun IlustracaoComprimentoPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Comprimento.entries.forEachIndexed { i, c -> IlustracaoComprimento(c, selecionado = i == 2) }
    }
}
