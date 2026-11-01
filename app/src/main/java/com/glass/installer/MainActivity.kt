package com.glass.installer

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val incoming = mutableStateOf<Uri?>(null)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        enableEdgeToEdge()
        cfg.bind(applicationContext)
        incoming.value = intent?.data
        setContent {
            GlassTheme {
                Root(incoming.value) { incoming.value = null }
            }
        }
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        incoming.value = i.data
    }
}

/** Reads gravity so the glass layers shift with the phone — the 4D effect. */
@Composable
fun rememberTilt(): Offset {
    val ctx = LocalContext.current
    val raw = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val s = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                raw.value = Offset(
                    (-e.values[0] / 9.8f).coerceIn(-1f, 1f),
                    (e.values[1] / 9.8f - 0.6f).coerceIn(-1f, 1f)
                )
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        s?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm.unregisterListener(l) }
    }
    val target = if (cfg.motion) raw.value else Offset.Zero
    return animateOffsetAsState(target, spring(stiffness = 60f), label = "tilt").value
}

@Composable
private fun BoxScope.Blob(color: Color, size: Dp, tilt: Offset, k: Float, align: Alignment, x: Dp, y: Dp) {
    val d = LocalDensity.current
    Box(
        Modifier
            .align(align)
            .offset(x, y)
            .offset {
                IntOffset(
                    (tilt.x * k * cfg.parallax).dp.roundToPx(),
                    (tilt.y * k * cfg.parallax).dp.roundToPx()
                )
            }
            .size(size)
            .blur(48.dp)
            .background(Brush.radialGradient(listOf(color.copy(0.55f), Color.Transparent)))
    )
}

@Composable
private fun Root(incoming: Uri?, consume: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tilt = rememberTilt()

    var report by remember { mutableStateOf<Report?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var err by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(cfg.history()) }

    fun start(u: Uri) {
        scope.launch {
            scanning = true
            err = null
            progress = 0f
            step = "File copy ho rahi hai"
            installStatus.value = ""
            try {
                val r = analyze(ctx, u, cfg.vtKey) { s, p ->
                    step = s
                    progress = p
                }
                report = r
                cfg.addHistory(Hist(r.label, r.pkg, r.score, System.currentTimeMillis(), r.size))
                history = cfg.history()
                progress = 1f
                scanning = false
                showReport = true
            } catch (e: Exception) {
                scanning = false
                err = e.message ?: "Kuch galat ho gaya"
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) start(u)
    }
    LaunchedEffect(incoming) {
        incoming?.let {
            start(it)
            consume()
        }
    }

    BackHandler(enabled = showReport || showSettings) {
        showReport = false
        showSettings = false
    }

    Box(Modifier.fillMaxSize().background(Pal.bg)) {
        // ambient background
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1024), Pal.bg, Color(0xFF070510))
                    )
                )
        )
        Blob(Pal.violet, 460.dp, tilt, 42f, Alignment.TopStart, (-140).dp, (-120).dp)
        Blob(Pal.cyan, 380.dp, tilt, 68f, Alignment.CenterEnd, 130.dp, 30.dp)
        Blob(Pal.pink, 420.dp, tilt, 96f, Alignment.BottomStart, (-90).dp, 110.dp)
        if (cfg.motion) {
            Blob(Pal.blue, 300.dp, tilt, 54f, Alignment.CenterStart, (-160).dp, (-40).dp)
        }

        // content
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HomeScreen(
                tilt = tilt,
                busy = scanning,
                err = err,
                history = history,
                onPick = {
                    picker.launch(
                        arrayOf(
                            "application/vnd.android.package-archive",
                            "application/octet-stream"
                        )
                    )
                },
                onSettings = { showSettings = true },
                onClearHistory = {
                    cfg.clearHistory()
                    history = emptyList()
                }
            )
        }

        // scrim
        AnimatedVisibility(
            visible = showReport || showSettings || scanning,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showReport = false
                        showSettings = false
                    }
            )
        }

        // scanner overlay
        AnimatedVisibility(
            visible = scanning,
            enter = fadeIn(tween(200)) + scaleIn(tween(220), 0.94f),
            exit = fadeOut(tween(200))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(
                    Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                    tilt = tilt,
                    tint = Color(0xFF0A0E1E).copy(0.85f),
                    radius = 28.dp,
                    pad = 22.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LogoMark(38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Txt("Jaanch chal rahi hai", Type.h2)
                            Txt("Sab kuch aapke phone par", Type.micro, Color.White.copy(0.5f))
                        }
                    }
                    Gap(18.dp)
                    ProgressBar(progress, Pal.violet)
                    Gap(12.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(step, Type.small, Color.White.copy(0.75f), modifier = Modifier.weight(1f))
                        Txt("${(progress * 100).toInt()}%", Type.small, Color.White.copy(0.5f))
                    }
                    Gap(6.dp)
                    Txt(
                        "Bade APK mein 10-20 second lag sakte hain.",
                        Type.micro,
                        Color.White.copy(0.35f)
                    )
                }
            }
        }

        // sheets
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = showReport && !scanning,
                enter = slideInVertically(spr()) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(200))
            ) {
                report?.let { r ->
                    ReportSheet(r, tilt) { showReport = false }
                }
            }
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(spr()) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(200))
            ) {
                SettingsSheet(tilt) { showSettings = false }
            }
        }
    }
}
