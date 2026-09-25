package io.github.ahmadnayfeh.silah.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Neutral Arabic phrasing: numbers stated as facts, never as judgements. */
object ArabicText {

    /** Correct Arabic noun form for "day" after a number. */
    private fun days(n: Long): String = when {
        n % 100 in 3..10 -> "$n أيام"
        n % 100 in 11..99 -> "$n يوماً"
        else -> "$n يوم"
    }

    /** "اليوم" / "أمس" / "قبل يومين" / "قبل 5 أيام" / "قبل 12 يوماً". */
    fun ago(daysSince: Long): String = when (daysSince) {
        0L -> "اليوم"
        1L -> "أمس"
        2L -> "قبل يومين"
        else -> "قبل ${days(daysSince)}"
    }

    fun lastContact(daysSince: Long?): String =
        if (daysSince == null) "لم تتواصلا بعد" else "آخر تواصل ${ago(daysSince)}"

    /** "يومياً" / "كل 3 أيام" / "كل أسبوع" / "كل أسبوعين" / "كل شهر" / "كل 45 يوماً". */
    fun every(targetDays: Int): String = when (targetDays) {
        1 -> "يومياً"
        2 -> "كل يومين"
        7 -> "كل أسبوع"
        14 -> "كل أسبوعين"
        30 -> "كل شهر"
        else -> "كل ${days(targetDays.toLong())}"
    }

    /** "اليوم" / "غداً" / "بعد يومين" / "بعد 5 أيام". */
    fun inDays(n: Int): String = when (n) {
        0 -> "اليوم"
        1 -> "غداً"
        2 -> "بعد يومين"
        else -> "بعد ${days(n.toLong())}"
    }

    /** "غداً ميلاد سارة". */
    fun occasion(title: String, name: String, daysUntil: Int): String =
        "${inDays(daysUntil)} ${title.trim()} $name"

    fun perWeek(n: Int): String = when (n) {
        0 -> "لا اقتراحات حالياً"
        1 -> "اقتراح واحد في الأسبوع"
        2 -> "اقتراحان في الأسبوع"
        else -> "$n اقتراحات في الأسبوع"
    }

    fun coverage(contacted: Int, total: Int): String = "تواصلت مع $contacted من $total هذا الشهر"

    private val MONTHS = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
    )

    fun month(m: YearMonth): String = MONTHS[m.monthValue - 1]

    fun monthShort(m: YearMonth): String = MONTHS[m.monthValue - 1]

    fun date(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"

    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"

    fun dayName(d: DayOfWeek): String = when (d) {
        DayOfWeek.SATURDAY -> "السبت"
        DayOfWeek.SUNDAY -> "الأحد"
        DayOfWeek.MONDAY -> "الاثنين"
        DayOfWeek.TUESDAY -> "الثلاثاء"
        DayOfWeek.WEDNESDAY -> "الأربعاء"
        DayOfWeek.THURSDAY -> "الخميس"
        DayOfWeek.FRIDAY -> "الجمعة"
    }

    /** Single-letter label for compact day chips. */
    fun dayShort(d: DayOfWeek): String = when (d) {
        DayOfWeek.SATURDAY -> "س"
        DayOfWeek.SUNDAY -> "ح"
        DayOfWeek.MONDAY -> "ن"
        DayOfWeek.TUESDAY -> "ث"
        DayOfWeek.WEDNESDAY -> "ر"
        DayOfWeek.THURSDAY -> "خ"
        DayOfWeek.FRIDAY -> "ج"
    }

    /** Minutes after midnight → "7:30 م". */
    fun time(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val suffix = if (h < 12) "ص" else "م"
        return "$h12:${m.toString().padStart(2, '0')} $suffix"
    }

    fun intervalVsTarget(average: Double?, target: Int): String {
        val avg = average?.let { "كل ${days(Math.round(it).coerceAtLeast(1))}" } ?: "—"
        return "$avg / الهدف $target"
    }
}
