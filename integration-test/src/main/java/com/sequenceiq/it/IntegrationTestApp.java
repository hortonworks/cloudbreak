package com.sequenceiq.it;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

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
import org.springframework.util.CollectionUtils;
import org.testng.TestNG;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;
import com.sequenceiq.it.cloudbreak.cloud.v4.aws.AwsProperties;
import com.sequenceiq.it.config.ITProps;
import com.sequenceiq.it.util.TestNGUtil;
import com.sequenceiq.it.util.cleanup.CleanupUtil;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;

@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = { "com.sequenceiq.it", "com.sequenceiq.cloudbreak.cloud.gcp.util", "com.sequenceiq.cloudbreak.auth.crn" })
@EnableConfigurationProperties({ITProps.class, AwsProperties.class})
public class IntegrationTestApp implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestApp.class);

    private static final String CLEANUP_COMMAND = "cleanup";

    private static final String IMAGE_VALIDATION_COMMAND = "imagevalidation";

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

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private TestNGUtil testNGUtil;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        SpringApplication springApp = new SpringApplication(IntegrationTestApp.class);
        springApp.setWebApplicationType(WebApplicationType.NONE);
        springApp.setDefaultProperties(ImmutableMap.of("spring.main.allow-bean-definition-overriding", "true"));
        try {
            BouncyCastleFipsProviderLoader.load();
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
        testNGUtil.decorate(testng);
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
                testng.setXmlSuites(testNGUtil.loadSuites(suitePathes));
                break;
            case IMAGE_VALIDATION_COMMAND:
                testng.setXmlSuites(imageValidatorE2ETestUtil.getSuites());
                break;
            case CLEANUP_COMMAND:
                cleanupUtil.cleanupAllResources();
                break;
            default:
                LOG.info("Unknown command: {}", itCommand);
                break;
        }
    }
}
