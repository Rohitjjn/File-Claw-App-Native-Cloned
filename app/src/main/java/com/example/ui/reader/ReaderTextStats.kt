package com.example.ui.reader

/** What a save would change, shown in the save confirmation. */
data class ChangeSummary(
    val addedLines: Int,
    val removedLines: Int,
    val words: Int,
    val characters: Int,
    val identical: Boolean
)

/** Small pure helpers for the editor UI (unit tested). */
object ReaderTextStats {
    private val word = Regex("\\S+")

    fun wordCount(text: String): Int = word.findAll(text).count()

    /**
     * Line-level change counts: common leading/trailing lines are skipped, the middle is compared as
     * a multiset so a line that only moved inside the edited region is not counted. A modified line
     * counts once as removed and once as added.
     */
    fun summarize(original: String, edited: String): ChangeSummary {
        if (original == edited) return ChangeSummary(0, 0, wordCount(edited), edited.length, true)
        val a = original.replace("\r\n", "\n").split('\n')
        val b = edited.replace("\r\n", "\n").split('\n')
        var prefix = 0
        while (prefix < a.size && prefix < b.size && a[prefix] == b[prefix]) prefix++
        var suffix = 0
        while (suffix < a.size - prefix && suffix < b.size - prefix && a[a.size - 1 - suffix] == b[b.size - 1 - suffix]) suffix++
        val removedMiddle = a.subList(prefix, a.size - suffix)
        val addedMiddle = b.subList(prefix, b.size - suffix)
        val remaining = HashMap<String, Int>()
        for (line in removedMiddle) remaining[line] = (remaining[line] ?: 0) + 1
        var added = 0
        for (line in addedMiddle) {
            val count = remaining[line] ?: 0
            if (count > 0) remaining[line] = count - 1 else added++
        }
        val removed = remaining.values.sum()
        return ChangeSummary(added, removed, wordCount(edited), edited.length, false)
    }

    /** "just now", "42 s ago", "5 min ago", or a clock time for older entries. */
    fun relativeTime(timeMillis: Long, nowMillis: Long, clock: (Long) -> String): String {
        val seconds = ((nowMillis - timeMillis) / 1000).coerceAtLeast(0)
        return when {
            seconds < 10 -> "just now"
            seconds < 60 -> "$seconds s ago"
            seconds < 3600 -> "${seconds / 60} min ago"
            else -> clock(timeMillis)
        }
    }
}
