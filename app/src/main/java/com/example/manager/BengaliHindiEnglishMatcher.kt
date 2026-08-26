package com.example.manager

import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Phonetic normalizer and cross-script matcher for Indian languages (Bengali, Hindi, Assamese,
 * Gujarati, Kannada, Malayalam, Marathi, Odia, Punjabi, Tamil, Telugu, Urdu) and English.
 */
object BengaliHindiEnglishMatcher {

    /**
     * Transliterates Indic scripts (Bengali, Assamese, Devanagari, Gujarati, Gurmukhi,
     * Odia, Tamil, Telugu, Kannada, Malayalam) and Urdu to Latin phonetic characters.
     */
    fun transliterateToLatin(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            val code = ch.code

            val mapped: String? = when {
                // Bengali & Assamese (0x0980 - 0x09FF)
                code in 0x0985..0x098C -> when (code) {
                    0x0985, 0x0986 -> "a"
                    0x0987, 0x0988 -> "i"
                    0x0989, 0x098A -> "u"
                    0x098B, 0x098C -> "ri"
                    else -> "a"
                }
                code in 0x098F..0x0994 -> when (code) {
                    0x098F -> "e"
                    0x0990 -> "ai"
                    0x0993 -> "o"
                    0x0994 -> "au"
                    else -> "e"
                }
                code in 0x09BE..0x09CC -> when (code) {
                    0x09BE -> "a"
                    0x09BF, 0x09C0 -> "i"
                    0x09C1, 0x09C2 -> "u"
                    0x09C3, 0x09C4 -> "ri"
                    0x09C7 -> "e"
                    0x09C8 -> "ai"
                    0x09CB -> "o"
                    0x09CC -> "au"
                    else -> ""
                }
                code in 0x0995..0x09B9 -> when (code) {
                    0x0995 -> "k"
                    0x0996 -> "kh"
                    0x0997 -> "g"
                    0x0998 -> "gh"
                    0x0999 -> "ng"
                    0x099A -> "ch"
                    0x099B -> "chh"
                    0x099C -> "j"
                    0x099D -> "jh"
                    0x099E -> "n"
                    0x099F, 0x09A4 -> "t"
                    0x09A0, 0x09A5 -> "th"
                    0x09A1, 0x09A6 -> "d"
                    0x09A2, 0x09A7 -> "dh"
                    0x09A3, 0x09A8 -> "n"
                    0x09AA -> "p"
                    0x09AB -> "f"
                    0x09AC -> "b"
                    0x09AD -> "bh"
                    0x09AE -> "m"
                    0x09AF -> "j"
                    0x09B0 -> "r"
                    0x09B2 -> "l"
                    0x09B6, 0x09B7 -> "sh"
                    0x09B8 -> "s"
                    0x09B9 -> "h"
                    else -> ""
                }
                code == 0x09CE -> "t"
                code == 0x09DC -> "r"
                code == 0x09DD -> "rh"
                code == 0x09DF -> "y"
                code == 0x09F0 -> "r" // Assamese Ra
                code == 0x09F1 -> "w" // Assamese Wa
                code == 0x0981 || code == 0x0982 -> "n"
                code == 0x0983 -> "h"
                code == 0x09CD || code == 0x09BC -> ""

                // Devanagari (0x0900 - 0x097F)
                code in 0x0904..0x090C -> when (code) {
                    0x0904, 0x0905, 0x0906 -> "a"
                    0x0907, 0x0908 -> "i"
                    0x0909, 0x090A -> "u"
                    0x090B, 0x090C -> "ri"
                    else -> "a"
                }
                code in 0x090F..0x0914 -> when (code) {
                    0x090F, 0x0910 -> "e"
                    0x0911, 0x0912, 0x0913 -> "o"
                    0x0914 -> "au"
                    else -> "e"
                }
                code in 0x093E..0x094C -> when (code) {
                    0x093E -> "a"
                    0x093F, 0x0940 -> "i"
                    0x0941, 0x0942 -> "u"
                    0x0943, 0x0944 -> "ri"
                    0x0947 -> "e"
                    0x0948 -> "ai"
                    0x094B -> "o"
                    0x094C -> "au"
                    else -> ""
                }
                code in 0x0915..0x0939 -> when (code) {
                    0x0915 -> "k"
                    0x0916 -> "kh"
                    0x0917 -> "g"
                    0x0918 -> "gh"
                    0x0919 -> "ng"
                    0x091A -> "ch"
                    0x091B -> "chh"
                    0x091C -> "j"
                    0x091D -> "jh"
                    0x091E -> "n"
                    0x091F, 0x0924 -> "t"
                    0x0920, 0x0925 -> "th"
                    0x0921, 0x0926 -> "d"
                    0x0922, 0x0927 -> "dh"
                    0x0923, 0x0928 -> "n"
                    0x092A -> "p"
                    0x092B -> "f"
                    0x092C -> "b"
                    0x092D -> "bh"
                    0x092E -> "m"
                    0x092F -> "y"
                    0x0930 -> "r"
                    0x0931 -> "r"
                    0x0932, 0x0933, 0x0934 -> "l"
                    0x0935 -> "v"
                    0x0936, 0x0937 -> "sh"
                    0x0938 -> "s"
                    0x0939 -> "h"
                    else -> ""
                }
                code in 0x0958..0x095F -> when (code) {
                    0x0958 -> "q"
                    0x0959 -> "kh"
                    0x095A -> "g"
                    0x095B -> "z"
                    0x095C -> "r"
                    0x095D -> "rh"
                    0x095E -> "f"
                    0x095F -> "y"
                    else -> ""
                }
                code == 0x0901 || code == 0x0902 -> "n"
                code == 0x0903 -> "h"
                code == 0x094D || code == 0x093C -> ""

                // Gujarati (0x0A80 - 0x0AFF)
                code in 0x0A85..0x0A94 -> when (code) {
                    0x0A85, 0x0A86 -> "a"
                    0x0A87, 0x0A88 -> "i"
                    0x0A89, 0x0A8A -> "u"
                    0x0A8F, 0x0A90 -> "e"
                    0x0A93, 0x0A94 -> "o"
                    else -> "a"
                }
                code in 0x0ABE..0x0ACC -> when (code) {
                    0x0ABE -> "a"
                    0x0ABF, 0x0AC0 -> "i"
                    0x0AC1, 0x0AC2 -> "u"
                    0x0AC7, 0x0AC8 -> "e"
                    0x0ACB, 0x0ACC -> "o"
                    else -> ""
                }
                code in 0x0A95..0x0AB9 -> when (code) {
                    0x0A95 -> "k"
                    0x0A96 -> "kh"
                    0x0A97 -> "g"
                    0x0A98 -> "gh"
                    0x0A9A -> "ch"
                    0x0A9B -> "chh"
                    0x0A9C -> "j"
                    0x0A9D -> "jh"
                    0x0A9F, 0x0AA4 -> "t"
                    0x0AA0, 0x0AA5 -> "th"
                    0x0AA1, 0x0AA6 -> "d"
                    0x0AA2, 0x0AA7 -> "dh"
                    0x0AA3, 0x0AA8 -> "n"
                    0x0AAA -> "p"
                    0x0AAB -> "f"
                    0x0AAC -> "b"
                    0x0AAD -> "bh"
                    0x0AAE -> "m"
                    0x0AAF -> "y"
                    0x0AB0 -> "r"
                    0x0AB2, 0x0AB3 -> "l"
                    0x0AB5 -> "v"
                    0x0AB6, 0x0AB7 -> "sh"
                    0x0AB8 -> "s"
                    0x0AB9 -> "h"
                    else -> ""
                }
                code == 0x0A81 || code == 0x0A82 -> "n"
                code == 0x0ACD -> ""

                // Gurmukhi / Punjabi (0x0A00 - 0x0A7F)
                code in 0x0A05..0x0A14 -> when (code) {
                    0x0A05, 0x0A06 -> "a"
                    0x0A07, 0x0A08 -> "i"
                    0x0A09, 0x0A0A -> "u"
                    0x0A0F, 0x0A10 -> "e"
                    0x0A13, 0x0A14 -> "o"
                    else -> "a"
                }
                code in 0x0A3E..0x0A4C -> when (code) {
                    0x0A3E -> "a"
                    0x0A3F, 0x0A40 -> "i"
                    0x0A41, 0x0A42 -> "u"
                    0x0A47, 0x0A48 -> "e"
                    0x0A4B, 0x0A4C -> "o"
                    else -> ""
                }
                code in 0x0A15..0x0A39 -> when (code) {
                    0x0A15 -> "k"
                    0x0A16 -> "kh"
                    0x0A17 -> "g"
                    0x0A18 -> "gh"
                    0x0A1A -> "ch"
                    0x0A1B -> "chh"
                    0x0A1C -> "j"
                    0x0A1D -> "jh"
                    0x0A1F, 0x0A24 -> "t"
                    0x0A20, 0x0A25 -> "th"
                    0x0A21, 0x0A26 -> "d"
                    0x0A22, 0x0A27 -> "dh"
                    0x0A23, 0x0A28 -> "n"
                    0x0A2A -> "p"
                    0x0A2B -> "f"
                    0x0A2C -> "b"
                    0x0A2D -> "bh"
                    0x0A2E -> "m"
                    0x0A2F -> "y"
                    0x0A30 -> "r"
                    0x0A32, 0x0A33 -> "l"
                    0x0A35 -> "v"
                    0x0A36 -> "sh"
                    0x0A38 -> "s"
                    0x0A39 -> "h"
                    else -> ""
                }
                code in 0x0A59..0x0A5E -> when (code) {
                    0x0A59 -> "kh"
                    0x0A5A -> "g"
                    0x0A5B -> "z"
                    0x0A5C -> "r"
                    0x0A5E -> "f"
                    else -> ""
                }
                code == 0x0A01 || code == 0x0A02 || code == 0x0A70 || code == 0x0A71 -> "n"
                code == 0x0A4D || code == 0x0A3C -> ""

                // Odia (0x0B00 - 0x0B7F)
                code in 0x0B05..0x0B14 -> when (code) {
                    0x0B05, 0x0B06 -> "a"
                    0x0B07, 0x0B08 -> "i"
                    0x0B09, 0x0B0A -> "u"
                    0x0B0F, 0x0B10 -> "e"
                    0x0B13, 0x0B14 -> "o"
                    else -> "a"
                }
                code in 0x0B3E..0x0B4C -> when (code) {
                    0x0B3E -> "a"
                    0x0B3F, 0x0B40 -> "i"
                    0x0B41, 0x0B42 -> "u"
                    0x0B47, 0x0B48 -> "e"
                    0x0B4B, 0x0B4C -> "o"
                    else -> ""
                }
                code in 0x0B15..0x0B39 -> when (code) {
                    0x0B15 -> "k"
                    0x0B16 -> "kh"
                    0x0B17 -> "g"
                    0x0B18 -> "gh"
                    0x0B1A -> "ch"
                    0x0B1B -> "chh"
                    0x0B1C -> "j"
                    0x0B1D -> "jh"
                    0x0B1F, 0x0B24 -> "t"
                    0x0B20, 0x0B25 -> "th"
                    0x0B21, 0x0B26 -> "d"
                    0x0B22, 0x0B27 -> "dh"
                    0x0B23, 0x0B28 -> "n"
                    0x0B2A -> "p"
                    0x0B2B -> "f"
                    0x0B2C -> "b"
                    0x0B2D -> "bh"
                    0x0B2E -> "m"
                    0x0B2F, 0x0B5F -> "y"
                    0x0B30 -> "r"
                    0x0B32, 0x0B33 -> "l"
                    0x0B36, 0x0B37 -> "sh"
                    0x0B38 -> "s"
                    0x0B39 -> "h"
                    else -> ""
                }
                code in 0x0B5C..0x0B5D -> "r"
                code == 0x0B01 || code == 0x0B02 -> "n"
                code == 0x0B4D || code == 0x0B3C -> ""

                // Tamil (0x0B80 - 0x0BFF)
                code in 0x0B85..0x0B94 -> when (code) {
                    0x0B85, 0x0B86 -> "a"
                    0x0B87, 0x0B88 -> "i"
                    0x0B89, 0x0B8A -> "u"
                    0x0B8E, 0x0B8F, 0x0B90 -> "e"
                    0x0B92, 0x0B93, 0x0B94 -> "o"
                    else -> "a"
                }
                code in 0x0BBE..0x0BCC -> when (code) {
                    0x0BBE -> "a"
                    0x0BBF, 0x0BC0 -> "i"
                    0x0BC1, 0x0BC2 -> "u"
                    0x0BC6, 0x0BC7, 0x0BC8 -> "e"
                    0x0BCA, 0x0BCB, 0x0BCC -> "o"
                    else -> ""
                }
                code in 0x0B95..0x0BB9 -> when (code) {
                    0x0B95 -> "k"
                    0x0B99 -> "ng"
                    0x0B9A -> "s"
                    0x0B9C -> "j"
                    0x0B9E -> "n"
                    0x0B9F -> "t"
                    0x0BA3, 0x0BA8, 0x0BA9 -> "n"
                    0x0BA4 -> "th"
                    0x0BAA -> "p"
                    0x0BAE -> "m"
                    0x0BAF -> "y"
                    0x0BB0, 0x0BB1 -> "r"
                    0x0BB2, 0x0BB4, 0x0BB5, 0x0BB6 -> when (code) {
                        0x0BB2 -> "l"
                        0x0BB4 -> "zh"
                        0x0BB5 -> "v"
                        0x0BB6, 0x0BB7 -> "sh"
                        else -> ""
                    }
                    0x0BB8 -> "s"
                    0x0BB9 -> "h"
                    else -> ""
                }
                code == 0x0BCD -> ""

                // Telugu (0x0C00 - 0x0C7F)
                code in 0x0C05..0x0C14 -> when (code) {
                    0x0C05, 0x0C06 -> "a"
                    0x0C07, 0x0C08 -> "i"
                    0x0C09, 0x0C0A -> "u"
                    0x0C0E, 0x0C0F, 0x0C10 -> "e"
                    0x0C12, 0x0C13, 0x0C14 -> "o"
                    else -> "a"
                }
                code in 0x0C3E..0x0C4C -> when (code) {
                    0x0C3E -> "a"
                    0x0C3F, 0x0C40 -> "i"
                    0x0C41, 0x0C42 -> "u"
                    0x0C46, 0x0C47, 0x0C48 -> "e"
                    0x0C4A, 0x0C4B, 0x0C4C -> "o"
                    else -> ""
                }
                code in 0x0C15..0x0C39 -> when (code) {
                    0x0C15 -> "k"
                    0x0C16 -> "kh"
                    0x0C17 -> "g"
                    0x0C18 -> "gh"
                    0x0C1A -> "ch"
                    0x0C1B -> "chh"
                    0x0C1C -> "j"
                    0x0C1D -> "jh"
                    0x0C1F, 0x0C24 -> "t"
                    0x0C20, 0x0C25 -> "th"
                    0x0C21, 0x0C26 -> "d"
                    0x0C22, 0x0C27 -> "dh"
                    0x0C23, 0x0C28 -> "n"
                    0x0C2A -> "p"
                    0x0C2B -> "f"
                    0x0C2C -> "b"
                    0x0C2D -> "bh"
                    0x0C2E -> "m"
                    0x0C2F -> "y"
                    0x0C30, 0x0C31 -> "r"
                    0x0C32, 0x0C33 -> "l"
                    0x0C35 -> "v"
                    0x0C36, 0x0C37 -> "sh"
                    0x0C38 -> "s"
                    0x0C39 -> "h"
                    else -> ""
                }
                code == 0x0C01 || code == 0x0C02 -> "n"
                code == 0x0C4D -> ""

                // Kannada (0x0C80 - 0x0CFF)
                code in 0x0C85..0x0C94 -> when (code) {
                    0x0C85, 0x0C86 -> "a"
                    0x0C87, 0x0C88 -> "i"
                    0x0C89, 0x0C8A -> "u"
                    0x0C8E, 0x0C8F, 0x0C90 -> "e"
                    0x0C92, 0x0C93, 0x0C94 -> "o"
                    else -> "a"
                }
                code in 0x0CBE..0x0CCC -> when (code) {
                    0x0CBE -> "a"
                    0x0CBF, 0x0CC0 -> "i"
                    0x0CC1, 0x0CC2 -> "u"
                    0x0CC6, 0x0CC7, 0x0CC8 -> "e"
                    0x0CCA, 0x0CCB, 0x0CCC -> "o"
                    else -> ""
                }
                code in 0x0C95..0x0CB9 -> when (code) {
                    0x0C95 -> "k"
                    0x0C96 -> "kh"
                    0x0C97 -> "g"
                    0x0C98 -> "gh"
                    0x0C9A -> "ch"
                    0x0C9B -> "chh"
                    0x0C9C -> "j"
                    0x0C9D -> "jh"
                    0x0C9F, 0x0CA4 -> "t"
                    0x0CA0, 0x0CA5 -> "th"
                    0x0CA1, 0x0CA6 -> "d"
                    0x0CA2, 0x0CA7 -> "dh"
                    0x0CA3, 0x0CA8 -> "n"
                    0x0CAA -> "p"
                    0x0CAB -> "f"
                    0x0CAC -> "b"
                    0x0CAD -> "bh"
                    0x0CAE -> "m"
                    0x0CAF -> "y"
                    0x0CB0, 0x0CB1 -> "r"
                    0x0CB2, 0x0CB3 -> "l"
                    0x0CB5 -> "v"
                    0x0CB6, 0x0CB7 -> "sh"
                    0x0CB8 -> "s"
                    0x0CB9 -> "h"
                    else -> ""
                }
                code == 0x0C82 -> "n"
                code == 0x0CCD -> ""

                // Malayalam (0x0D00 - 0x0D7F)
                code in 0x0D05..0x0D14 -> when (code) {
                    0x0D05, 0x0D06 -> "a"
                    0x0D07, 0x0D08 -> "i"
                    0x0D09, 0x0D0A -> "u"
                    0x0D0E, 0x0D0F, 0x0D10 -> "e"
                    0x0D12, 0x0D13, 0x0D14 -> "o"
                    else -> "a"
                }
                code in 0x0D3E..0x0D4C -> when (code) {
                    0x0D3E -> "a"
                    0x0D3F, 0x0D40 -> "i"
                    0x0D41, 0x0D42 -> "u"
                    0x0D46, 0x0D47, 0x0D48 -> "e"
                    0x0D4A, 0x0D4B, 0x0D4C -> "o"
                    else -> ""
                }
                code in 0x0D15..0x0D39 -> when (code) {
                    0x0D15 -> "k"
                    0x0D16 -> "kh"
                    0x0D17 -> "g"
                    0x0D18 -> "gh"
                    0x0D1A -> "ch"
                    0x0D1B -> "chh"
                    0x0D1C -> "j"
                    0x0D1D -> "jh"
                    0x0D1F, 0x0D24 -> "t"
                    0x0D20, 0x0D25 -> "th"
                    0x0D21, 0x0D26 -> "d"
                    0x0D22, 0x0D27 -> "dh"
                    0x0D23, 0x0D28 -> "n"
                    0x0D2A -> "p"
                    0x0D2B -> "f"
                    0x0D2C -> "b"
                    0x0D2D -> "bh"
                    0x0D2E -> "m"
                    0x0D2F -> "y"
                    0x0D30, 0x0D31 -> "r"
                    0x0D32, 0x0D33 -> "l"
                    0x0D34 -> "zh"
                    0x0D35 -> "v"
                    0x0D36, 0x0D37 -> "sh"
                    0x0D38 -> "s"
                    0x0D39 -> "h"
                    else -> ""
                }
                code in 0x0D7A..0x0D7F -> when (code) {
                    0x0D7A, 0x0D7B -> "n"
                    0x0D7C -> "r"
                    0x0D7D, 0x0D7E -> "l"
                    0x0D7F -> "k"
                    else -> ""
                }
                code == 0x0D02 -> "n"
                code == 0x0D4D -> ""

                // Urdu / Arabic script (0x0600 - 0x06FF)
                code in 0x0621..0x064A || code in 0x0679..0x06D2 -> when (code) {
                    0x0627, 0x0622 -> "a"
                    0x0628 -> "b"
                    0x067E -> "p"
                    0x062A, 0x0679, 0x0637 -> "t"
                    0x062B, 0x0633, 0x0635 -> "s"
                    0x062C -> "j"
                    0x0686 -> "ch"
                    0x062D, 0x06C1, 0x06C2, 0x06D5 -> "h"
                    0x062E -> "kh"
                    0x062F, 0x0688 -> "d"
                    0x0630, 0x0632, 0x0636, 0x0638 -> "z"
                    0x0631, 0x0691 -> "r"
                    0x0698, 0x0634 -> "sh"
                    0x0639 -> "a"
                    0x063A -> "gh"
                    0x0641 -> "f"
                    0x0642, 0x06A9 -> "k"
                    0x06AF -> "g"
                    0x0644 -> "l"
                    0x0645 -> "m"
                    0x0646, 0x06BA -> "n"
                    0x0648 -> "v"
                    0x064A, 0x06CC -> "i"
                    0x06D2 -> "e"
                    else -> ""
                }

                else -> null
            }

            if (mapped != null) {
                sb.append(mapped)
            } else {
                sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Phonetic normalization:
     * - Convert to lowercase
     * - Transliterate Indic scripts to Latin
     * - Normalize common equivalent sounds (b/v, s/sh, ee/i, oo/u, ph/f, k/c, etc.)
     * - Strip non-alphanumeric noise
     * - Collapse duplicate characters
     */
    fun normalizePhonetic(raw: String): String {
        if (raw.isBlank()) return ""

        val transliterated = transliterateToLatin(raw.lowercase(Locale.ROOT))

        // Replace digraphs and equivalent phonemes across Indian transliterations
        var cleaned = transliterated
            .replace("bh", "b")
            .replace("dh", "d")
            .replace("th", "t")
            .replace("kh", "k")
            .replace("gh", "g")
            .replace("ch", "c")
            .replace("jh", "j")
            .replace("sh", "s")
            .replace("ph", "f")
            .replace("zh", "l") // Tamil/Malayalam 'zh' sound
            .replace("ee", "i")
            .replace("ea", "i")
            .replace("oo", "u")
            .replace("ou", "u")
            .replace("ai", "e")
            .replace("ay", "e")
            .replace("ck", "k")
            .replace("c", "k")
            .replace("w", "v")
            .replace("b", "v") // Indian b <-> v interchangeability (e.g. Vinod / Binod, Bikash / Vikas, Bijay / Vijay)
            .replace("z", "j")
            .replace("x", "ks")
            .replace("q", "k")

        // Retain only ASCII lowercase letters and digits
        cleaned = cleaned.filter { it in 'a'..'z' || it in '0'..'9' }

        // Collapse duplicate consecutive characters (e.g. "roooom" -> "rom", "mann" -> "man")
        val collapsed = StringBuilder()
        for (idx in cleaned.indices) {
            if (idx == 0 || cleaned[idx] != cleaned[idx - 1]) {
                collapsed.append(cleaned[idx])
            }
        }

        return collapsed.toString()
    }

    /**
     * Strips filler words, command keywords, and postpositions across all supported Indian languages and English.
     */
    fun cleanContactQuery(raw: String): String {
        var text = raw.trim()

        // Leading conversational fillers / sequence words
        text = text.replace(Regex("(?i)^(?:please|first|firstly|can\\s+you\\s+call|could\\s+you\\s+call|pehle|pehla|ekto|ekbar)\\s+"), "")
        text = text.replace(Regex("^(?:দয়া\\s+করে|দয়া\\s+করে|দয়া\\s+কৰি|একটু|একবার|প্রথমে|প্রথমেই|পহলে|पहले|पहला|कृपया|जरा|एक\\s+बार)\\s+"), "")

        // English command prefixes/suffixes
        text = text.replace(Regex("(?i)^(?:please\\s+)?(?:make\\s+a\\s+)?(?:call|phone|dial|ring)\\s+(?:to\\s+)?"), "")
        text = text.replace(Regex("(?i)\\s+(?:please|now)$"), "")

        // Mixed/Cross-language verb suffixes and keywords (e.g. "call koro", "call karo", "phone lagao", "call dao")
        text = text.replace(Regex("(?i)\\s+(?:call|phone|dial|ring)\\s*(?:koro|karo|lagao|daao|dao|kora|kori)?$"), "")
        text = text.replace(Regex("(?i)^(?:call|phone|dial|ring)\\s*(?:koro|karo|lagao|daao|dao|kora|kori)?\\s+"), "")

        // Bengali & Assamese verb words & prefixes
        text = text.replace(Regex("^(?:কল|ফোন|ডায়াল)\\s*(?:করো|কর|করুন|কৰক|কৰা|লাগাও|দাও)\\s*"), "")
        text = text.replace(Regex("\\s*(?:কল|ফোন|ডায়াল)\\s*(?:করো|কর|করুন|কৰক|কৰা|লাগাও|দাও)$"), "")

        // Hindi & Marathi verb words & prefixes
        text = text.replace(Regex("^(?:कॉल|फोन|डायल)\\s*(?:करो|करें|कीजिए|लगाओ|मिलाओ|करा|लावा|कर)\\s*"), "")
        text = text.replace(Regex("\\s*(?:कॉल|फोन|डायल)\\s*(?:करो|करें|कीजिए|लगाओ|मिलाओ|करा|लावा|कर)$"), "")

        // Gujarati verb words & prefixes
        text = text.replace(Regex("^(?:કોલ|ફોન)\\s*(?:કરો|લગાવો)\\s*"), "")
        text = text.replace(Regex("\\s*(?:કોલ|ફોન)\\s*(?:કરો|લગાવો)$"), "")

        // Punjabi verb words & prefixes
        text = text.replace(Regex("^(?:ਕਾਲ|ਫ਼ੋਨ)\\s*(?:ਕਰੋ|ਲਗਾਓ)\\s*"), "")
        text = text.replace(Regex("\\s*(?:ਕਾਲ|ਫ਼ੋਨ)\\s*(?:ਕਰੋ|ਲਗਾਓ)$"), "")

        // Odia verb words & prefixes
        text = text.replace(Regex("^(?:କଲ୍|ଫୋନ୍)\\s*(?:କରନ୍ତୁ|କର)\\s*"), "")
        text = text.replace(Regex("\\s*(?:କଲ୍|ଫୋନ୍)\\s*(?:କରନ୍ତୁ|କର)$"), "")

        // Tamil verb words & prefixes
        text = text.replace(Regex("^(?:போன்|கால்)\\s*(?:செய்|பண்ணு|பண்ணுங்க)\\s*"), "")
        text = text.replace(Regex("\\s*(?:போன்|கால்)\\s*(?:செய்|பண்ணு|பண்ணுங்க)$"), "")

        // Telugu verb words & prefixes
        text = text.replace(Regex("^(?:కాల్|ఫోన్)\\s*(?:చేయి|చెయ్యి|చేయండి)\\s*"), "")
        text = text.replace(Regex("\\s*(?:కాల్|ఫోన్)\\s*(?:చేయి|చెయ్యి|చేయండి)$"), "")

        // Kannada verb words & prefixes
        text = text.replace(Regex("^(?:ಕರೆ|ಕಾಲ್|ಫೋನ್)\\s*(?:ಮಾಡಿ|ಮಾಡು)\\s*"), "")
        text = text.replace(Regex("\\s*(?:ಕರೆ|ಕಾಲ್|ಫೋನ್)\\s*(?:ಮಾಡಿ|ಮಾಡು)$"), "")

        // Malayalam verb words & prefixes
        text = text.replace(Regex("^(?:വിളിക്കൂ|കോൾ ചെയ്യുക|ഫോൺ ചെയ്യുക)\\s*"), "")
        text = text.replace(Regex("\\s*(?:വിളിക്കൂ|കോൾ ചെയ്യുക|ഫോൺ ചെയ്യുക)$"), "")

        // Urdu verb words & prefixes
        text = text.replace(Regex("^(?:کال|فون)\\s*(?:کرو|کریں|لگائیں)\\s*"), "")
        text = text.replace(Regex("\\s*(?:کال|فون)\\s*(?:کرو|کریں|لگائیں)$"), "")

        // Strip standalone postpositions (both Indic and Romanized)
        text = text.replace(Regex("(?i)\\s+(?:ko|ke|er|re|ra|ku|ki)$"), "")
        text = text.replace(Regex("\\s+(?:को|ला|यांना|ने|ਨੂੰ|ଙ୍କୁ|କୁ|కి|కు|ಅವರಿಗೆ|ಗೆ|ന്|நெ|க|লৈ|কো)$"), "")

        // Strip attached postposition suffixes
        text = text.replace(Regex("லுக்கு$"), "ல்")
        text = text.replace(Regex("ருக்கு$"), "ர்")
        text = text.replace(Regex("னுக்கு$"), "ன்")
        text = text.replace(Regex("(?:ுக்கு|க்கு|உக்கு)$"), "")
        text = text.replace(Regex("(?:\\s*কে|\\s*ের|\\s*েরকে|\\s*রে|\\s*র)$"), "")
        text = text.replace(Regex("(?:\\s*को|\\s*ला|\\s*यांना)$"), "")
        text = text.replace(Regex("(?:\\s*ने)$"), "")
        text = text.replace(Regex("(?:\\s*ਨੂੰ)$"), "")
        text = text.replace(Regex("(?:\\s*ଙ୍କୁ|\\s*କୁ)$"), "")
        text = text.replace(Regex("(?:\\s*కి|\\s*కు)$"), "")
        text = text.replace(Regex("(?:\\s*ಅವರಿಗೆ|\\s*ರಿಗೆ|\\s*ಿಗೆ|\\s*ಗೆ)$"), "")
        text = text.replace(Regex("(?:\\s*ിനെ|\\s*നെ|\\s*ിന്|\\s*ന്)$"), "")
        text = text.replace(Regex("(?:\\s*লৈ|\\s*ক)$"), "")
        text = text.replace(Regex("(?:\\s*کو)$"), "")

        return text.trim()
    }

    /**
     * Calculates phonetic similarity score between query and contact name (0.0 to 1.0).
     */
    fun calculateMatchScore(query: String, contactName: String): Double = computeMatchScore(query, contactName)

    /**
     * Computes phonetic similarity score between query and contact name (0.0 to 1.0).
     */
    fun computeMatchScore(query: String, contactName: String): Double {
        val rawQ = query.trim().lowercase(Locale.ROOT)
        val rawN = contactName.trim().lowercase(Locale.ROOT)

        if (rawQ.isEmpty() || rawN.isEmpty()) return 0.0

        // 1. Exact raw match
        if (rawQ == rawN) return 1.0

        // 2. Transliterated raw match
        val transQ = transliterateToLatin(rawQ)
        val transN = transliterateToLatin(rawN)
        if (transQ.equals(transN, ignoreCase = true)) return 0.98

        // 3. Phonetically normalized match
        val normQ = normalizePhonetic(rawQ)
        val normN = normalizePhonetic(rawN)

        if (normQ.isEmpty() || normN.isEmpty()) return 0.0
        if (normQ == normN) return 0.95

        // 4. Token-level matching (e.g. "Vinod" matches "Vinod Kumar" or "Binod Das")
        val contactTokens = rawN.split(Regex("\\s+")).map { normalizePhonetic(it) }
        for (token in contactTokens) {
            if (token == normQ) return 0.92
            if (token.startsWith(normQ) || normQ.startsWith(token)) return 0.85
        }

        // 5. Consonant skeleton match (handles unwritten inherent vowels in Indic script)
        val consQ = normQ.replace(Regex("[aeiou]"), "")
        val consN = normN.replace(Regex("[aeiou]"), "")
        if (consQ.isNotEmpty() && consQ.length >= 2 && consQ == consN) return 0.90

        // 6. Prefix or Substring check
        if (normN.startsWith(normQ) || normQ.startsWith(normN)) return 0.88
        if (normN.contains(normQ) || normQ.contains(normN)) return 0.80

        // 7. Levenshtein edit distance on phonetically normalized strings
        val distance = levenshteinDistance(normQ, normN)
        val maxLength = max(normQ.length, normN.length)
        if (maxLength == 0) return 0.0

        val similarity = 1.0 - (distance.toDouble() / maxLength.toDouble())
        return if (similarity >= 0.70) similarity else 0.0
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
