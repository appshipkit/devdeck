package io.devdeck.flags

import org.json.JSONTokener

public sealed class FeatureFlag<T : Any>(
    public val key: String,
    public val title: String,
    public val default: T,
    public val section: String,
    public val category: String,
    public val editable: Boolean,
) {
    internal abstract fun decode(raw: String): T?

    internal open fun encode(value: T): String = value.toString()

    internal abstract fun toJson(value: T): Any

    public class Toggle(
        key: String,
        default: Boolean,
        title: String = key,
        section: String = DEFAULT_SECTION,
        category: String = DEFAULT_CATEGORY,
        editable: Boolean = true,
    ) : FeatureFlag<Boolean>(key, title, default, section, category, editable) {
        override fun decode(raw: String): Boolean? = raw.toBooleanStrictOrNull()
        override fun toJson(value: Boolean): Any = value
    }

    public class Text(
        key: String,
        default: String,
        title: String = key,
        section: String = DEFAULT_SECTION,
        category: String = DEFAULT_CATEGORY,
        editable: Boolean = true,
    ) : FeatureFlag<String>(key, title, default, section, category, editable) {
        override fun decode(raw: String): String = raw
        override fun toJson(value: String): Any = value
    }

    public class Number(
        key: String,
        default: Double,
        title: String = key,
        section: String = DEFAULT_SECTION,
        category: String = DEFAULT_CATEGORY,
        editable: Boolean = true,
        public val wholeNumber: Boolean = false,
        public val range: ClosedFloatingPointRange<Double>? = null,
    ) : FeatureFlag<Double>(key, title, default, section, category, editable) {
        override fun decode(raw: String): Double? = raw.toDoubleOrNull()
        override fun toJson(value: Double): Any = if (wholeNumber) value.toLong() else value
    }

    public class Json(
        key: String,
        default: String,
        title: String = key,
        section: String = DEFAULT_SECTION,
        category: String = DEFAULT_CATEGORY,
        editable: Boolean = true,
    ) : FeatureFlag<String>(key, title, default, section, category, editable) {
        override fun decode(raw: String): String = raw
        override fun toJson(value: String): Any = runCatching { JSONTokener(value).nextValue() }.getOrDefault(value)
    }

    public companion object {
        public const val DEFAULT_SECTION: String = "Flags"
        public const val DEFAULT_CATEGORY: String = "general"
    }
}
