package com.sequenceiq.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.springframework.util.CollectionUtils;
import org.testng.TestNG;
import org.testng.internal.YamlParser;
import org.testng.xml.IFileParser;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlSuite;
import org.uncommons.reportng.JUnitXMLReporter;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.it.cloudbreak.config.ITProps;
import com.sequenceiq.it.cloudbreak.newway.listener.ReportListener;
import com.sequenceiq.it.cloudbreak.newway.logsearch.CustomHTMLReporter;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = "com.sequenceiq.it")
@EnableConfigurationProperties(ITProps.class)
public class IntegrationTestApp implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestApp.class);

    private static final IFileParser<XmlSuite> XML_PARSER = new SuiteXmlParser();

    private static final IFileParser<XmlSuite> YAML_PARSER = new YamlParser();

    private static final IFileParser<XmlSuite> DEFAULT_FILE_PARSER = XML_PARSER;

    @Value("${integrationtest.testsuite.threadPoolSize}")
    private int suiteThreadPoolSize;

    @Value("${integrationtest.command:}")
    private String itCommand;

    @Value("${integrationtest.fulltest.regindex:-1}")
    private int fullTestRegionIndex;

    @Value("${integrationtest.fulltest.regnum:-1}")
    private int fullTestRegionNumber;

    @Value("${integrationtest.outputdir:.}")
    private String outputDirectory;

    @Inject
    private TestNG testng;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ITProps itProps;

    public static void main(String[] args) {
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

    private List<XmlSuite> loadSuites(Iterable<String> suitePathes) throws IOException {
        List<XmlSuite> suites = new ArrayList<>();
        for (String suitePath : suitePathes) {
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
            return parser.parse(suitePath, inputStream, true);
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
