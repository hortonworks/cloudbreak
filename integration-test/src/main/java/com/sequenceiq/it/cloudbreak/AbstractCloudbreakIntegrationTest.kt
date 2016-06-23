package com.sequenceiq.it.cloudbreak

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList

import javax.inject.Inject

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.ConfigFileApplicationContextInitializer
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.util.CollectionUtils
import org.testng.Assert
import org.testng.ITestContext
import org.testng.ITestResult
import org.testng.SkipException
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Optional
import org.testng.annotations.Parameters

import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.it.IntegrationTestContext
import com.sequenceiq.it.SuiteContext
import com.sequenceiq.it.config.IntegrationTestConfiguration

@ContextConfiguration(classes = IntegrationTestConfiguration::class, initializers = ConfigFileApplicationContextInitializer::class)
abstract class AbstractCloudbreakIntegrationTest : AbstractTestNGSpringContextTests() {
    protected var itContext: IntegrationTestContext? = null
        private set

    protected var cloudbreakClient: CloudbreakClient? = null
        private set

    @Inject
    private val suiteContext: SuiteContext? = null

    @BeforeClass
    @Throws(Exception::class)
    fun checkContextParameters(testContext: ITestContext) {
        itContext = suiteContext!!.getItContext(testContext.suite.name)
        if (itContext!!.getContextParam<Boolean>(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, Boolean::class.java) && !CollectionUtils.isEmpty(itContext!!.getContextParam<List>(CloudbreakITContextConstants.FAILED_TESTS, List<Any>::class.java))) {
            throw SkipException("Suite contains failed tests, the remaining tests will be skipped.")
        }
        cloudbreakClient = itContext!!.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java)

        Assert.assertNotNull(cloudbreakClient, "CloudbreakClient cannot be null.")
    }

    @AfterMethod
    @Parameters("sleepTime")
    fun sleepAfterTest(@Optional("0") sleepTime: Int) {
        if (sleepTime > 0) {
            LOGGER.info("Sleeping {}ms after test...", sleepTime)
            try {
                Thread.sleep(sleepTime.toLong())
            } catch (ex: Exception) {
                LOGGER.warn("Ex during sleep!")
            }

        }
    }

    @AfterMethod
    fun checkResult(testContext: ITestContext, testResult: ITestResult) {
        if (testResult.status == ITestResult.FAILURE) {
            var failedTests: MutableList<String>? = itContext!!.getContextParam<List<Any>>(CloudbreakITContextConstants.FAILED_TESTS, List<Any>::class.java)
            if (failedTests == null) {
                failedTests = ArrayList<String>()
                itContext!!.putContextParam(CloudbreakITContextConstants.FAILED_TESTS, failedTests)
            }
            failedTests.add(testContext.name)
        }
    }

    protected fun createTempFileFromClasspath(file: String): File {
        try {
            val sshPemInputStream = ClassPathResource(file).inputStream
            val tempKeystoreFile = File.createTempFile(file, ".tmp")
            try {
                FileOutputStream(tempKeystoreFile).use { outputStream -> IOUtils.copy(sshPemInputStream, outputStream) }
            } catch (e: IOException) {
                LOGGER.error("can't write " + file, e)
            }

            return tempKeystoreFile
        } catch (e: IOException) {
            throw RuntimeException(file + " not found", e)
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest::class.java)
    }
}
