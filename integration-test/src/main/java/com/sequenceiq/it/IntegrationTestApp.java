package com.sequenceiq.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.testng.TestNG;
import org.testng.internal.YamlParser;
import org.testng.xml.IFileParser;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlSuite;
import org.uncommons.reportng.JUnitXMLReporter;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.it.cloudbreak.config.ITProps;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.newway.listener.ReportListener;
import com.sequenceiq.it.cloudbreak.newway.logsearch.CustomHTMLReporter;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = "com.sequenceiq.it")
@EnableConfigurationProperties({ITProps.class, AwsProperties.class})
public class IntegrationTestApp implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestApp.class);

    private static final IFileParser<XmlSuite> XML_PARSER = new SuiteXmlParser();

    private static final IFileParser<XmlSuite> YAML_PARSER = new YamlParser();

    private static final IFileParser<XmlSuite> DEFAULT_FILE_PARSER = XML_PARSER;

    @Value("${integrationtest.testsuite.threadPoolSize:8}")
    private int suiteThreadPoolSize;

    @Value("${integrationtest.command:}")
    private String itCommand;

    @Value("${integrationtest.fulltest.regindex:-1}")
    private int fullTestRegionIndex;

    @Value("${integrationtest.fulltest.regnum:-1}")
    private int fullTestRegionNumber;

    @Value("${integrationtest.outputdir:.}")
    private String outputDirectory;

    @Value("${integrationtest.threadCount:2}")
    private int threadCount;

    @Value("${integrationtest.parallel:FALSE}")
    private String parallel;

    @Inject
    private TestNG testng;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ITProps itProps;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication springApp = new SpringApplication(IntegrationTestApp.class);
        springApp.setWebApplicationType(WebApplicationType.NONE);
        springApp.setDefaultProperties(ImmutableMap.of("spring.main.allow-bean-definition-overriding", "true"));
        try {
            ConfigurableApplicationContext context = springApp.run(args);
            LOG.info("Closing Spring test context.");
            context.close();
            LOG.info("test successfully run");
        } catch (Exception e) {
            LOG.error("Something went wrong during closing Spring Context.");
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            threadSet.stream().forEach(t -> LOG.info("Runnning threads: {}", t.getName()));
            System.exit(1);
        }
        LOG.info("run successfully: {}", System.currentTimeMillis() - start);
        System.exit(0);
    }

    @Override
    public void run(String... args) throws Exception {
        testng.setSuiteThreadPoolSize(suiteThreadPoolSize);
        testng.setVerbose(2);
        testng.setOutputDirectory(outputDirectory + "/test-output");
        testng.addListener(new ThreadLocalTestListener());
        testng.addListener(new CustomHTMLReporter());
        testng.addListener(new JUnitXMLReporter());
        testng.addListener(new ReportListener());
        setupSuites(testng);
        testng.run();
        LOG.info("Html result of test run: file://{}/test-output/index.html", System.getProperty("user.dir"));
        LOG.info("Text based result of test run: file://{}/test-output/emailable-report.html", System.getProperty("user.dir"));
    }

    private void setupSuites(TestNG testng) throws Exception {
        switch (itCommand) {
            case "smoketest":
                setupSmokeTest(testng, itProps.getTestTypes());
                break;
            case "fulltest":
                if (fullTestRegionIndex > -1 && fullTestRegionNumber > 0) {
                    setupFullTest(testng, fullTestRegionIndex, fullTestRegionNumber);
                } else {
                    LOG.info("fulltest command require integrationtest.fulltest.regindex and integrationtest.fulltest.regnum parameters!");
                }
                break;
            case "suites":
                List<String> suiteFiles = itProps.getSuiteFiles();
                if (!CollectionUtils.isEmpty(suiteFiles)) {
                    testng.setTestSuites(suiteFiles);
                }
                break;
            case "suiteurls":
                List<String> suitePathes = itProps.getSuiteFiles();
                testng.setXmlSuites(loadSuites(suitePathes));

                break;
            default:
                LOG.info("Unknown command: {}", itCommand);
                break;
        }
    }

    private void setupSmokeTest(TestNG testng, Collection<String> testTypes) throws IOException {
        if (!CollectionUtils.isEmpty(testTypes)) {
            Collection<String> suitePathes = new LinkedHashSet<>();
            for (String testType : testTypes) {
                List<String> suites = itProps.getTestSuites(testType);
                if (suites != null) {
                    suitePathes.addAll(suites);
                }
            }
            testng.setXmlSuites(loadSuites(suitePathes));
        }
    }

    private void setupFullTest(TestNG testng, int salt, int regionNum) throws IOException {
        Collection<Resource> suites = new ArrayList<>();
        suites.addAll(getProviderSuites("classpath:/testsuites/aws/full/*.yaml", salt, regionNum));
        suites.addAll(getProviderSuites("classpath:/testsuites/azure/full/*.yaml", salt, regionNum));
        suites.addAll(getProviderSuites("classpath:/testsuites/gcp/full/*.yaml", salt, regionNum));
        LOG.info("The following suites will be executed: {}", suites);
        testng.setXmlSuites(loadSuiteResources(suites));
    }

    private Collection<Resource> getProviderSuites(String providerDirPattern, int salt, int regionNum) throws IOException {
        Resource[] suites = applicationContext.getResources(providerDirPattern);
        Collection<Resource> providerTests = new HashSet<>();
        regionNum = Math.min(regionNum, suites.length);
        int regionIndex = salt * regionNum % suites.length;
        for (int i = regionIndex; i < regionIndex + regionNum; i++) {
            providerTests.add(suites[i % suites.length]);
        }
        return providerTests;
    }

    private List<XmlSuite> loadSuiteResources(Iterable<Resource> suitePaths) throws IOException {
        List<XmlSuite> suites = new ArrayList<>();
        for (Resource suite : suitePaths) {
            suites.add(loadSuite(suite.getURL().toString(), suite));
        }
        return suites;
    }

    private List<XmlSuite> loadSuites(Iterable<String> suitePaths) throws IOException {
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
            xmlSuite.setParallel(XmlSuite.ParallelMode.valueOf(parallel.toUpperCase()));
            xmlSuite.setThreadCount(threadCount);
            LOG.info("Test are running in: {} type of parallel mode", parallel.toUpperCase());
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
}
