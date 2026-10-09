package com.wskakuj.grabio.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Krótka lista z przeciąganiem: przytrzymaj wiersz i przeciągnij w górę/dół,
 * a po puszczeniu pozycja przeskoczy na nowe miejsce.
 */
@Composable
fun ReorderableColumn(
    count: Int,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 56.dp,
    content: @Composable (index: Int, dragHandle: Modifier) -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }

    Column(modifier = modifier) {
        for (i in 0 until count) {
            val isDragging = draggingIndex == i
            val handle = Modifier
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    translationY = if (isDragging) dragOffset else 0f
                    alpha = if (isDragging) 0.92f else 1f
                }
                .pointerInput(i, count) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = i
                            dragOffset = 0f
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            dragOffset += delta.y
                        },
                        onDragEnd = {
                            val shift = (dragOffset / rowPx).roundToInt()
                            val to = (i + shift).coerceIn(0, count - 1)
                            draggingIndex = null
                            dragOffset = 0f
                            if (to != i) onMove(i, to)
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = 0f
                        }
                    )
                }
            content(i, handle)
        }
    }
}
