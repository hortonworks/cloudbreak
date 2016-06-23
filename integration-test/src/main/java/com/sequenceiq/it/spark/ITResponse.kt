package com.sequenceiq.it.spark

import java.io.IOException
import java.io.InputStream

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.fasterxml.jackson.databind.ObjectMapper

import spark.Route

abstract class ITResponse : Route {

    val objectMapper = ObjectMapper()

    companion object {

        val DOCKER_API_ROOT = "/docker/v1.18"
        val SWARM_API_ROOT = "/swarm/v1.18"
        val CONSUL_API_ROOT = "/v1"
        val AMBARI_API_ROOT = "/api/v1"
        val MOCK_ROOT = "/spi"
        val SALT_API_ROOT = "/saltapi"
        val SALT_BOOT_ROOT = "/saltboot"
        private val LOGGER = LoggerFactory.getLogger(ITResponse::class.java)
        private val MOCKRESPONSE = "/mockresponse/"

        protected fun responseFromJsonFile(path: String): String {
            try {
                ITResponse::class.java!!.getResourceAsStream(MOCKRESPONSE + path).use({ inputStream -> return IOUtils.toString(inputStream) })
            } catch (e: IOException) {
                LOGGER.error("can't read file from path", e)
                return ""
            }

        }
    }
}
