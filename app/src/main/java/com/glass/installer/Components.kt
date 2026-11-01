package com.glass.installer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Motion spec shared by every press, sheet and ring in the app. */
fun <T> spr() = spring<T>(dampingRatio = 0.86f - cfg.bounce * 0.42f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun Txt(
    s: String,
    style: TextStyle = Type.body,
    color: Color = Pal.ink,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier
) {
    Text(
        text = s,
        style = style,
        color = color,
        fontWeight = weight,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Material "settings" cog, so the gear needs no icon dependency. */
private const val GEAR_PATH =
    "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61" +
        "l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54" +
        "c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94" +
        "l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58" +
        "c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32" +
        "c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84" +
        "c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22" +
        "l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6" +
        "s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"

/** The one glass surface: soft fill, bright hairline edge, optional 4D tilt. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tilt: Offset = Offset.Zero,
    tint: Color = Color.Transparent,
    radius: Dp = 26.dp,
    pad: Dp = 16.dp,
    depthScale: Float = 1f,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val d = LocalDensity.current.density
    val tiltK = if (cfg.motion) cfg.depth * depthScale else 0f
    Column(
        modifier
            .graphicsLayer {
                rotationY = tilt.x * 5f * tiltK
                rotationX = -tilt.y * 5f * tiltK
                cameraDistance = 20f * d
            }
            .clip(shape)
            .background(tint)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(cfg.glass + 0.10f),
                        Color.White.copy(cfg.glass * 0.55f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(0.50f),
                        Color.White.copy(0.05f),
                        Color.White.copy(0.18f)
                    )
                ),
                shape
            )
            .padding(pad),
        content = content
    )
}

/** Primary action: gradient fill, spring press, optional leading mark. */
@Composable
fun Btn(
    text: String,
    colors: List<Color>,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    onClick: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pr) 0.95f else 1f, spr(), label = "btn")
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = sc
                scaleY = sc
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors))
            .border(1.dp, Color.White.copy(0.25f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, Type.h2.copy(fontWeight = FontWeight.Bold), Color.White)
    }
}

/** Quiet secondary action. */
@Composable
fun GhostBtn(text: String, height: Dp = 46.dp, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pr) 0.96f else 1f, spr(), label = "ghost")
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.07f))
            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, Type.h3, Color.White.copy(0.9f))
    }
}

@Composable
fun Pill(text: String, color: Color, solid: Boolean = false) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (solid) color else color.copy(0.14f))
            .border(1.dp, color.copy(if (solid) 0f else 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Txt(
            text,
            Type.micro.copy(fontWeight = FontWeight.SemiBold),
            if (solid) Color(0xFF05070F) else color
        )
    }
}

/** Score ring with an animated sweep. */
@Composable
fun ScoreRing(score: Int, color: Color, size: Dp = 78.dp, stroke: Dp = 9.dp) {
    val sweep by animateFloatAsState(score / 100f, spr(), label = "ring")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val st = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(0.12f), 0f, 360f, false, style = st)
            drawArc(color, -90f, 360f * sweep, false, style = st)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("$score", Type.number, Color.White)
            Txt("/ 100", Type.micro, Color.White.copy(0.5f))
        }
    }
}

/** Thin multi-colour bar showing how permissions are distributed by risk. */
@Composable
fun RiskBar(normal: Int, sensitive: Int, danger: Int, height: Dp = 10.dp) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(0.1f))
    ) {
        if (danger > 0) Box(
            Modifier
                .weight(danger.toFloat())
                .fillMaxHeight()
                .background(Pal.rose)
        )
        if (sensitive > 0) Box(
            Modifier
                .weight(sensitive.toFloat())
                .fillMaxHeight()
                .background(Pal.amber)
        )
        if (normal > 0) Box(
            Modifier
                .weight(normal.toFloat())
                .fillMaxHeight()
                .background(Pal.mint)
        )
    }
}

@Composable
fun Handle() {
    Box(
        Modifier
            .padding(top = 2.dp, bottom = 14.dp)
            .size(width = 42.dp, height = 5.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.28f))
    )
}

@Composable
fun SectionTitle(title: String, trailing: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Txt(title, Type.h3, Color.White.copy(0.95f), modifier = Modifier.weight(1f))
        if (trailing != null) Txt(trailing, Type.small, Color.White.copy(0.55f))
    }
}

/** Settings button — a drawn gear, so no icon dependency is needed. */
@Composable
fun GearButton(onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pr) 0.9f else 1f, spr(), label = "gear")
    Box(
        Modifier
            .size(52.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White.copy(0.20f), Color.White.copy(0.06f))))
            .border(1.dp, Color.White.copy(0.28f), CircleShape)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(21.dp)) {
            val k = size.minDimension / 24f
            val path = androidx.compose.ui.graphics.vector.PathParser()
                .parsePathString(GEAR_PATH)
                .toPath()
            scale(k, pivot = Offset.Zero) { drawPath(path, Color.White.copy(0.92f)) }
        }
    }
}

/** App mark: a glass shield with a check. Used in the header and on empty states. */
@Composable
fun LogoMark(size: Dp = 44.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.06f)
                lineTo(w * 0.9f, h * 0.24f)
                lineTo(w * 0.9f, h * 0.55f)
                cubicTo(w * 0.9f, h * 0.78f, w * 0.72f, h * 0.9f, w * 0.5f, h * 0.96f)
                cubicTo(w * 0.28f, h * 0.9f, w * 0.1f, h * 0.78f, w * 0.1f, h * 0.55f)
                lineTo(w * 0.1f, h * 0.24f)
                close()
            }
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(Pal.violet.copy(0.85f), Pal.cyan.copy(0.55f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            drawPath(path, Color.White.copy(0.55f), style = Stroke(width = w * 0.035f))
            val st = Stroke(width = w * 0.085f, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.32f, h * 0.5f), Offset(w * 0.45f, h * 0.64f), st.width, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.45f, h * 0.64f), Offset(w * 0.69f, h * 0.38f), st.width, StrokeCap.Round)
        }
    }
}

/** Small dot used in findings lists. */
@Composable
fun Dot(color: Color, size: Dp = 9.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** Fixed-height spacer helper keeps the vertical rhythm identical everywhere. */
@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun StatTile(value: String, label: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(0.08f))
            .border(1.dp, Color.White.copy(0.10f), RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, Type.h2.copy(fontWeight = FontWeight.Bold), Color.White)
        Txt(label, Type.micro, Color.White.copy(0.55f), modifier = Modifier.padding(top = 2.dp))
        Txt(sub, Type.micro, Color.White.copy(0.85f), align = TextAlign.Center)
    }
}

/** Round icon-less "chip" button used by the permission filter row. */
@Composable
fun FilterChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (selected) color.copy(0.22f) else Color.White.copy(0.06f))
            .border(1.dp, if (selected) color.copy(0.7f) else Color.White.copy(0.12f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Txt(text, Type.micro.copy(fontWeight = FontWeight.SemiBold), if (selected) Color.White else Color.White.copy(0.6f))
    }
}

/** Progress bar used by the scanner overlay. */
@Composable
fun ProgressBar(progress: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.12f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color, Pal.cyan)))
        )
    }
}

/** Empty-state art: a dashed circle with the logo inside. */
@Composable
fun EmptyArt() {
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                Color.White.copy(0.14f), 0f, 360f, false,
                topLeft = Offset(size.width * 0.06f, size.height * 0.06f),
                size = Size(size.width * 0.88f, size.height * 0.88f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        LogoMark(40.dp)
    }
}
