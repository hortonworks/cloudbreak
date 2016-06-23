package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.HashSet
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object FingerprintParserUtil {

    private val LOGGER = LoggerFactory.getLogger(FingerprintParserUtil::class.java)

    private val FINGERPRINT_PATTERNS = arrayOf(Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*ECDSA"), Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*RSA"))

    fun parseFingerprints(consoleLog: String): Set<String> {
        LOGGER.debug("Received console log: {}", consoleLog)
        val matchedFingerprints = HashSet<String>()
        val lines = consoleLog.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (line in lines) {
            for (pattern in FINGERPRINT_PATTERNS) {
                val m = pattern.matcher(line)
                if (m.find()) {
                    matchedFingerprints.add(m.group("fingerprint"))
                }
            }
        }
        return matchedFingerprints
    }
}
