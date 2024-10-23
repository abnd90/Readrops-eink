package com.readrops.app.util.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp


val BUTTON_SIZE = 40.dp
val BORDER_COLOR = Color.Black
val BORDER_WIDTH = 2.dp
val CORNER_RADIUS = 4.dp

@Composable
fun BorderedIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource()},
    content: @Composable () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            isPressed = interaction is PressInteraction.Press
        }
    }

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .minimumInteractiveComponentSize()
            .size(BUTTON_SIZE)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            )
            .border(
                width = BORDER_WIDTH,
                color = if (isPressed) BORDER_COLOR else Color.Transparent,
                shape = RoundedCornerShape(CORNER_RADIUS)
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentAlpha = if (enabled) 1.0f else 0.38f
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(
                alpha = contentAlpha
            )
        ) {
            content()
        }
    }
}

@Composable
fun BorderedToggleIconButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource()},
    drawBottomTriangle: Boolean = false,
    content: @Composable () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            isPressed = interaction is PressInteraction.Press
        }
    }
    val triangleSize = 10.dp

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .minimumInteractiveComponentSize()
            .size(BUTTON_SIZE)
            .then(
                if (checked) {
                    Modifier.border(
                        width = BORDER_WIDTH,
                        color = BORDER_COLOR,
                        shape = RoundedCornerShape(CORNER_RADIUS)
                    )
                } else {
                    Modifier
                }
            )
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null
            )
            .border(
                width = BORDER_WIDTH,
                color = if (isPressed) BORDER_COLOR else Color.Transparent,
                shape = RoundedCornerShape(CORNER_RADIUS)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()

        if (drawBottomTriangle) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(triangleSize)
            ) {
                val path = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    color = BORDER_COLOR
                )
            }
        }
    }
}

@Composable
fun BorderedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource()},
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            isPressed = interaction is PressInteraction.Press
        }
    }

    Surface(
        modifier = modifier
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            )
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            )
            .border(
                width = BORDER_WIDTH,
                color = if (isPressed) BORDER_COLOR else Color.Transparent,
                shape = RoundedCornerShape(CORNER_RADIUS)
            ),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.Black),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.Black) {
            ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                Box(
                    modifier = Modifier.padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    content()
                }
            }
        }
    }
}