package com.glass.installer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    tilt: Offset,
    busy: Boolean,
    err: String?,
    history: List<Hist>,
    onPick: () -> Unit,
    onSettings: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Gap(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogoMark(46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt("Glass Installer", Type.h1)
                Txt("Install se pehle sab kuch jaancho", Type.small, Color.White.copy(0.6f))
            }
            GearButton(onSettings)
        }

        Gap(20.dp)
        PickCard(busy = busy, tilt = tilt, onPick = onPick)

        err?.let {
            Gap(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pal.rose.copy(0.12f))
                    .border(1.dp, Pal.rose.copy(0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(Pal.rose)
                Spacer(Modifier.width(10.dp))
                Txt(it, Type.small, Color.White.copy(0.9f))
            }
        }

        Gap(18.dp)
        ChecksCard(tilt)

        Gap(18.dp)
        HistoryCard(history, tilt, onClearHistory)

        Gap(18.dp)
        Txt(
            "Sab jaanch aapke phone par hoti hai. Sirf VirusTotal par file ka SHA-256 hash jaata hai, APK nahi.",
            Type.micro,
            Color.White.copy(0.4f),
            align = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )
        Gap(28.dp)
    }
}

@Composable
private fun PickCard(busy: Boolean, tilt: Offset, onPick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale = androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.985f else 1f, spr(), label = "pick"
    )
    GlassCard(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .clickable(interactionSource = src, indication = null, enabled = !busy, onClick = onPick),
        tilt = tilt,
        tint = Pal.violet.copy(0.10f),
        radius = 30.dp,
        pad = 20.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Pal.violet, Pal.cyan, Pal.pink)))
        )
        Gap(18.dp)
        Txt(if (busy) "Jaanch chal rahi hai…" else "APK chuniye", Type.hero)
        Gap(8.dp)
        Txt(
            "Manifest, permissions, SDK, signature aur virus — sab install se pehle, ek jagah.",
            Type.body,
            Color.White.copy(0.68f)
        )
        Gap(18.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Pal.violet, Pal.indigo, Pal.blue)))
                .border(1.dp, Color.White.copy(0.28f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("＋", Type.h2.copy(fontWeight = FontWeight.Bold), Color.White)
                Spacer(Modifier.width(8.dp))
                Txt("File browser se APK chuno", Type.h3.copy(fontWeight = FontWeight.Bold), Color.White)
            }
        }
        Gap(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("Offline", Pal.mint)
            Spacer(Modifier.width(8.dp))
            Pill("Koi upload nahi", Pal.cyan)
        }
    }
}

@Composable
private fun ChecksCard(tilt: Offset) {
    val rows = listOf(
        Triple("Permissions", "Kaun si, kitni aur kitni khatarnak", Pal.rose),
        Triple("SDK level", "Min, Target aur Compile — purana app naya risk", Pal.amber),
        Triple("Signature", "Original signer aur installed app se mismatch", Pal.cyan),
        Triple("VirusTotal", "70+ engines ka virus scan, sirf hash bhej ke", Pal.mint)
    )
    GlassCard(Modifier.fillMaxWidth(), tilt = tilt) {
        SectionTitle("Kya check hota hai", "${rows.size} layers")
        Gap(12.dp)
        rows.forEachIndexed { i, (title, desc, color) ->
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Txt(title, Type.h3)
                    Txt(desc, Type.small, Color.White.copy(0.55f))
                }
            }
            if (i != rows.lastIndex) {
                Box(
                    Modifier
                        .padding(start = 22.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(0.07f))
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(history: List<Hist>, tilt: Offset, onClear: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), tilt = tilt) {
        SectionTitle("Recent scans", if (history.isEmpty()) null else "${history.size}")
        Gap(10.dp)
        if (history.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyArt()
                Gap(10.dp)
                Txt("Abhi koi scan nahi hua", Type.h3, Color.White.copy(0.8f))
                Txt("Pehla APK chuno — report yahan save rahegi.", Type.small, Color.White.copy(0.45f))
            }
        } else {
            history.forEachIndexed { i, h ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Pal.scoreColor(h.score).copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Txt("${h.score}", Type.small.copy(fontWeight = FontWeight.Bold), Pal.scoreColor(h.score))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Txt(h.name, Type.h3, modifier = Modifier)
                        Txt("${h.pkg} · ${h.size}", Type.micro, Color.White.copy(0.45f))
                    }
                    Pill(ago(h.ts), Color.White.copy(0.5f))
                }
                if (i != history.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.07f)))
                }
            }
            Gap(6.dp)
            GhostBtn("History saaf karo", height = 42.dp, onClick = onClear)
        }
    }
}

private fun ago(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    val m = d / 60000
    return when {
        m < 1 -> "abhi"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}
