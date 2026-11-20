package com.vyora.music.data.lyrics

/**
 * Romanisasi otomatis murni Kotlin (tanpa kamus berat) untuk lirik:
 * - Korea (Hangul -> Revised Romanization)
 * - Jepang (Hiragana & Katakana -> Hepburn Romaji)
 */
object LyricsRomanizer {

    private val HANGUL_INITIALS = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
        "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    )
    private val HANGUL_VOWELS = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa",
        "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )
    private val HANGUL_FINALS = arrayOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "k", "m", "p",
        "l", "l", "p", "l", "m", "p", "p", "t", "t", "ng", "t", "t",
        "k", "t", "p", "t"
    )

    private val KANA_DIGRAPHS = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja",  "じゅ" to "ju",  "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "ふぁ" to "fa",  "ふぃ" to "fi",  "ふぇ" to "fe",  "ふぉ" to "fo",
        "てぃ" to "ti",  "でぃ" to "di",  "とぅ" to "tu",  "どぅ" to "du",
        "うぃ" to "wi",  "うぇ" to "we",  "うぉ" to "wo",
        "しぇ" to "she", "ちぇ" to "che", "じぇ" to "je",
    )

    private val HIRAGANA_MONO = mapOf(
        'あ' to "a",  'い' to "i",  'う' to "u",  'え' to "e",  'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi",'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi",'つ' to "tsu",'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'ゐ' to "wi", 'ゑ' to "we", 'を' to "wo", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a",  'ぃ' to "i",  'ぅ' to "u",  'ぇ' to "e",  'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゎ' to "wa", 'ゔ' to "vu",
    )

    fun hasRomanizableScript(text: String): Boolean = text.any { ch ->
        ch.code in 0xAC00..0xD7A3 || ch.code in 0x3040..0x30FF
    }

    fun hasRomanizableLines(lines: List<LyricLine>): Boolean =
        lines.any { hasRomanizableScript(it.text) }

    /** Mengonversi katakana ke hiragana setara agar tabel konversi satu pintu. */
    private fun normalizeKana(ch: Char): Char {
        val code = ch.code
        return if (code in 0x30A1..0x30F6) (code - 0x60).toChar() else ch
    }

    /**
     * Mengembalikan teks romanisasi bila baris mengandung Hangul/Kana,
     * atau `null` bila baris sudah huruf Latin seluruhnya.
     */
    fun romanizeLine(line: String): String? {
        if (!hasRomanizableScript(line)) return null
        val sb = StringBuilder(line.length * 2)
        var i = 0
        var geminate = false

        while (i < line.length) {
            val ch = line[i]
            val code = ch.code

            // 1. Korea Hangul Syllable (AC00..D7A3)
            if (code in 0xAC00..0xD7A3) {
                val offset = code - 0xAC00
                val initial = offset / 588
                val medial = (offset % 588) / 28
                val fin = offset % 28
                sb.append(HANGUL_INITIALS[initial])
                sb.append(HANGUL_VOWELS[medial])
                sb.append(HANGUL_FINALS[fin])
                i++
                continue
            }

            // 2. Jepang Hiragana / Katakana
            val h1 = normalizeKana(ch)
            if (h1 == 'っ') {
                geminate = true
                i++
                continue
            }
            if (ch == 'ー') {
                if (sb.isNotEmpty() && sb.last() in "aiueo") {
                    sb.append(sb.last())
                }
                i++
                continue
            }
            if (i + 1 < line.length) {
                val h2 = normalizeKana(line[i + 1])
                val pair = "${h1}${h2}"
                val di = KANA_DIGRAPHS[pair]
                if (di != null) {
                    if (geminate && di.isNotEmpty()) {
                        sb.append(di[0])
                        geminate = false
                    }
                    sb.append(di)
                    i += 2
                    continue
                }
            }
            val mono = HIRAGANA_MONO[h1]
            if (mono != null) {
                if (geminate && mono.isNotEmpty()) {
                    sb.append(mono[0])
                    geminate = false
                }
                sb.append(mono)
                // Pisahkan kata Jepang halus bila setelah kana diikuti spasi/ASCII
                i++
                continue
            }

            geminate = false
            sb.append(ch)
            i++
        }

        val out = sb.toString().replace(Regex("\\s+"), " ").trim()
        return out.takeIf { it.isNotBlank() && !it.equals(line.trim(), ignoreCase = true) }
    }
}
