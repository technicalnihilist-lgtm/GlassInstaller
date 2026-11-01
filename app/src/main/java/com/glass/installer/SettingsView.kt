package com.glass.installer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSheet(tilt: Offset, onClose: () -> Unit) {
    val ctx = LocalContext.current

    @Composable
    fun slider(
        name: String,
        hint: String,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        onChange: (Float) -> Unit
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(name, Type.h3, modifier = Modifier.weight(1f))
                Txt("%.2f".format(value), Type.micro, Color.White.copy(0.5f))
            }
            Txt(hint, Type.micro, Color.White.copy(0.45f))
            Slider(
                value = value,
                onValueChange = { onChange(it); cfg.save() },
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Pal.violet,
                    inactiveTrackColor = Color.White.copy(0.15f)
                )
            )
        }
    }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tilt = tilt,
        tint = Color(0xFF0A0E1E).copy(0.78f),
        radius = 30.dp,
        pad = 18.dp,
        depthScale = 0.4f
    ) {
        Handle()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("Settings", Type.h1)
                Txt("Look, feel aur virus scan", Type.small, Color.White.copy(0.5f))
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Txt("✕", Type.small, Color.White.copy(0.8f)) }
        }
        Gap(12.dp)

        Column(Modifier.verticalScroll(rememberScrollState())) {
            // effects
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.05f))
                    .padding(14.dp)
            ) {
                Txt("Glass effects", Type.h3, Color.White.copy(0.9f))
                slider("Parallax", "Background blobs kitna hilein", cfg.parallax, 0f..2f) { cfg.parallax = it }
                slider("4D depth", "Cards ka tilt", cfg.depth, 0f..2f) { cfg.depth = it }
                slider("Transparency", "Glass ki dhundhlahat", cfg.glass, 0.04f..0.38f) { cfg.glass = it }
                slider("Bounce", "Open aur close ka spring", cfg.bounce, 0f..1f) { cfg.bounce = it }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.05f))
                        .clickable { cfg.motion = !cfg.motion; cfg.save() }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt("Motion effects", Type.body, Color.White.copy(0.9f))
                        Txt("Battery bachane ke liye band kar sakte ho", Type.micro, Color.White.copy(0.45f))
                    }
                    Box(
                        Modifier
                            .size(width = 46.dp, height = 26.dp)
                            .clip(CircleShape)
                            .background(if (cfg.motion) Pal.mint else Color.White.copy(0.18f)),
                        contentAlignment = if (cfg.motion) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Gap(12.dp)

            // virustotal
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.05f))
                    .padding(14.dp)
            ) {
                Txt("VirusTotal API key", Type.h3, Color.White.copy(0.9f))
                Gap(4.dp)
                Txt(
                    "virustotal.com se free key milti hai. Sirf file ka SHA-256 hash bheja jaata hai — APK kabhi upload nahi hota.",
                    Type.micro, Color.White.copy(0.5f)
                )
                OutlinedTextField(
                    value = cfg.vtKey,
                    onValueChange = { cfg.vtKey = it.trim(); cfg.save() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Txt("API key paste karo", Type.small, Color.White.copy(0.3f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(0.85f),
                        focusedBorderColor = Pal.violet,
                        unfocusedBorderColor = Color.White.copy(0.16f),
                        cursorColor = Pal.violet,
                        focusedContainerColor = Color.White.copy(0.04f),
                        unfocusedContainerColor = Color.White.copy(0.04f)
                    )
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Pal.cyan.copy(0.12f))
                        .clickable {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.virustotal.com/gui/my-apikey")))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Txt("Key kahan se milegi →", Type.micro.copy(fontWeight = FontWeight.SemiBold), Pal.cyan)
                }
            }

            Gap(12.dp)

            // about
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.05f))
                    .padding(14.dp)
            ) {
                Txt("About", Type.h3, Color.White.copy(0.9f))
                Gap(6.dp)
                InfoRow("App", "Glass Installer")
                InfoRow("Version", "2.0")
                InfoRow("Min Android", "9 (API 28)")
                InfoRow("Analysis", "Poori tarah on-device")
                Gap(8.dp)
                Txt(
                    "Koi analytics nahi, koi ad nahi. History sirf aapke phone par rehti hai.",
                    Type.micro, Color.White.copy(0.45f)
                )
            }

            Gap(14.dp)
            Btn("Ho gaya", listOf(Pal.violet, Pal.blue)) { onClose() }
            Gap(6.dp)
            GhostBtn("History saaf karo", 44.dp) { cfg.clearHistory() }
            Gap(16.dp)
        }
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(k, Type.small, Color.White.copy(0.5f), modifier = Modifier.weight(1f))
        Txt(v, Type.small, Color.White.copy(0.85f))
    }
}
