package com.glass.installer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** risk: 0 = normal, 1 = sensitive, 2 = dangerous */
data class Perm(val name: String, val label: String, val risk: Int, val why: String)

/** level: 0 = good, 1 = warning, 2 = bad */
data class Finding(val text: String, val level: Int)

data class Vt(val state: String, val mal: Int, val sus: Int, val total: Int)

data class Report(
    val file: File,
    val label: String,
    val pkg: String,
    val version: String,
    val size: String,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int,
    val perms: List<Perm>,
    val sha: String,
    val cert: String?,
    val certName: String?,
    val oldVersion: String?,
    val mismatch: Boolean,
    val downgrade: Boolean,
    val icon: Bitmap?,
    val score: Int,
    val findings: List<Finding>,
    val vt: Vt,
    val activities: Int,
    val services: Int,
    val receivers: Int,
    val providers: Int,
    val debuggable: Boolean
) {
    val vtUrl get() = "https://www.virustotal.com/gui/file/$sha"
}

val installStatus = mutableStateOf("")

private val HIGH = setOf(
    "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS",
    "BIND_ACCESSIBILITY_SERVICE", "SYSTEM_ALERT_WINDOW", "BIND_DEVICE_ADMIN", "MANAGE_EXTERNAL_STORAGE",
    "BIND_NOTIFICATION_LISTENER_SERVICE", "REQUEST_INSTALL_PACKAGES", "REQUEST_DELETE_PACKAGES",
    "BIND_INPUT_METHOD", "BIND_VPN_SERVICE", "PACKAGE_USAGE_STATS"
)

private val WHY = mapOf(
    "READ_SMS" to "Aapke saare SMS padh sakta hai — OTP chori ka seedha risk.",
    "RECEIVE_SMS" to "Aane wale SMS pakad sakta hai, bank OTP bhi.",
    "SEND_SMS" to "Aapke naam se premium SMS bhej sakta hai — paise kat sakte hain.",
    "READ_CALL_LOG" to "Call history padh sakta hai.",
    "WRITE_CALL_LOG" to "Call history badal sakta hai.",
    "PROCESS_OUTGOING_CALLS" to "Calls ko beech mein rok kar doosre number par bhej sakta hai.",
    "BIND_ACCESSIBILITY_SERVICE" to "Screen padh sakta hai aur khud tap kar sakta hai — bank app tak pahunch.",
    "SYSTEM_ALERT_WINDOW" to "Doosri apps ke upar apni window dikha sakta hai (fake login overlay).",
    "BIND_DEVICE_ADMIN" to "Device admin ban sakta hai — uninstall mushkil ho jata hai.",
    "MANAGE_EXTERNAL_STORAGE" to "Poori storage, saari files — photos, downloads, documents.",
    "BIND_NOTIFICATION_LISTENER_SERVICE" to "Har notification padh sakta hai, OTP notifications bhi.",
    "REQUEST_INSTALL_PACKAGES" to "Khud doosre APK install kar sakta hai — silent dropper ho sakta hai.",
    "REQUEST_DELETE_PACKAGES" to "Bina aapke apps uninstall karwa sakta hai.",
    "READ_CONTACTS" to "Aapke contacts padh sakta hai.",
    "WRITE_CONTACTS" to "Contacts badal sakta hai.",
    "CAMERA" to "Kabhi bhi camera chala sakta hai.",
    "RECORD_AUDIO" to "Kabhi bhi mic se awaaz record kar sakta hai.",
    "ACCESS_FINE_LOCATION" to "Aapki exact location track kar sakta hai.",
    "ACCESS_BACKGROUND_LOCATION" to "App band hone ke baad bhi location track kar sakta hai.",
    "READ_PHONE_STATE" to "Device ID aur SIM ki jaankari le sakta hai.",
    "READ_EXTERNAL_STORAGE" to "Storage ki files padh sakta hai.",
    "QUERY_ALL_PACKAGES" to "Aapke phone ki saari apps ki list dekh sakta hai."
)

private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

private fun riskOf(pm: PackageManager, name: String): Int {
    if (name.substringAfterLast('.') in HIGH) return 2
    return try {
        val p = pm.getPermissionInfo(name, 0)
        if ((p.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS) 1 else 0
    } catch (e: Exception) {
        0
    }
}

private fun prettyName(raw: String): String = raw.substringAfterLast('.')
    .removePrefix("android.permission.")
    .replace('_', ' ')
    .lowercase()
    .split(' ')
    .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

private fun vtLookup(sha: String, key: String): Vt {
    if (key.isBlank()) return Vt("nokey", 0, 0, 0)
    return try {
        val c = URL("https://www.virustotal.com/api/v3/files/$sha").openConnection() as HttpURLConnection
        c.setRequestProperty("x-apikey", key.trim())
        c.connectTimeout = 9000
        c.readTimeout = 9000
        when (c.responseCode) {
            200 -> {
                val st = JSONObject(c.inputStream.bufferedReader().readText())
                    .getJSONObject("data").getJSONObject("attributes")
                    .getJSONObject("last_analysis_stats")
                val m = st.optInt("malicious")
                val s = st.optInt("suspicious")
                Vt("ok", m, s, m + s + st.optInt("harmless") + st.optInt("undetected"))
            }
            404 -> Vt("unknown", 0, 0, 0)
            401, 403 -> Vt("badkey", 0, 0, 0)
            else -> Vt("error", 0, 0, 0)
        }
    } catch (e: Exception) {
        Vt("error", 0, 0, 0)
    }
}

/**
 * The whole analysis: copy the file, hash it, read the manifest, grade every
 * permission, compare signatures with the installed copy and (optionally) ask
 * VirusTotal about the hash. Runs on Dispatchers.IO; progress is reported back
 * so the UI can show real steps instead of a spinner.
 */
suspend fun analyze(
    ctx: Context,
    uri: Uri,
    vtKey: String,
    onStep: (String, Float) -> Unit = { _, _ -> }
): Report = withContext(Dispatchers.IO) {
    onStep("File copy ho rahi hai", 0.05f)
    val f = File(ctx.cacheDir, "pending.apk")
    val md = MessageDigest.getInstance("SHA-256")
    ctx.contentResolver.openInputStream(uri)?.use { i ->
        f.outputStream().use { o ->
            val b = ByteArray(65536)
            while (true) {
                val n = i.read(b)
                if (n < 0) break
                o.write(b, 0, n)
                md.update(b, 0, n)
            }
        }
    } ?: error("File kholi nahi ja saki")
    if (f.length() == 0L) error("File khaali hai")
    val sha = md.digest().joinToString("") { "%02x".format(it) }

    onStep("Manifest padha ja raha hai", 0.22f)
    val pm = ctx.packageManager
    val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES
    val pi = pm.getPackageArchiveInfo(f.path, flags) ?: error("Ye valid APK nahi hai")
    val ai = pi.applicationInfo ?: error("Manifest nahi padha ja saka")
    ai.sourceDir = f.path
    ai.publicSourceDir = f.path

    onStep("Permissions check ho rahi hain", 0.4f)
    val perms = (pi.requestedPermissions ?: emptyArray())
        .distinct()
        .map {
            val short = it.substringAfterLast('.')
            Perm(it, prettyName(it), riskOf(pm, it), WHY[short] ?: "")
        }
        .sortedWith(compareByDescending<Perm> { it.risk }.thenBy { it.label })

    onStep("Signature verify ho raha hai", 0.55f)
    val certBytes = pi.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
    val cert = certBytes?.let { sha256(it) }
    val certName = cert?.let { "Signer SHA-256: ${it.take(32)}…" }
    var old: String? = null
    var mismatch = false
    var downgrade = false
    try {
        val inst = pm.getPackageInfo(pi.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        old = inst.versionName
        val ic = inst.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()?.let { sha256(it) }
        mismatch = ic != null && cert != null && ic != cert
        if (Build.VERSION.SDK_INT >= 28) {
            downgrade = inst.longVersionCode > (pi.longVersionCode ?: 0L)
        }
    } catch (e: Exception) {
        // app installed nahi hai — normal case
    }

    onStep(if (vtKey.isBlank()) "Score nikala ja raha hai" else "VirusTotal se pooch rahe hain", 0.72f)
    val vt = vtLookup(sha, vtKey)
    onStep("Report taiyaar ho rahi hai", 0.92f)

    val tgt = ai.targetSdkVersion
    val dbg = (ai.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    var score = 100
    val fx = mutableListOf<Finding>()
    val high = perms.count { it.risk == 2 }
    val sens = perms.count { it.risk == 1 }

    if (high > 0) {
        score -= minOf(high * 11, 48)
        fx += Finding("$high khatarnak permission maangi gayi hai", 2)
    }
    if (sens > 0) {
        score -= minOf(sens * 2, 14)
        fx += Finding("$sens sensitive permission", 1)
    }
    if (tgt < 28) {
        score -= 22
        fx += Finding("Target SDK $tgt — bahut purana, naye safety rules nahi", 2)
    } else if (tgt < 33) {
        score -= 9
        fx += Finding("Target SDK $tgt — purana hai", 1)
    }
    if (cert == null) {
        score -= 30
        fx += Finding("Signature nahi mila", 2)
    }
    if (mismatch) {
        score -= 45
        fx += Finding("Installed app se signature alag — fake update ho sakta hai", 2)
    }
    if (downgrade) {
        score -= 8
        fx += Finding("Purana version hai (downgrade)", 1)
    }
    if (dbg) {
        score -= 12
        fx += Finding("Debug build hai (debuggable)", 1)
    }
    if (vt.state == "ok" && vt.mal > 0) {
        score = minOf(score - vt.mal * 8, if (vt.mal >= 3) 15 else 45)
        fx += Finding("${vt.mal} antivirus engines ne malware bataya", 2)
    }
    if (vt.state == "ok" && vt.mal == 0 && vt.sus == 0 && vt.total > 0) {
        fx += Finding("VirusTotal par saaf: ${vt.total} engines, 0 flag", 0)
    }
    if (high == 0 && sens == 0) fx += Finding("Koi khatarnak permission nahi", 0)
    if (!dbg && cert != null) fx += Finding("Signed build, koi mismatch nahi", 0)
    if (fx.none { it.level == 0 }) fx += Finding("${perms.size} permission, sab normal range mein", 0)

    val icon = try {
        ai.loadIcon(pm).toBitmap(180, 180)
    } catch (e: Exception) {
        null
    }

    Report(
        file = f,
        label = ai.loadLabel(pm).toString(),
        pkg = pi.packageName,
        version = pi.versionName ?: "?",
        size = "%.1f MB".format(f.length() / 1048576.0),
        minSdk = ai.minSdkVersion,
        targetSdk = tgt,
        compileSdk = if (Build.VERSION.SDK_INT >= 31) ai.compileSdkVersion else 0,
        perms = perms,
        sha = sha,
        cert = cert,
        certName = certName,
        oldVersion = old,
        mismatch = mismatch,
        downgrade = downgrade,
        icon = icon,
        score = score.coerceIn(0, 100),
        findings = fx.sortedByDescending { it.level },
        vt = vt,
        activities = pi.activities?.size ?: 0,
        services = pi.services?.size ?: 0,
        receivers = pi.receivers?.size ?: 0,
        providers = pi.providers?.size ?: 0,
        debuggable = dbg
    )
}

/** Hands the APK to the system installer through a PackageInstaller session. */
fun install(ctx: Context, f: File) {
    val pi = ctx.packageManager.packageInstaller
    val id = pi.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
    pi.openSession(id).use { s ->
        f.inputStream().use { i ->
            s.openWrite("app", 0, f.length()).use { o ->
                i.copyTo(o)
                s.fsync(o)
            }
        }
        val it = Intent(ctx, InstallReceiver::class.java)
        val pend = PendingIntent.getBroadcast(
            ctx, id, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        s.commit(pend.intentSender)
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        when (val s = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val a: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    i.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                a?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    c.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> installStatus.value = "ok"
            PackageInstaller.STATUS_FAILURE_ABORTED -> installStatus.value = "cancel"
            else -> installStatus.value = "fail:" +
                (i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: s.toString())
        }
    }
}
