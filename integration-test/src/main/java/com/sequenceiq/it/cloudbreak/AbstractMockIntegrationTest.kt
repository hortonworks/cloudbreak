package com.sequenceiq.it.cloudbreak

import spark.Spark.after
import spark.Spark.before
import spark.Spark.secure
import spark.Spark.stop

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.HashMap

import javax.inject.Inject

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ResourceLoader
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass

import com.google.gson.Gson
import com.sequenceiq.it.verification.Call
import com.sequenceiq.it.verification.Verification

import spark.Response

abstract class AbstractMockIntegrationTest : AbstractCloudbreakIntegrationTest() {

    private val gson = Gson()
    private var requestResponseMap: MutableMap<Call, Response>? = null

    @Inject
    private val resourceLoader: ResourceLoader? = null

    @BeforeClass
    fun configMockServer() {
        requestResponseMap = HashMap<Call, Response>()
        val keystoreFile = createTempFileFromClasspath("/keystore_server")
        secure(keystoreFile.path, "secret", null, null)
    }

    fun gson(): Gson {
        return gson
    }

    protected fun initSpark() {
        before { req, res -> res.type("application/json") }
        after { request, response -> requestResponseMap!!.put(Call.fromRequest(request), response) }
    }

    @AfterClass
    fun breakDown() {
        stop()
    }

    fun verify(path: String, httpMethod: String): Verification {
        return Verification(path, httpMethod, requestResponseMap, false)
    }

    fun verifyRegexpPath(regexpPath: String, httpMethod: String): Verification {
        return Verification(regexpPath, httpMethod, requestResponseMap, true)
    }

    protected fun responseFromJsonFile(path: String): String {
        try {
            resourceLoader!!.getResource("/mockresponse/" + path).inputStream.use { inputStream -> return IOUtils.toString(inputStream) }
        } catch (e: IOException) {
            LOGGER.error("can't read file from path", e)
            return ""
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AbstractMockIntegrationTest::class.java)
    }
}
