package com.example.ui.reader

import android.content.Context

/** Page colours for the Markdown/TXT reader. AUTO follows the app theme. */
enum class ReaderTheme(val key: String, val label: String, val background: Long, val foreground: Long) {
    AUTO("auto", "Auto", 0xFFFAF9F5, 0xFF3D3D3A),
    LIGHT("light", "Light", 0xFFFAF9F5, 0xFF3D3D3A),
    SEPIA("sepia", "Sepia", 0xFFF4ECD8, 0xFF4B3F2F),
    DARK("dark", "Dark", 0xFF151514, 0xFFD1CFC7),
    BLACK("black", "Black", 0xFF000000, 0xFFD6D3CC)
}

/** Reader typeface. AUTO = sans for Markdown, monospace for TXT. */
enum class ReaderFont(val key: String, val label: String) {
    AUTO("auto", "Default"),
    SANS("sans", "Sans"),
    SERIF("serif", "Serif"),
    MONO("mono", "Mono")
}

/**
 * Viewer Appearance for Markdown and TXT. Persisted in SharedPreferences and applied live through
 * CSS variables (no reload, reading position kept). [fontSizePx] = 0 follows the app's text-size setting.
 */
data class ReaderAppearance(
    val zoomPercent: Int = 100,
    val theme: ReaderTheme = ReaderTheme.AUTO,
    val font: ReaderFont = ReaderFont.AUTO,
    val fontSizePx: Int = 0,
    val lineSpacing: Float = 1.6f,
    val marginPx: Int = 16,
    val readableWidth: Boolean = false,
    val justify: Boolean = false,
    val wrapText: Boolean = true,
    val wrapCode: Boolean = false
) {
    fun resolvedTheme(appDark: Boolean): ReaderTheme =
        if (theme == ReaderTheme.AUTO) (if (appDark) ReaderTheme.DARK else ReaderTheme.LIGHT) else theme

    fun isDark(appDark: Boolean): Boolean = resolvedTheme(appDark).let { it == ReaderTheme.DARK || it == ReaderTheme.BLACK }

    fun effectiveFontSize(appFontSetting: String, kind: ReaderKind): Int =
        if (fontSizePx > 0) fontSizePx.coerceIn(MIN_FONT_PX, MAX_FONT_PX) else defaultFontSize(appFontSetting, kind)

    fun resolve(appDark: Boolean, appFontSetting: String, kind: ReaderKind): ResolvedAppearance {
        val themeKey = resolvedTheme(appDark).key
        val size = effectiveFontSize(appFontSetting, kind)
        val zoom = zoomPercent.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val line = lineSpacing.coerceIn(1.1f, 2.4f)
        val margin = marginPx.coerceIn(0, 64)
        val fontKey = if (font == ReaderFont.AUTO) (if (kind == ReaderKind.MARKDOWN) "sans" else "mono") else font.key
        val json = buildString {
            append("{\"theme\":\"").append(themeKey).append('"')
            append(",\"font\":\"").append(font.key).append('"')
            append(",\"fontSize\":").append(size)
            append(",\"lineHeight\":").append(line)
            append(",\"margin\":").append(margin)
            append(",\"readableWidth\":").append(readableWidth)
            append(",\"justify\":").append(justify)
            append(",\"wrapText\":").append(wrapText)
            append(",\"wrapCode\":").append(wrapCode)
            append(",\"zoom\":").append(zoom)
            append('}')
        }
        val rootStyle = buildString {
            append("--fc-font: ").append(FONT_STACKS.getValue(fontKey)).append("; ")
            append("--fc-size: ").append(size).append("px; ")
            append("--fc-line: ").append(line).append("; ")
            append("--fc-pad: ").append(margin).append("px; ")
            append("--fc-maxw: ").append(if (readableWidth) "42em" else "none").append("; ")
            append("--fc-align: ").append(if (justify) "justify" else "left").append("; ")
            append("--fc-wrap: ").append(if (wrapText) "pre-wrap" else "pre").append("; ")
            append("--fc-wrap-word: ").append(if (wrapText) "anywhere" else "normal").append(';')
        }
        return ResolvedAppearance(
            themeKey = themeKey,
            json = json,
            rootStyle = rootStyle,
            zoomPercent = zoom,
            wrapText = wrapText,
            wrapCode = wrapCode,
            backgroundArgb = resolvedTheme(appDark).background.toInt(),
            isDark = isDark(appDark)
        )
    }

    companion object {
        const val MIN_ZOOM = 25
        const val MAX_ZOOM = 500
        const val MIN_FONT_PX = 10
        const val MAX_FONT_PX = 40

        /** Browser-like zoom steps used by the − / + buttons. */
        val ZOOM_STEPS = listOf(25, 33, 50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300, 400, 500)

        fun nextZoom(current: Int): Int = ZOOM_STEPS.firstOrNull { it > current } ?: MAX_ZOOM
        fun previousZoom(current: Int): Int = ZOOM_STEPS.lastOrNull { it < current } ?: MIN_ZOOM

        fun defaultFontSize(appFontSetting: String, kind: ReaderKind): Int {
            val markdown = kind == ReaderKind.MARKDOWN
            return when (appFontSetting) {
                "Small" -> if (markdown) 14 else 13
                "Large" -> if (markdown) 20 else 18
                else -> if (markdown) 16 else 15
            }
        }

        private val FONT_STACKS = mapOf(
            "sans" to "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans', Helvetica, Arial, sans-serif",
            "serif" to "'Noto Serif', 'Source Serif Pro', Georgia, Cambria, 'Times New Roman', serif",
            "mono" to "ui-monospace, 'JetBrains Mono', 'Roboto Mono', 'Droid Sans Mono', Menlo, Consolas, monospace"
        )
    }
}

/** An appearance resolved against the app theme, text-size setting and document kind. */
data class ResolvedAppearance(
    val themeKey: String,
    val json: String,
    val rootStyle: String,
    val zoomPercent: Int,
    val wrapText: Boolean,
    val wrapCode: Boolean,
    val backgroundArgb: Int,
    val isDark: Boolean
)

/** SharedPreferences storage for [ReaderAppearance] (one global reader setting for MD and TXT). */
object ReaderAppearanceStore {
    private const val PREFS = "reader_appearance"

    fun load(context: Context): ReaderAppearance {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaults = ReaderAppearance()
        return ReaderAppearance(
            zoomPercent = p.getInt("zoom", defaults.zoomPercent).coerceIn(ReaderAppearance.MIN_ZOOM, ReaderAppearance.MAX_ZOOM),
            theme = ReaderTheme.values().firstOrNull { it.key == p.getString("theme", null) } ?: defaults.theme,
            font = ReaderFont.values().firstOrNull { it.key == p.getString("font", null) } ?: defaults.font,
            fontSizePx = p.getInt("font_size", defaults.fontSizePx).let { if (it == 0) 0 else it.coerceIn(ReaderAppearance.MIN_FONT_PX, ReaderAppearance.MAX_FONT_PX) },
            lineSpacing = p.getFloat("line_spacing", defaults.lineSpacing).coerceIn(1.1f, 2.4f),
            marginPx = p.getInt("margin", defaults.marginPx).coerceIn(0, 64),
            readableWidth = p.getBoolean("readable_width", defaults.readableWidth),
            justify = p.getBoolean("justify", defaults.justify),
            wrapText = p.getBoolean("wrap_text", defaults.wrapText),
            wrapCode = p.getBoolean("wrap_code", defaults.wrapCode)
        )
    }

    fun save(context: Context, appearance: ReaderAppearance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("zoom", appearance.zoomPercent)
            .putString("theme", appearance.theme.key)
            .putString("font", appearance.font.key)
            .putInt("font_size", appearance.fontSizePx)
            .putFloat("line_spacing", appearance.lineSpacing)
            .putInt("margin", appearance.marginPx)
            .putBoolean("readable_width", appearance.readableWidth)
            .putBoolean("justify", appearance.justify)
            .putBoolean("wrap_text", appearance.wrapText)
            .putBoolean("wrap_code", appearance.wrapCode)
            .apply()
    }
}
