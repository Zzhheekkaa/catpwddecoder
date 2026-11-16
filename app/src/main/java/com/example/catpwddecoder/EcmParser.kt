package com.example.catpwddecoder

object EcmParser {

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

    fun parseEngineSerial(xml: String): String? {
        val regex = Regex(
            "<Description>\\s*Engine Serial Number\\s*</Description>\\s*<Value>(.*?)</Value>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun parseEcmName(xml: String): String? {
        val regex = Regex(
            "<ECMName>(.*?)</ECMName>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
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
        return result
    }

    fun parseAll(xml: String): List<ConfigItem> {
        val items = mutableListOf<ConfigItem>()

        // 1. ECM Info
        val ecmInfoBlock = Regex(
            "<ECMInfo[^>]*>(.*?)</ECMInfo>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(xml)?.groupValues?.get(1)

        if (ecmInfoBlock != null) {
            val itemRegex = Regex(
                "<Item>(.*?)</Item>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            )
            for (m in itemRegex.findAll(ecmInfoBlock)) {
                val body = m.groupValues[1]
                val desc = extractTag(body, "Description") ?: continue
                if (desc.equals("Supported Device ID", ignoreCase = true)) continue
                val value = extractTag(body, "Value") ?: ""
                val displayDesc = when {
                    desc.equals("Personality Module Part Number", ignoreCase = true) -> "Flash Files"
                    desc.equals("Software Group Part Number", ignoreCase = true) -> "Flash Files"
                    else -> desc
                }
                items += ConfigItem("ECM Info", displayDesc, value)
            }
        }

        // 2. Configuration
        val configBlock = Regex(
            "<Config[^>]*>(.*?)</Config>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(xml)?.groupValues?.get(1)

        if (configBlock != null) {
            parseConfigBlock(configBlock, "Configuration", items)
        }

        // 3. Totals
        val totalsBlock = Regex(
            "<Totals[^>]*>(.*?)</Totals>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        ).find(xml)?.groupValues?.get(1)

        if (totalsBlock != null) {
            parseConfigBlock(totalsBlock, "Totals", items)
        }

        return items
    }

    private fun parseConfigBlock(block: String, section: String, out: MutableList<ConfigItem>) {
        val itemRegex = Regex(
            "<Item>(.*?)</Item>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        for (m in itemRegex.findAll(block)) {
            val body = m.groupValues[1]
            val desc = extractTag(body, "Description") ?: continue

            val realValue = extractTag(body, "RealValue")
            val valueEnglish = extractTag(body, "ValueEnglish")
            val valueMetric = extractTag(body, "ValueMetric")
            val stringIdDesc = extractTag(body, "StringIDDescription")
            val value = extractTag(body, "Value")

            val displayValue = when {
                realValue != null -> decodeRealValue(realValue)
                valueEnglish != null -> valueEnglish
                valueMetric != null -> valueMetric
                stringIdDesc != null -> stringIdDesc
                value != null -> value
                else -> ""
            }

            if (displayValue.isNotBlank() || desc.isNotBlank()) {
                out += ConfigItem(section, desc, displayValue)
            }
        }
    }

    private fun extractTag(body: String, tag: String): String? {
        val regex = Regex(
            "<$tag>(.*?)</$tag>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return regex.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
