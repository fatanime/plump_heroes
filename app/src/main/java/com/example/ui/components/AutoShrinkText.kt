package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun AutoShrinkText(
    text: String,
    color: Color,
    initialFontSizeSp: Float,
    minFontSizeSp: Float = 8f,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier
) {
    var currentFontSizeSp by remember(text, initialFontSizeSp) { mutableFloatStateOf(initialFontSizeSp) }
    Text(
        text = text,
        color = color,
        fontSize = currentFontSizeSp.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && currentFontSizeSp > minFontSizeSp) {
                currentFontSizeSp = (currentFontSizeSp * 0.9f).coerceAtLeast(minFontSizeSp)
            }
        },
        modifier = modifier
    )
}
