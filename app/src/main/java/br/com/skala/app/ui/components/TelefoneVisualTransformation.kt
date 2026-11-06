package br.com.skala.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Mostra só dígitos como celular brasileiro: (11) 91234-5678 ou (11) 1234-5678.
 * O valor guardado continua sendo apenas os dígitos (até 11).
 */
class TelefoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        val saida = StringBuilder()
        val origemParaSaida = IntArray(digitos.length + 1)

        for (i in digitos.indices) {
            when (i) {
                0 -> saida.append('(')
                2 -> saida.append(") ")
                6 -> if (digitos.length <= 10) saida.append('-')
                7 -> if (digitos.length == 11) saida.append('-')
            }
            origemParaSaida[i] = saida.length
            saida.append(digitos[i])
        }
        origemParaSaida[digitos.length] = saida.length

        val saidaParaOrigem = IntArray(saida.length + 1) { t ->
            (0 until digitos.length).count { origemParaSaida[it] < t }
        }

        val mapeamento = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = origemParaSaida[offset]
            override fun transformedToOriginal(offset: Int): Int = saidaParaOrigem[offset]
        }
        return TransformedText(AnnotatedString(saida.toString()), mapeamento)
    }
}
