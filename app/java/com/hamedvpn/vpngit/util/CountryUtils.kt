package com.hamedvpn.vpngit.util

import com.hamedvpn.vpngit.handler.SettingsManager
import java.util.Locale

object CountryUtils {

    private const val REGIONAL_INDICATOR_START = 0x1F1E6
    private const val REGIONAL_INDICATOR_END = 0x1F1FF

    
    const val UNKNOWN_FLAG = "🌐"

    
    fun extractFlagEmoji(text: String?): String? {
        if (text.isNullOrEmpty()) return null
        val codePoints = text.codePoints().toArray()
        for (i in 0 until codePoints.size - 1) {
            val a = codePoints[i]
            val b = codePoints[i + 1]
            if (a in REGIONAL_INDICATOR_START..REGIONAL_INDICATOR_END &&
                b in REGIONAL_INDICATOR_START..REGIONAL_INDICATOR_END
            ) {
                return buildString {
                    appendCodePoint(a)
                    appendCodePoint(b)
                }
            }
        }
        return null
    }

    
    private fun flagToIsoCode(flag: String): String? {
        val codePoints = flag.codePoints().toArray()
        if (codePoints.size != 2) return null
        val first = codePoints[0] - REGIONAL_INDICATOR_START + 'A'.code
        val second = codePoints[1] - REGIONAL_INDICATOR_START + 'A'.code
        if (first !in 'A'.code..'Z'.code || second !in 'A'.code..'Z'.code) return null
        return "${first.toChar()}${second.toChar()}"
    }

    
    fun countryFromRemarks(remarks: String?): Pair<String?, String?> {
        val flag = extractFlagEmoji(remarks) ?: return null to null
        val iso = flagToIsoCode(flag) ?: return flag to null
        val name = Locale("", iso).getDisplayCountry(SettingsManager.getLocale())
        return if (name.isNotBlank() && !name.equals(iso, ignoreCase = true)) {
            flag to name
        } else {
            flag to null
        }
    }
}

