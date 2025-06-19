package com.example.tokofafa.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DarkCard = Color(0xFF2A2A2A)
val AccentGreen = Color(0xFF00C853)
val AccentText = Color.White
val BorderGray = Color(0xFF424242)
val LightBackground = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val DarkText = Color(0xFF000000)
val PrimaryBlue = Color(0xFF3b5998)

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun KasirkuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = lightColors(
            primary = PrimaryBlue,
            background = LightBackground,
            surface = LightCard,
            onPrimary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText
        ),
        typography = Typography(),
        shapes = Shapes,
        content = content
    )
}