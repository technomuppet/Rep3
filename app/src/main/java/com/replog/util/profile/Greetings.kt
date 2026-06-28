package com.replog.util.profile

/**
 * Pure, deterministic name-aware copy used across the app. The hour-of-day is
 * always passed in (never read from the clock here) so screens stay testable and
 * recompositions are stable.
 */
object Greetings {

    /** "Good morning, David." / "Good afternoon, David." / "Good evening, David." */
    fun timeOfDay(name: String, hourOfDay: Int): String {
        val part = when (hourOfDay) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        return "$part, ${clean(name)}."
    }

    /** "Welcome back, David." */
    fun welcomeBack(name: String): String = "Welcome back, ${clean(name)}."

    /** Possessive screen titles: "David's Workout Today", "David's Recovery Centre". */
    fun possessive(name: String, noun: String): String = "${possessiveName(name)} $noun"

    /** "David's" (handles names ending in s -> "Chris'"). */
    fun possessiveName(name: String): String {
        val n = clean(name)
        return if (n.endsWith("s", ignoreCase = true)) "$n'" else "$n's"
    }

    /** "David, you're fully recovered today." style sentence. */
    fun sentence(name: String, rest: String): String = "${clean(name)}, $rest"

    private fun clean(name: String): String = name.trim().ifBlank { "Athlete" }
}
