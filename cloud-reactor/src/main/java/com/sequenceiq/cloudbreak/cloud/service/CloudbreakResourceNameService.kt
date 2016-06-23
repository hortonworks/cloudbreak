package com.sequenceiq.cloudbreak.cloud.service

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.StringUtils

import com.google.common.base.Joiner
import com.google.common.base.Splitter


abstract class CloudbreakResourceNameService : ResourceNameService {

    protected fun checkArgs(argCnt: Int, vararg parts: Any) {
        if (null == parts && parts.size != argCnt) {
            throw IllegalStateException("No suitable name parts provided to generate resource name!")
        }
    }

    fun trimHash(part: String?): String {
        if (part == null) {
            throw IllegalStateException("Resource name part must not be null!")
        }
        LOGGER.debug("Trim the hash part from the end: {}", part)
        val parts = part.split(DELIMITER.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        var trimmed: String = part
        try {
            val ts = java.lang.Long.valueOf(parts[parts.size - 1])!!
            trimmed = StringUtils.collectionToDelimitedString(Arrays.asList(*Arrays.copyOf<String>(parts, parts.size - 1)), DELIMITER)
        } catch (nfe: NumberFormatException) {
            LOGGER.debug("No need to trim hash: {}", part)
        }

        return trimmed
    }

    fun adjustPartLength(part: String?): String {
        if (part == null) {
            throw IllegalStateException("Resource name part must not be null!")
        }
        var shortPart: String = part
        if (part.length > MAX_PART_LENGTH) {
            LOGGER.debug("Shortening part name: {}", part)
            shortPart = String(part.toCharArray(), 0, MAX_PART_LENGTH)
        } else {
            LOGGER.debug("Part name length OK, no need to shorten: {}", part)
        }
        return shortPart
    }

    protected fun adjustBaseLength(base: String?, platformSpecificLength: Int): String {
        if (base == null) {
            throw IllegalStateException("Name must not be null!")
        }
        if (base.length > platformSpecificLength) {
            LOGGER.debug("Shortening name: {}", base)
            val splitedBase = Splitter.on("-").splitToList(base)
            var stackName: String? = null
            var instanceName: String? = null
            if (splitedBase[0].length - (base.length - platformSpecificLength) > 1) {
                stackName = String(Splitter.fixedLength(splitedBase[0].length - (base.length - platformSpecificLength)).splitToList(splitedBase[0])[0])
                instanceName = splitedBase[1]
            } else {
                stackName = String(Splitter.fixedLength(1).splitToList(splitedBase[0])[0])
                instanceName = String(Splitter.fixedLength(splitedBase[1].length - (Math.abs(splitedBase[0].length - (base.length - platformSpecificLength)) + stackName.length)).splitToList(splitedBase[1])[0])
            }
            val shortBase = StringBuilder(stackName + DELIMITER + instanceName)
            for (i in 2..splitedBase.size - 1) {
                shortBase.append(DELIMITER).append(splitedBase[i])
            }
            return shortBase.toString()
        } else {
            LOGGER.debug("Name length OK, no need to shorten: {}", base)
            return base
        }
    }

    fun normalize(part: String?): String {
        if (part == null) {
            throw IllegalStateException("Resource name part must not be null!")
        }
        LOGGER.debug("Normalizing resource name part: {}", part)

        var normalized = StringUtils.trimAllWhitespace(part)
        LOGGER.debug("Trimmed whitespaces: {}", part)

        normalized = normalized.replace("[^a-zA-Z0-9]".toRegex(), "")
        LOGGER.debug("Trimmed invalid characters: {}", part)

        normalized = normalized.toLowerCase()
        LOGGER.debug("Lower case: {}", part)

        return normalized
    }

    protected fun appendPart(base: String?, part: String?): String {
        if (null == base && null == part) {
            throw IllegalArgumentException("base and part are both null! Can't append them!")
        }
        var sb: StringBuilder? = null
        if (null != base) {
            sb = StringBuilder(base).append(DELIMITER).append(part)
        } else {
            sb = StringBuilder(part)
        }
        return sb!!.toString()
    }

    protected fun appendHash(name: String, timestamp: Date): String {
        val df = SimpleDateFormat(DATE_FORMAT)
        return Joiner.on("").join(name, DELIMITER, df.format(timestamp))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudbreakResourceNameService::class.java)
        private val DELIMITER = "-"
        private val MAX_PART_LENGTH = 20
        private val DATE_FORMAT = "yyyyMMddHHmmss"
    }
}


