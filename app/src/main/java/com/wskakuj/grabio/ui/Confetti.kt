package com.wskakuj.grabio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * Delikatne confetti — odtwarza się raz, gdy wszystko jest spakowane.
 */
@Composable
fun ConfettiOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2600, easing = LinearEasing)
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val colors = listOf(
        Color(0xFFA3FF12),
        Color(0xFF7ED321),
        Color.White,
        Color(0xFF64B5F6),
        Color(0xFFFFB74D)
    )

    Canvas(modifier = modifier) {
        val p = progress.value
        if (p <= 0f) return@Canvas
        val n = 30
        val w = size.width
        val h = size.height
        for (i in 0 until n) {
            val seed = (i * 0.6180339887f) % 1f
            val x = ((seed * 7.13f) % 1f) * w
            val delay = ((seed * 3.71f) % 1f) * 0.25f
            val local = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val y = local * (h + 80f) - 40f
            val alpha = (1f - local * 0.55f).coerceIn(0f, 1f)
            drawRect(
                color = colors[i % colors.size].copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(10f, 18f)
            )
        }
    }
}
