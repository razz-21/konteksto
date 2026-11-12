package com.example.konteksto.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Stacks [header] above [body] and gives [body] the height left on screen, but never less than
 * [minBodyHeight]. On a short screen the whole column scrolls instead of squeezing [body].
 */
@Composable
fun FillRemainingHeightColumn(
    minBodyHeight: Dp,
    header: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    body: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val viewportHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
        Layout(
            contents = listOf(
                { Column(modifier = Modifier.fillMaxWidth(), content = header) },
                { Box(modifier = Modifier.fillMaxSize(), content = body) },
            ),
            modifier = Modifier.verticalScroll(scrollState),
        ) { (headerMeasurables, bodyMeasurables), layoutConstraints ->
            val width = layoutConstraints.maxWidth
            val headerPlaceable = headerMeasurables.single().measure(Constraints(maxWidth = width))
            val bodyHeight = maxOf(viewportHeight - headerPlaceable.height, minBodyHeight.roundToPx())
            val bodyPlaceable = bodyMeasurables.single().measure(Constraints.fixed(width, bodyHeight))

            layout(width, headerPlaceable.height + bodyHeight) {
                headerPlaceable.place(0, 0)
                bodyPlaceable.place(0, headerPlaceable.height)
            }
        }
    }
}
