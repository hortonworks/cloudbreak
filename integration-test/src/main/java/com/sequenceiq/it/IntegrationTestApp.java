package com.sequenceiq.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.cloudbreak.listener.ReportListener;
import com.sequenceiq.it.cloudbreak.listener.TestInvocationListener;
import com.sequenceiq.it.cloudbreak.listener.TestNgListener;
import com.sequenceiq.it.cloudbreak.listener.ThreadLocalTestListener;
import com.sequenceiq.it.cloudbreak.search.CustomHTMLReporter;
import com.sequenceiq.it.cloudbreak.search.CustomJUnitXMLReporter;
import com.sequenceiq.it.config.ITProps;
import com.sequenceiq.it.cloudbreak.listener.TestCaseTimeoutListener;
import com.sequenceiq.it.util.cleanup.CleanupUtil;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = { "com.sequenceiq.it", "com.sequenceiq.cloudbreak.cloud.gcp.util", "com.sequenceiq.cloudbreak.auth.crn" })
@EnableConfigurationProperties({ITProps.class, AwsProperties.class})
public class IntegrationTestApp implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestApp.class);

    private static final IFileParser<XmlSuite> XML_PARSER = new SuiteXmlParser();

    private static final IFileParser<XmlSuite> YAML_PARSER = new YamlParser();

    private static final IFileParser<XmlSuite> DEFAULT_FILE_PARSER = XML_PARSER;

    private static final String CLEANUP_COMMAND = "cleanup";

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

    @Inject
    private TestNG testng;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ITProps itProps;

    @Inject
    private CleanupUtil cleanupUtil;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication springApp = new SpringApplication(IntegrationTestApp.class);
        springApp.setWebApplicationType(WebApplicationType.NONE);
        springApp.setDefaultProperties(ImmutableMap.of("spring.main.allow-bean-definition-overriding", "true"));
        try {
            ConfigurableApplicationContext context = springApp.run(args);
            LOG.info("Closing Spring test context.");
            context.close();
            LOG.info("Tests have been done successfully: {} milliseconds", System.currentTimeMillis() - start);
            System.exit(0);
        } catch (Exception e) {
            LOG.error("Exception occurred during closing Spring Context: {}", e.getMessage(), e);
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            threadSet.stream().forEach(t -> LOG.info("Running threads: {}", t.getName()));
            System.exit(1);
        }
    }

    @Override
    public void run(String... args) throws Exception {
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
        setupSuites(testng);
        if (!CLEANUP_COMMAND.equals(itCommand)) {
            testng.run();
            LOG.info("JUnit Xml results of test run: file://{}/test-output/xml/", Paths.get(outputDirectory).toAbsolutePath().normalize());
            LOG.info("Html result of test run: file://{}/test-output/html/index.html", Paths.get(outputDirectory).toAbsolutePath().normalize());
            LOG.info("Text based result of test run: file://{}/test-output/emailable-report.html", Paths.get(outputDirectory).toAbsolutePath().normalize());
        }
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
                removeOldResourceFiles();
                break;
            case CLEANUP_COMMAND:
                cleanupUtil.cleanupAllResources();
                break;
            default:
                LOG.info("Unknown command: {}", itCommand);
                break;
        }
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
            xmlSuite.setParallel(XmlSuite.ParallelMode.getValidParallel(parallel));
            xmlSuite.setThreadCount(threadCount);
            xmlSuite.setTimeOut(timeOut);
            xmlSuite.setVerbose(2);
            xmlSuite.setListeners(Arrays.asList(TestNgListener.class.getName(), ThreadLocalTestListener.class.getName(),
                    ReportListener.class.getName(), TestInvocationListener.class.getName(), CustomHTMLReporter.class.getName(),
                    CustomJUnitXMLReporter.class.getName(), TestCaseTimeoutListener.class.getName()));
            LOG.info("Test are running in: {} type of parallel mode, thread count: {} and with test timeout: {}", parallel.toUpperCase(), threadCount, timeOut);
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

    /**
     * Removes all the collected E2E test resources' files from the test folder (defined by 'outputDirectory') if there is any present.
     */
    private void removeOldResourceFiles() {
        File folder = new File(outputDirectory);
        Path folderPath = Path.of(folder.getPath()).normalize().toAbsolutePath();

        File[] foundResourceFiles = Optional.ofNullable(folder.listFiles((file, path) -> path.contains("resource_names_"))).orElse(new File[0]);
        if (foundResourceFiles.length > 0) {
            Arrays.stream(foundResourceFiles).forEach(file -> {
                try {
                    FileTime creationTime = Files.readAttributes(Path.of(file.getName()), BasicFileAttributes.class).creationTime();
                    if (file.delete()) {
                        LOG.info("Old resource file: {} (creation time: {}) have been found and deleted at: {}.", file.getName(), creationTime, folderPath);
                    } else {
                        LOG.info("Old resource file: {} (creation time: {}) have NOT been deleted at: {}.", file.getName(), creationTime, folderPath);
                    }
                } catch (IOException e) {
                    LOG.info("{} resource file get creation time throws exception: {}", file.getName(), e.getMessage(), e);
                } catch (Exception e) {
                    LOG.info("{} resource file cleanup has been failed, because of: {}", file.getName(), e.getMessage(), e);
                }
            });
        }
    }
}
