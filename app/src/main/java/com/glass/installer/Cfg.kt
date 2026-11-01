package com.glass.installer

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** A finished scan, kept only as a one-line history card on the home screen. */
data class Hist(val name: String, val pkg: String, val score: Int, val ts: Long, val size: String)

/**
 * Live app settings. Every field is Compose state, so a slider drag re-renders
 * the glass effect instantly and is persisted in the same gesture.
 */
class Cfg {
    var parallax by mutableStateOf(1f)
    var depth by mutableStateOf(1f)
    var glass by mutableStateOf(0.14f)
    var bounce by mutableStateOf(0.6f)
    var vtKey by mutableStateOf("")
    var motion by mutableStateOf(true)

    private var sp: SharedPreferences? = null

    fun bind(ctx: Context) {
        if (sp != null) return
        val p = ctx.getSharedPreferences("glass_installer", Context.MODE_PRIVATE)
        sp = p
        parallax = p.getFloat("par", 1f)
        depth = p.getFloat("dep", 1f)
        glass = p.getFloat("gl", 0.14f)
        bounce = p.getFloat("bo", 0.6f)
        vtKey = p.getString("vt", "") ?: ""
        motion = p.getBoolean("motion", true)
    }

    fun save() {
        sp?.edit()
            ?.putFloat("par", parallax)
            ?.putFloat("dep", depth)
            ?.putFloat("gl", glass)
            ?.putFloat("bo", bounce)
            ?.putString("vt", vtKey)
            ?.putBoolean("motion", motion)
            ?.apply()
    }

    fun history(): List<Hist> {
        val raw = sp?.getString("hist", null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                Hist(o.optString("n"), o.optString("p"), o.optInt("s"), o.optLong("t"), o.optString("z"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(h: Hist) {
        val list = (listOf(h) + history()).distinctBy { it.pkg }.take(8)
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("n", it.name).put("p", it.pkg).put("s", it.score).put("t", it.ts).put("z", it.size))
        }
        sp?.edit()?.putString("hist", a.toString())?.apply()
    }

    fun clearHistory() {
        sp?.edit()?.remove("hist")?.apply()
    }
}

val cfg = Cfg()
