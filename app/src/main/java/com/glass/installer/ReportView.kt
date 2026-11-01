package com.glass.installer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReportSheet(r: Report, tilt: Offset, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val st by installStatus
    var started by remember(r) { mutableStateOf(false) }
    var filter by remember(r) { mutableStateOf(0) }
    var openPerm by remember(r) { mutableStateOf(-1) }

    val color = Pal.scoreColor(r.score)
    val verdict = when {
        r.score >= 75 -> "Secure lag raha hai"
        r.score >= 45 -> "Suspicious — savdhan"
        else -> "Khatarnak — install mat karo"
    }
    val verdictSub = when {
        r.score >= 75 -> "Koi bada red flag nahi mila. Phir bhi source check karo."
        r.score >= 45 -> "Kuch cheezein risky hain. Neeche wajah dekho."
        else -> "Kai serious red flags. Sirf trusted source se hi aage badho."
    }

    val danger = r.perms.count { it.risk == 2 }
    val sens = r.perms.count { it.risk == 1 }
    val normal = r.perms.count { it.risk == 0 }
    val shown = when (filter) {
        1 -> r.perms.filter { it.risk == 2 }
        2 -> r.perms.filter { it.risk == 1 }
        3 -> r.perms.filter { it.risk == 0 }
        else -> r.perms
    }

    fun share() {
        val txt = buildString {
            appendLine("Glass Installer report")
            appendLine("${r.label} (${r.pkg}) v${r.version} · ${r.size}")
            appendLine("Score: ${r.score}/100 — $verdict")
            appendLine("Min SDK ${r.minSdk} · Target SDK ${r.targetSdk}")
            appendLine("Permissions: ${r.perms.size} (${danger} khatarnak, ${sens} sensitive)")
            appendLine("SHA-256: ${r.sha}")
            appendLine("VirusTotal: ${r.vtUrl}")
            r.findings.forEach { appendLine("• ${it.text}") }
        }
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, txt)
        }
        ctx.startActivity(Intent.createChooser(i, "Report share karo"))
    }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.93f)
            .statusBarsPadding(),
        tilt = tilt,
        tint = Color(0xFF0A0E1E).copy(0.72f),
        radius = 30.dp,
        pad = 18.dp,
        depthScale = 0.4f
    ) {
        Handle()

        // ---- sticky header
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (r.icon != null) {
                Image(
                    r.icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(0.18f), RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) { LogoMark(30.dp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Txt(r.label, Type.h2, maxLines = 1)
                Txt("v${r.version} · ${r.size}", Type.micro, Color.White.copy(0.5f))
                Txt(r.pkg, Type.mono, Color.White.copy(0.4f), maxLines = 1)
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

        Gap(16.dp)

        // ---- scrollable body
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // verdict
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(color.copy(0.20f), Color.White.copy(0.05f))))
                    .border(1.dp, color.copy(0.35f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreRing(r.score, color, 84.dp, 9.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Txt(verdict, Type.h2, Color.White)
                    Gap(4.dp)
                    Txt(verdictSub, Type.small, Color.White.copy(0.65f))
                }
            }

            Gap(14.dp)

            // findings
            SectionTitle("Kya mila", "${r.findings.size}")
            Gap(8.dp)
            r.findings.forEach { f ->
                val c = when (f.level) {
                    2 -> Pal.rose
                    1 -> Pal.amber
                    else -> Pal.mint
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.05f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Dot(c)
                    Spacer(Modifier.width(10.dp))
                    Txt(f.text, Type.small, Color.White.copy(0.88f))
                }
            }

            Gap(18.dp)

            // sdk
            SectionTitle("SDK level")
            Gap(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(if (r.minSdk == 0) "—" else "${r.minSdk}", "Min", an(r.minSdk), Modifier.weight(1f))
                StatTile(if (r.targetSdk == 0) "—" else "${r.targetSdk}", "Target", an(r.targetSdk), Modifier.weight(1f))
                StatTile(if (r.compileSdk == 0) "—" else "${r.compileSdk}", "Compile", an(r.compileSdk), Modifier.weight(1f))
            }

            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Activities", "${r.activities}", Modifier.weight(1f))
                MiniStat("Services", "${r.services}", Modifier.weight(1f))
                MiniStat("Receivers", "${r.receivers}", Modifier.weight(1f))
                MiniStat("Providers", "${r.providers}", Modifier.weight(1f))
            }

            Gap(18.dp)

            // permissions
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Permissions", "${r.perms.size} total", modifier = Modifier.weight(1f))
            }
            Gap(10.dp)
            RiskBar(normal, sens, danger)
            Gap(8.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend("Normal", normal, Pal.mint)
                Legend("Sensitive", sens, Pal.amber)
                Legend("Khatarnak", danger, Pal.rose)
            }
            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip("Sab ${r.perms.size}", filter == 0, Pal.violet) { filter = 0 }
                FilterChip("Khatarnak $danger", filter == 1, Pal.rose) { filter = 1 }
                FilterChip("Sensitive $sens", filter == 2, Pal.amber) { filter = 2 }
                FilterChip("Normal $normal", filter == 3, Pal.mint) { filter = 3 }
            }
            Gap(8.dp)
            if (shown.isEmpty()) {
                Txt("Is category mein kuch nahi.", Type.small, Color.White.copy(0.45f), modifier = Modifier.padding(vertical = 8.dp))
            }
            shown.forEach { p ->
                val idx = r.perms.indexOf(p)
                val open = openPerm == idx
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(if (open) 0.09f else 0.05f))
                        .clickable { openPerm = if (open) -1 else idx }
                        .animateContentSize()
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(Pal.risk(p.risk))
                        Spacer(Modifier.width(10.dp))
                        Txt(p.label, Type.body, Color.White.copy(0.92f), modifier = Modifier.weight(1f))
                        Txt(
                            listOf("Normal", "Sensitive", "Khatarnak")[p.risk],
                            Type.micro,
                            Pal.risk(p.risk).copy(0.9f)
                        )
                    }
                    AnimatedVisibility(open) {
                        Column {
                            Gap(6.dp)
                            Txt(
                                if (p.why.isNotBlank()) p.why else "Ye permission app ke normal kaam ke liye hai. System ise install ke waqt dikhata hai.",
                                Type.small,
                                Color.White.copy(0.6f)
                            )
                            Txt(p.name, Type.mono, Color.White.copy(0.35f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            Gap(18.dp)

            // signature
            SectionTitle("Signature")
            Gap(8.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(if (r.cert == null) Pal.rose else if (r.mismatch) Pal.rose else Pal.mint)
                    Spacer(Modifier.width(10.dp))
                    Txt(
                        when {
                            r.cert == null -> "Unsigned APK"
                            r.mismatch -> "Signature installed app se alag!"
                            else -> "Signed — koi mismatch nahi"
                        },
                        Type.body, Color.White.copy(0.9f), modifier = Modifier.weight(1f)
                    )
                }
                r.certName?.let {
                    Txt(it, Type.micro, Color.White.copy(0.5f), modifier = Modifier.padding(top = 6.dp), maxLines = 2)
                }
                r.oldVersion?.let {
                    Txt(
                        "Pehle se installed: v$it" + if (r.mismatch) " (signature alag!)" else "",
                        Type.micro, Color.White.copy(0.5f), modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Gap(8.dp)
                Txt("SHA-256", Type.micro, Color.White.copy(0.4f))
                Txt(r.sha, Type.mono, Color.White.copy(0.7f))
                Gap(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("Hash copy") {
                        clip.setText(AnnotatedString(r.sha))
                    }
                    SmallAction("Share report") { share() }
                }
            }

            Gap(18.dp)

            // virustotal
            SectionTitle("VirusTotal")
            Gap(8.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                val vtColor = when (r.vt.state) {
                    "ok" -> if (r.vt.mal > 0) Pal.rose else Pal.mint
                    "nokey", "unknown" -> Pal.amber
                    else -> Pal.amber
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(vtColor)
                    Spacer(Modifier.width(10.dp))
                    Txt(vtText(r.vt), Type.body, Color.White.copy(0.9f), modifier = Modifier.weight(1f))
                }
                when (r.vt.state) {
                    "nokey" -> Txt(
                        "Settings mein free VirusTotal API key daalo — sirf hash bhejta hai, file nahi.",
                        Type.micro, Color.White.copy(0.45f), modifier = Modifier.padding(top = 6.dp)
                    )
                    "ok", "unknown" -> {
                        Gap(10.dp)
                        SmallAction("VirusTotal par kholo") {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.vtUrl)))
                        }
                    }
                    else -> Txt(
                        "Check fail hua — internet ya key dekho.",
                        Type.micro, Color.White.copy(0.45f), modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Gap(20.dp)
        }

        // ---- sticky install bar
        Gap(10.dp)
        val label = when {
            st == "ok" -> "Open karo"
            st == "cancel" -> "Cancel hua — phir try karo"
            st.startsWith("fail") -> "Fail hua — phir try karo"
            started -> "Install ho raha hai…"
            r.score < 45 -> "Phir bhi install karo"
            else -> "Install karo"
        }
        val grad = when {
            st == "ok" -> listOf(Pal.mint, Pal.mintDeep)
            r.score < 45 -> listOf(Pal.rose, Pal.roseDeep)
            else -> listOf(Pal.violet, Pal.blue)
        }
        Btn(label, grad, enabled = !(started && st == "")) {
            when {
                st == "ok" -> {
                    ctx.packageManager.getLaunchIntentForPackage(r.pkg)?.let { ctx.startActivity(it) }
                    onClose()
                }
                !ctx.packageManager.canRequestPackageInstalls() -> {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${ctx.packageName}")
                        )
                    )
                }
                else -> {
                    installStatus.value = ""
                    started = true
                    try {
                        install(ctx, r.file)
                    } catch (e: Exception) {
                        installStatus.value = "fail:" + (e.message ?: "unknown")
                    }
                }
            }
        }
        if (st.startsWith("fail")) {
            Gap(6.dp)
            Txt(st.removePrefix("fail:"), Type.micro, Color.White.copy(0.6f))
        }
        Gap(4.dp)
        GhostBtn("Band karo", 44.dp, onClose)
    }
}

private fun vtText(v: Vt) = when (v.state) {
    "ok" -> if (v.mal + v.sus == 0) "VirusTotal: ${v.total} engines, koi flag nahi" else "${v.mal + v.sus} / ${v.total} engines ne flag kiya"
    "unknown" -> "Ye file VirusTotal par nahi mili (naya ya rare APK)"
    "nokey" -> "Virus scan band hai — API key nahi hai"
    "badkey" -> "API key galat lag rahi hai"
    else -> "VirusTotal check fail hua"
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(0.05f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt(value, Type.h3.copy(fontWeight = FontWeight.Bold), Color.White.copy(0.92f))
        Txt(label, Type.micro, Color.White.copy(0.45f))
    }
}

@Composable
private fun Legend(label: String, n: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, 8.dp)
        Spacer(Modifier.width(6.dp))
        Txt("$label $n", Type.micro, Color.White.copy(0.6f))
    }
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.09f))
            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Txt(text, Type.micro.copy(fontWeight = FontWeight.SemiBold), Color.White.copy(0.85f))
    }
}
