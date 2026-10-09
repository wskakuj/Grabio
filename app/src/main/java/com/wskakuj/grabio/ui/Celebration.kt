package com.wskakuj.grabio.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Celebracja, gdy wszystko jest spakowane: kolorowe confetti z obrotami,
 * rozbłysk i plakietka „Wszystko spakowane!”. Odtwarza się raz —
 * tylko w momencie przejścia z „niepełnej” listy na pełną.
 */
@Composable
fun CelebrationOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var previous by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(visible) {
        val wasVisible = previous
        previous = visible
        if (wasVisible == false && visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3200, easing = LinearEasing)
            )
        } else if (!visible) {
            // cofnięcie odhaczenia natychmiast sprząta confetti i plakietkę
            progress.snapTo(0f)
        }
    }

    val p = progress.value
    if (p <= 0f) return

    val colors = listOf(
        Color(0xFFA3FF12),
        Color(0xFF7ED321),
        Color(0xFFFFFFFF),
        Color(0xFF64B5F6),
        Color(0xFFFFB74D),
        Color(0xFFFF6FB5)
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val n = 70
            for (i in 0 until n) {
                val s1 = ((i * 0.6180339887f) % 1f)
                val s2 = ((i * 0.7548776662f) % 1f)
                val s3 = ((i * 0.4142135624f) % 1f)

                val delay = s3 * 0.30f
                val local = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (local <= 0f) continue

                val sway = sin((local * 6.28f) + i) * (18f + s2 * 26f)
                val x = s1 * w + sway
                val y = -60f + local * (h + 140f)
                val fade = (1f - local * local * 0.85f).coerceIn(0f, 1f)
                val alpha = (fade * 1.0f).coerceIn(0f, 1f)
                val size = 7f + s2 * 11f
                val spin = (local * 900f) * (if (s2 > 0.5f) 1f else -1f) + i * 23f
                val color = colors[i % colors.size].copy(alpha = alpha)

                rotate(degrees = spin, pivot = Offset(x, y)) {
                    if (i % 3 == 0) {
                        drawCircle(color = color, radius = size * 0.45f, center = Offset(x, y))
                    } else {
                        drawRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(size, size * 1.7f)
                        )
                    }
                }
            }
        }

        // Plakietka: pojawia się szybko, chwilę trzyma, potem znika.
        val bannerAlpha = when {
            p < 0.10f -> p / 0.10f
            p > 0.72f -> ((1f - p) / 0.28f).coerceIn(0f, 1f)
            else -> 1f
        }
        val bannerScale = (p / 0.10f).coerceIn(0f, 1f)
        if (bannerAlpha > 0f) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.graphicsLayer {
                        alpha = bannerAlpha
                        scaleX = bannerScale
                        scaleY = bannerScale
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🎉", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Wszystko spakowane!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
