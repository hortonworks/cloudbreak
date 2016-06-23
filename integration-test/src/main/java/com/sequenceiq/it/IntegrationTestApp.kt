package com.sequenceiq.it

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.io.Resource
import org.springframework.util.CollectionUtils
import org.testng.TestNG
import org.testng.internal.YamlParser
import org.testng.xml.IFileParser
import org.testng.xml.SuiteXmlParser
import org.testng.xml.XmlSuite
import org.uncommons.reportng.HTMLReporter
import org.uncommons.reportng.JUnitXMLReporter

import com.sequenceiq.it.cloudbreak.config.ITProps

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.sequenceiq.it")
@EnableConfigurationProperties(ITProps::class)
class IntegrationTestApp : CommandLineRunner {

    @Value("${integrationtest.testsuite.threadPoolSize}")
    private val suiteThreadPoolSize: Int = 0
    @Value("${integrationtest.command:}")
    private val itCommand: String? = null
    @Value("${integrationtest.fulltest.regindex:-1}")
    private val fullTestRegionIndex: Int = 0
    @Value("${integrationtest.fulltest.regnum:-1}")
    private val fullTestRegionNumber: Int = 0
    @Value("${integrationtest.outputdir:.}")
    private val outputDirectory: String? = null

    @Inject
    private val testng: TestNG? = null

    @Inject
    private val applicationContext: ApplicationContext? = null

    @Inject
    private val itProps: ITProps? = null

    @Throws(Exception::class)
    override fun run(vararg args: String) {
        testng!!.suiteThreadPoolSize = suiteThreadPoolSize
        testng.setVerbose(2)
        testng.outputDirectory = outputDirectory!! + "/test-output"
        testng.addListener(ThreadLocalTestListener())
        testng.addListener(HTMLReporter())
        testng.addListener(JUnitXMLReporter())
        setupSuites(testng)
        testng.run()
    }

    @Throws(Exception::class)
    private fun setupSuites(testng: TestNG) {
        when (itCommand) {
            "smoketest" -> setupSmokeTest(testng, itProps!!.testTypes)
            "fulltest" -> if (fullTestRegionIndex > -1 && fullTestRegionNumber > 0) {
                setupFullTest(testng, fullTestRegionIndex, fullTestRegionNumber)
            } else {
                LOG.info("fulltest command require integrationtest.fulltest.regindex and integrationtest.fulltest.regnum parameters!")
            }
            "suites" -> {
                val suiteFiles = itProps!!.suiteFiles
                if (!CollectionUtils.isEmpty(suiteFiles)) {
                    testng.setTestSuites(suiteFiles)
                }
            }
            "suiteurls" -> {
                val suitePathes = itProps!!.suiteFiles
                testng.setXmlSuites(loadSuites(suitePathes))
            }
            else -> LOG.info("Unknown command: {}", itCommand)
        }
    }

    @Throws(IOException::class)
    private fun setupSmokeTest(testng: TestNG, testTypes: List<String>) {
        if (!CollectionUtils.isEmpty(testTypes)) {
            val suitePathes = LinkedHashSet<String>()
            for (testType in testTypes) {
                val suites = itProps!!.getTestSuites(testType)
                if (suites != null) {
                    suitePathes.addAll(suites)
                }
            }
            testng.setXmlSuites(loadSuites(suitePathes))
        }
    }

    @Throws(IOException::class)
    private fun setupFullTest(testng: TestNG, salt: Int, regionNum: Int) {
        val suites = ArrayList<Resource>()
        suites.addAll(getProviderSuites("classpath:/testsuites/aws/full/*.yaml", salt, regionNum))
        suites.addAll(getProviderSuites("classpath:/testsuites/azure/full/*.yaml", salt, regionNum))
        suites.addAll(getProviderSuites("classpath:/testsuites/gcp/full/*.yaml", salt, regionNum))
        LOG.info("The following suites will be executed: {}", suites)
        testng.setXmlSuites(loadSuiteResources(suites))
    }

    @Throws(IOException::class)
    private fun getProviderSuites(providerDirPattern: String, salt: Int, regionNum: Int): Set<Resource> {
        var regionNum = regionNum
        val suites = applicationContext!!.getResources(providerDirPattern)
        val providerTests = HashSet<Resource>()
        regionNum = Math.min(regionNum, suites.size)
        val regionIndex = salt * regionNum % suites.size
        for (i in regionIndex..regionIndex + regionNum - 1) {
            providerTests.add(suites[i % suites.size])
        }
        return providerTests
    }

    @Throws(IOException::class)
    private fun loadSuiteResources(suitePathes: Collection<Resource>): List<XmlSuite> {
        val suites = ArrayList<XmlSuite>()
        for (suite in suitePathes) {
            suites.add(loadSuite(suite.url.toString(), suite))
        }
        return suites
    }

    @Throws(IOException::class)
    private fun loadSuites(suitePathes: Collection<String>): List<XmlSuite> {
        val suites = ArrayList<XmlSuite>()
        for (suitePath in suitePathes) {
            suites.add(loadSuite(suitePath))
        }
        return suites
    }

    @Throws(IOException::class)
    private fun loadSuite(suitePath: String, resource: Resource = applicationContext!!.getResource(suitePath)): XmlSuite {
        val parser = getParser(suitePath)
        resource.inputStream.use { inputStream -> return parser.parse(suitePath, inputStream, true) }
    }

    private fun getParser(fileName: String): IFileParser<Any> {
        var result: IFileParser<Any> = DEFAULT_FILE_PARSER
        if (fileName.endsWith("xml")) {
            result = XML_PARSER
        } else if (fileName.endsWith("yaml") || fileName.endsWith("yml")) {
            result = YAML_PARSER
        }
        return result
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IntegrationTestApp::class.java)
        private val XML_PARSER = SuiteXmlParser()
        private val YAML_PARSER = YamlParser()
        private val DEFAULT_FILE_PARSER = XML_PARSER

        @Throws(Exception::class)
        @JvmStatic fun main(args: Array<String>) {
            val springApp = SpringApplication(*arrayOf<Any>(IntegrationTestApp::class.java))
            springApp.setWebEnvironment(false)
            springApp.run(*args)
        }
    }
}
