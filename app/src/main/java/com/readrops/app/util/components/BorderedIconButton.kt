package com.readrops.app.util.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


val BUTTON_SIZE = 40.dp

@Composable
fun BorderedIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
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
                indication = BorderIndication()
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BorderedToggleIconButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    drawBottomTriangle: Boolean = false,
    content: @Composable () -> Unit,
) {
    val borderColor = Color.Black
    val borderWidth = 2.dp
    val cornerRadius = 4.dp
    val triangleSize = 10.dp

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .minimumInteractiveComponentSize()
            .size(BUTTON_SIZE)
            .then(
                if (checked) {
                    Modifier.border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(cornerRadius)
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
                indication = BorderIndication()
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
                    color = borderColor
                )
            }
        }
    }
}

private class BorderNode(private val interactionSource: InteractionSource) :
    Modifier.Node(), DrawModifierNode {

    private val borderColor = Color.Black
    private val borderWidth = 2.dp
    private val cornerRadius = 4.dp

    private var isPressed by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {isPressed = true}
                    is PressInteraction.Release, is PressInteraction.Cancel -> {isPressed = false}
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        if (isPressed) {
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = borderWidth.toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
}

object BorderIndication {
    operator fun invoke(): IndicationNodeFactory = object : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return BorderNode(interactionSource)
        }

        override fun equals(other: Any?): Boolean = other === this
        override fun hashCode() = System.identityHashCode(this)
    }
}