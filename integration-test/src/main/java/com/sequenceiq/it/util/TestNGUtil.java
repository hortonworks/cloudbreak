package com.sequenceiq.it.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;
import org.testng.TestNG;
import org.testng.internal.YamlParser;
import org.testng.xml.IFileParser;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.sequenceiq.it.cloudbreak.listener.GatekeeperBehaviour;
import com.sequenceiq.it.cloudbreak.listener.PassedTestsReporter;
import com.sequenceiq.it.cloudbreak.listener.ReportListener;
import com.sequenceiq.it.cloudbreak.listener.TestCaseTimeoutListener;
import com.sequenceiq.it.cloudbreak.listener.TestInvocationListener;
import com.sequenceiq.it.cloudbreak.listener.TestNgListener;
import com.sequenceiq.it.cloudbreak.listener.ThreadLocalTestListener;
import com.sequenceiq.it.cloudbreak.search.CustomHTMLReporter;
import com.sequenceiq.it.cloudbreak.search.CustomJUnitXMLReporter;

@Component
public class TestNGUtil {

    private static final Logger LOG = LoggerFactory.getLogger(TestNGUtil.class);

    private static final IFileParser<XmlSuite> XML_PARSER = new SuiteXmlParser();

    private static final IFileParser<XmlSuite> YAML_PARSER = new YamlParser();

    private static final IFileParser<XmlSuite> DEFAULT_FILE_PARSER = XML_PARSER;

    @Inject
    private ApplicationContext applicationContext;

    @Value("${integrationtest.testsuite.threadPoolSize:8}")
    private int suiteThreadPoolSize;

    @Value("${integrationtest.command:}")
    private String itCommand;

    @Value("${integrationtest.outputdir:.}")
    private String outputDirectory;

    @Value("${integrationtest.threadCount:8}")
    private int threadCount;

    @Value("${integrationtest.parallel:methods}")
    private String parallel;

    @Value("${integrationtest.timeOut:6000000}")
    private String timeOut;

    public void decorate(TestNG testng) {
        testng.setOutputDirectory(outputDirectory + "/test-output");
        testng.setParallel(XmlSuite.ParallelMode.getValidParallel(parallel));
        testng.setThreadCount(threadCount);
        testng.setSuiteThreadPoolSize(suiteThreadPoolSize);
        testng.setVerbose(2);
        testng.addListener(new TestNgListener());
        testng.addListener(new ThreadLocalTestListener());
        testng.addListener(new ReportListener());
        testng.addListener(new TestInvocationListener());
        testng.addListener(new CustomHTMLReporter());
        testng.addListener(new CustomJUnitXMLReporter());
        testng.addListener(new TestCaseTimeoutListener());
        testng.addListener(new PassedTestsReporter());
        testng.addListener(new GatekeeperBehaviour());
    }

    private void decorate(XmlSuite xmlSuite) {
        xmlSuite.setParallel(XmlSuite.ParallelMode.getValidParallel(parallel));
        xmlSuite.setThreadCount(threadCount);
        xmlSuite.setTimeOut(timeOut);
        xmlSuite.setVerbose(2);
        xmlSuite.setListeners(
                Arrays.asList(
                    TestNgListener.class.getName(),
                    ThreadLocalTestListener.class.getName(),
                    ReportListener.class.getName(),
                    TestInvocationListener.class.getName(),
                    CustomHTMLReporter.class.getName(),
                    CustomJUnitXMLReporter.class.getName(),
                    TestCaseTimeoutListener.class.getName(),
                    PassedTestsReporter.class.getName(),
                    GatekeeperBehaviour.class.getName()
                )
        );
    }

    public List<XmlSuite> loadSuites(Iterable<String> suitePaths) throws IOException {
        List<XmlSuite> suites = new ArrayList<>();
        for (String suitePath : suitePaths) {
            suites.add(loadSuite(suitePath));
        }
        LOG.info("parsed suites: {}", suites.size());
        return suites;
    }

    private XmlSuite loadSuite(String suitePath) throws IOException {
        LOG.info("load suite: {}", suitePath);
        try {
            return loadSuite(suitePath, applicationContext.getResource(suitePath));
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
            throw e;
        }
    }

    private XmlSuite loadSuite(String suitePath, InputStreamSource resource) throws IOException {
        IFileParser<XmlSuite> parser = getParser(suitePath);
        try (InputStream inputStream = resource.getInputStream()) {
            XmlSuite xmlSuite = parser.parse(suitePath, inputStream, true);
            decorate(xmlSuite);
            LOG.info("Test are running in: {} type of parallel mode, thread count: {} and with test timeout: {}",
                    parallel.toUpperCase(Locale.ROOT),
                    threadCount,
                    timeOut
            );
            return xmlSuite;
        }
    }

    private IFileParser<XmlSuite> getParser(String fileName) {
        IFileParser<XmlSuite> result = DEFAULT_FILE_PARSER;
        if (fileName.endsWith("xml")) {
            result = XML_PARSER;
        } else if (fileName.endsWith("yaml") || fileName.endsWith("yml")) {
            result = YAML_PARSER;
        }
        return result;
    }

    public XmlSuite createSuite(String name) {
        XmlSuite xmlSuite = new XmlSuite();
        xmlSuite.setName(name);
        decorate(xmlSuite);
        return xmlSuite;
    }

    public XmlTest createTest(XmlSuite xmlSuite, String name, boolean gatekeeper) {
        XmlTest xmlTest = new XmlTest();
        xmlTest.setSuite(xmlSuite);
        xmlSuite.addTest(xmlTest);
        xmlTest.setName(name);
        if (gatekeeper) {
            xmlTest.addParameter(GatekeeperBehaviour.IS_GATEKEEPER, GatekeeperBehaviour.TRUE);
            xmlTest.setPreserveOrder(true);
        }
        return xmlTest;
    }

    public void addTestCase(XmlTest xmlTest, Class<?> testClass, String methodName) {
        XmlClass xmlClass = new XmlClass(testClass);
        xmlClass.setIncludedMethods(List.of(new XmlInclude(methodName)));
        xmlTest.getXmlClasses().add(xmlClass);
    }

}
