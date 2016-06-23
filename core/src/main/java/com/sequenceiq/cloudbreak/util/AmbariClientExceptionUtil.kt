package com.sequenceiq.cloudbreak.util

import java.io.StringReader

import org.apache.commons.io.IOUtils

import groovyx.net.http.HttpResponseException

object AmbariClientExceptionUtil {

    fun getErrorMessage(e: HttpResponseException): String {
        try {
            val json = IOUtils.toString(e.response.data as StringReader)
            return JsonUtil.readTree(json).get("message").asText()
        } catch (ex: Exception) {
            return "Could not get error cause from exception of Ambari client: " + e.toString()
        }

    }

}
