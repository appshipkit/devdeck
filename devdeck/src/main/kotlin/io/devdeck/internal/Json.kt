package io.devdeck.internal

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal object Json {

    fun parse(raw: String): Any {
        val parsed = JSONTokener(raw.trim()).nextValue()
        require(parsed is JSONObject || parsed is JSONArray) { "Expected a JSON object or array" }
        return parsed
    }

    fun isJson(raw: String?): Boolean = raw != null && runCatching { parse(raw) }.isSuccess

    fun error(raw: String): String? = when {
        raw.isBlank() -> "JSON is empty"
        else -> runCatching { parse(raw) }.exceptionOrNull()?.let { (it.message ?: "Invalid JSON").substringBefore(" of ") }
    }

    fun pretty(raw: String): String = when (val parsed = parse(raw)) {
        is JSONObject -> parsed.toString(2)
        is JSONArray -> parsed.toString(2)
        else -> raw
    }

    fun minify(raw: String): String = parse(raw).toString()

    fun prettyOrNull(raw: String?): String? = raw?.let { runCatching { pretty(it) }.getOrNull() }
}
