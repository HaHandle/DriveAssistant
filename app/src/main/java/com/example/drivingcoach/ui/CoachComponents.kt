package com.example.drivingcoach.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One-shot, input-driven motion only. Rendering uses target values immediately when restricted. */
val LocalCoachMotion = staticCompositionLocalOf { false }

enum class CoachSymbol { COACH, HISTORY, SETTINGS, ARROW, CHECK, LEAF, WAVE, SHIELD, BACK, PLAY, STOP }

@Composable
fun CoachIcon(symbol: CoachSymbol, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier.size(28.dp).clearAndSetSemantics {}) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(1.65f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, point(x1, y1), point(x2, y2), stroke.width, cap = stroke.cap)
        fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), tint, style = stroke)
        when (symbol) {
            CoachSymbol.COACH -> {
                drawCircle(tint, 9f * unit, point(12f, 12f), style = stroke)
                path { moveTo(8*unit, 15*unit); lineTo(11*unit, 8*unit); lineTo(16*unit, 11*unit); close() }
            }
            CoachSymbol.HISTORY -> {
                drawRoundRect(tint, point(4f, 3f), androidx.compose.ui.geometry.Size(16*unit,18*unit),
                    CornerRadius(3*unit), style = stroke)
                line(8f,8f,16f,8f); line(8f,12f,16f,12f); line(8f,16f,12f,16f)
            }
            CoachSymbol.SETTINGS -> {
                line(4f,7f,20f,7f); line(4f,17f,20f,17f)
                drawCircle(tint, 3*unit, point(9f,7f), style=stroke)
                drawCircle(tint, 3*unit, point(15f,17f), style=stroke)
            }
            CoachSymbol.ARROW -> { line(5f,12f,19f,12f); line(14f,7f,19f,12f); line(14f,17f,19f,12f) }
            CoachSymbol.BACK -> { line(19f,12f,5f,12f); line(10f,7f,5f,12f); line(10f,17f,5f,12f) }
            CoachSymbol.CHECK -> { line(5f,12f,10f,17f); line(10f,17f,19f,7f) }
            CoachSymbol.LEAF -> {
                path {
                    moveTo(5*unit,18*unit); cubicTo(1*unit,7*unit,12*unit,3*unit,20*unit,4*unit)
                    cubicTo(21*unit,14*unit,17*unit,22*unit,5*unit,18*unit); close()
                }
                line(5f,19f,15f,9f)
            }
            CoachSymbol.WAVE -> path {
                moveTo(2*unit,14*unit); cubicTo(6*unit,14*unit,5*unit,6*unit,10*unit,6*unit)
                cubicTo(15*unit,6*unit,13*unit,18*unit,18*unit,18*unit)
                cubicTo(21*unit,18*unit,21*unit,12*unit,23*unit,12*unit)
            }
            CoachSymbol.SHIELD -> {
                path {
                    moveTo(12*unit,2*unit); lineTo(20*unit,5*unit); lineTo(20*unit,12*unit)
                    quadraticTo(19*unit,18*unit,12*unit,22*unit)
                    quadraticTo(5*unit,18*unit,4*unit,12*unit); lineTo(4*unit,5*unit); close()
                }
                line(8f,11f,11f,14f); line(11f,14f,16f,9f)
            }
            CoachSymbol.PLAY -> path {
                moveTo(8*unit,4*unit); lineTo(20*unit,12*unit); lineTo(8*unit,20*unit); close()
            }
            CoachSymbol.STOP -> drawRoundRect(tint, point(6f,6f),
                androidx.compose.ui.geometry.Size(12*unit,12*unit), CornerRadius(2*unit))
        }
    }
}

@Composable
fun LiquidSelection(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    symbols: List<CoachSymbol>? = null,
    vertical: Boolean = false,
    enabled: Boolean = true,
) {
    val motion = LocalCoachMotion.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(28.dp)).background(colors.surfaceVariant)
            .padding(4.dp).selectableGroup(),
    ) {
        val cell = if (vertical) 88.dp else maxWidth / labels.size
        val targetStart = cell * selected
        val targetEnd = cell * (selected + 1)
        val selection = updateTransition(selected, label = "liquidSelection")
        val animatedStart by selection.animateDp(transitionSpec = {
            if (motion) tween(if (targetState > initialState) 240 else 180,
                easing = FastOutSlowInEasing) else snap()
        }, label = "liquidTrailingEdge") { cell * it }
        val animatedEnd by selection.animateDp(transitionSpec = {
            if (motion) tween(if (targetState > initialState) 180 else 240,
                easing = FastOutSlowInEasing) else snap()
        }, label = "liquidLeadingEdge") { cell * (it + 1) }
        val start = if (motion) animatedStart else targetStart
        val end = if (motion) animatedEnd else targetEnd
        Box(Modifier.matchParentSize()) {
            val indicator = if (vertical) {
                Modifier.offset(y = start).fillMaxWidth().height((end - start).coerceAtLeast(1.dp))
            } else {
                Modifier.offset(x = start).width((end - start).coerceAtLeast(1.dp)).fillMaxHeight()
            }
            Box(indicator.clip(shape).background(colors.surface))
        }
        @Composable
        fun Item(index: Int, modifier: Modifier) {
            val color = if (selected == index) colors.onSurface else colors.onSurfaceVariant
            Column(
                modifier.heightIn(min = 80.dp).clip(shape)
                    .selectable(selected == index, enabled = enabled, role = Role.Tab,
                        onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                symbols?.let {
                    CoachIcon(it[index], tint = color)
                    Spacer(Modifier.height(6.dp))
                }
                Text(labels[index], color = color, style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center)
            }
        }
        if (vertical) {
            Column { labels.indices.forEach { Item(it, Modifier.fillMaxWidth().height(88.dp)) } }
        } else {
            Row(Modifier.height(IntrinsicSize.Min)) {
                labels.indices.forEach { Item(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }
    }
}

@Composable
fun LiquidAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    symbol: CoachSymbol = CoachSymbol.ARROW,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalCoachMotion.current
    val scale by animateFloatAsState(if (pressed && motion) .985f else 1f,
        if (motion) tween(120) else snap(), label = "buttonPressure")
    val radius by animateDpAsState(if (pressed && motion) 19.dp else 24.dp,
        if (motion) tween(160) else snap(), label = "buttonContour")
    Button(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.heightIn(min = 80.dp).graphicsLayer {
            scaleX = if (motion) scale else 1f
            scaleY = if (motion) scale else 1f
        },
        shape = RoundedCornerShape(radius),
        colors = if (secondary) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) else ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(16.dp))
        CoachIcon(symbol)
    }
}

@Composable
fun CoachPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun LiquidScore(score: Int, description: String, caption: String, modifier: Modifier = Modifier, contour: Int = 0) {
    val motion = LocalCoachMotion.current
    val amount by animateFloatAsState(score / 100f,
        if (motion) tween(240, easing = FastOutSlowInEasing) else snap(), label = "scoreLevel")
    val progress = if (motion) amount else score / 100f
    val targetBend = listOf(.12f, -.13f, .06f)[contour.coerceIn(0, 2)]
    val animatedBend by animateFloatAsState(targetBend,
        if (motion) tween(240, easing = FastOutSlowInEasing) else snap(), label = "coachingContour")
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(208.dp).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val circle = Path().apply { addOval(Rect(Offset.Zero, size)) }
            drawCircle(colors.surfaceVariant)
            clipPath(circle) {
                val level = diameter * (1f - progress * .64f)
                val bend = diameter * if (motion) animatedBend else targetBend
                val water = Path().apply {
                    moveTo(0f, level)
                    cubicTo(diameter*.32f, level-bend, diameter*.58f, level+bend, diameter, level)
                    lineTo(diameter, diameter); lineTo(0f, diameter); close()
                }
                drawPath(water, colors.primaryContainer)
            }
            drawArc(colors.secondary, -90f, progress * 360f, false,
                topLeft = Offset(5.dp.toPx(),5.dp.toPx()),
                size = size.copy(width = diameter-10.dp.toPx(), height = diameter-10.dp.toPx()),
                style = Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score.toString(), style = MaterialTheme.typography.displayLarge)
            Text(caption, style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant)
        }
    }
}
