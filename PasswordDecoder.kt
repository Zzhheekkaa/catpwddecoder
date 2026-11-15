package com.example.catpwddecoder

object PasswordDecoder {

    private val TABLE: Map<String, Char> = buildMap {
        put("b4", '1'); put("b7", '2'); put("b6", '3'); put("b1", '4'); put("b0", '5')
        put("b3", '6'); put("b2", '7'); put("bd", '8'); put("bc", '9'); put("b5", '0')
        put("e4", 'a'); put("e7", 'b'); put("e6", 'c'); put("e1", 'd'); put("e0", 'e')
        put("e3", 'f'); put("e2", 'g'); put("ed", 'h'); put("ec", 'i'); put("ef", 'j')
        put("ee", 'k'); put("e9", 'l'); put("e8", 'm'); put("eb", 'n'); put("ea", 'o')
        put("f5", 'p'); put("f4", 'q'); put("f7", 'r'); put("f6", 's'); put("f1", 't')
        put("f0", 'u'); put("f3", 'v'); put("f2", 'w'); put("fd", 'x'); put("fc", 'y'); put("ff", 'z')
        put("c4", 'A'); put("c7", 'B'); put("c6", 'C'); put("c1", 'D'); put("c0", 'E')
        put("c3", 'F'); put("c2", 'G'); put("cd", 'H'); put("cc", 'I'); put("cf", 'J')
        put("ce", 'K'); put("c9", 'L'); put("c8", 'M'); put("cb", 'N'); put("ca", 'O')
        put("d5", 'P'); put("d4", 'Q'); put("d7", 'R'); put("d6", 'S'); put("d1", 'T')
        put("d0", 'U'); put("d3", 'V'); put("d2", 'W'); put("dd", 'X'); put("dc", 'Y'); put("df", 'Z')
    }

    fun decodeRealValue(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 8 <= raw.length) {
            val hex = raw.substring(i + 6, i + 8).lowercase()
            if (hex == "a5") break
            TABLE[hex]?.let { sb.append(it) }
            i += 8
        }
        return sb.toString()
    }

    fun parsePasswords(xml: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val blockRegex = Regex(
            "Customer\\s*Password\\s*#\\s*(\\d).*?<RealValue>(.*?)</RealValue>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        for (m in blockRegex.findAll(xml)) {
            if (result.size >= 2) break
            result += "Customer Password #${m.groupValues[1]}" to
                    decodeRealValue(m.groupValues[2].trim())
        }
        if (result.isEmpty()) {
            val rvRegex = Regex("<RealValue>(.*?)</RealValue>", RegexOption.DOT_MATCHES_ALL)
            var idx = 1
            for (m in rvRegex.findAll(xml)) {
                if (idx > 2) break
                result += "Password #$idx" to decodeRealValue(m.groupValues[1].trim())
                idx++
            }
        }
        return result
    }
}