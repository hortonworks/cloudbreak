package com.sequenceiq.it

import javax.inject.Inject

import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.ConfigFileApplicationContextInitializer
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.springframework.util.StringUtils
import org.testng.ITestContext
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Optional
import org.testng.annotations.Parameters

import com.sequenceiq.it.config.IntegrationTestConfiguration

@ContextConfiguration(classes = IntegrationTestConfiguration::class, initializers = ConfigFileApplicationContextInitializer::class)
class TestSuiteInitializer : AbstractTestNGSpringContextTests() {
    @Value("${integrationtest.uaa.server}")
    private val defaultUaaServer: String? = null

    @Value("${integrationtest.uaa.user}")
    private val defaultUaaUser: String? = null

    @Value("${integrationtest.uaa.password}")
    private val defaultUaaPassword: String? = null

    @Inject
    private val suiteContext: SuiteContext? = null
    private var itContext: IntegrationTestContext? = null

    @BeforeSuite
    @Throws(Exception::class)
    fun initSuiteMap(testContext: ITestContext) {
        val suiteName = testContext.suite.name
        MDC.put("suite", suiteName)

        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass()
        springTestContextPrepareTestInstance()

        suiteContext!!.putItContext(suiteName, IntegrationTestContext())
        itContext = suiteContext.getItContext(suiteName)
    }

    @BeforeSuite(dependsOnMethods = "initSuiteMap", groups = "suiteInit")
    @Parameters("uaaServer", "uaaUser", "uaaPassword")
    @Throws(Exception::class)
    fun initTestSuite(@Optional("") uaaServer: String, @Optional("") uaaUser: String, @Optional("") uaaPassword: String) {
        var uaaServer = uaaServer
        var uaaUser = uaaUser
        var uaaPassword = uaaPassword
        uaaServer = getString(uaaServer, defaultUaaServer)
        uaaUser = getString(uaaUser, defaultUaaUser)
        uaaPassword = getString(uaaPassword, defaultUaaPassword)

        itContext!!.putContextParam(IntegrationTestContext.IDENTITY_URL, uaaServer)
        itContext!!.putContextParam(IntegrationTestContext.AUTH_USER, uaaUser)
        itContext!!.putContextParam(IntegrationTestContext.AUTH_PASSWORD, uaaPassword)
    }

    private fun getString(paramValue: String, defaultValue: String): String {
        return if (StringUtils.hasLength(paramValue)) paramValue else defaultValue
    }
}
