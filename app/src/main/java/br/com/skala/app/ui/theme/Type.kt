package br.com.skala.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.skala.app.R

// Jost nos textos e Cormorant Garamond nos títulos e no nome "SKALA" (Google Fonts, licença OFL).
val BodyFontFamily = FontFamily(
    Font(R.font.jost_regular, FontWeight.Normal),
    Font(R.font.jost_medium, FontWeight.Medium),
    Font(R.font.jost_semibold, FontWeight.SemiBold),
)
val BrandFontFamily = FontFamily(
    Font(R.font.cormorant_medium, FontWeight.Medium),
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_medium_italic, FontWeight.Medium, FontStyle.Italic),
)

val SkalaTypography = Typography(
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = BodyFontFamily, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
    ),
)

/** Títulos das telas: serifado em itálico ("Que bom te ver", "Escolha o horário"). */
val TituloTela = TextStyle(
    fontFamily = BrandFontFamily,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Medium,
    fontSize = 30.sp,
    lineHeight = 36.sp,
)

/** Títulos de seção e nomes de serviço dentro de cartões. */
val TituloSecao = TextStyle(
    fontFamily = BrandFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
)

/** O nome "SKALA" ao lado ou abaixo da ilustração. */
val Marca = TextStyle(
    fontFamily = BrandFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    letterSpacing = 10.sp,
)
