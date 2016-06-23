package com.sequenceiq.it.util

import java.io.IOException

import org.apache.commons.codec.binary.Base64
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.util.StreamUtils

object ResourceUtil {
    private val RAWDATA_START = 4

    @Throws(IOException::class)
    fun readStringFromResource(applicationContext: ApplicationContext, resourceLocation: String): String {
        if (resourceLocation.startsWith("raw:")) {
            return resourceLocation.substring(RAWDATA_START)
        } else {
            return String(readResource(applicationContext, resourceLocation))
        }
    }

    @Throws(IOException::class)
    fun readBase64EncodedContentFromResource(applicationContext: ApplicationContext, resourceLocation: String): String {
        if (resourceLocation.startsWith("raw:")) {
            return resourceLocation.substring(RAWDATA_START)
        } else {
            return Base64.encodeBase64String(readResource(applicationContext, resourceLocation))
        }
    }

    @Throws(IOException::class)
    fun readResource(applicationContext: ApplicationContext, resourceLocation: String): ByteArray {
        val resource = applicationContext.getResource(resourceLocation)
        return StreamUtils.copyToByteArray(resource.inputStream)
    }
}
