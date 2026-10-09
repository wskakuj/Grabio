package com.wskakuj.grabio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Logotyp „Grabio” — pogrubiony, rozstrzelony, z zielonym „io”. */
@Composable
fun GrabioWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    accent: Color = MaterialTheme.colorScheme.primary,
    base: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Black, letterSpacing = 1.sp)) {
                append("Grab")
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = accent
                )
            ) {
                append("io")
            }
        },
        modifier = modifier,
        fontSize = fontSize,
        color = base
    )
}

/** Stopka na dole Ustawień. */
@Composable
fun GrabioCredit(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GrabioWordmark(fontSize = 28.sp)
        Text(
            text = "MADE WITH DEMENTIA",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
