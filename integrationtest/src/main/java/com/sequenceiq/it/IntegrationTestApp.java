package com.sequenceiq.it;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;
import org.testng.TestNG;
import org.testng.internal.Yaml;
import org.testng.xml.XmlSuite;

import com.sequenceiq.it.cloudbreak.config.ITProps;

@SpringBootApplication
@EnableConfigurationProperties(ITProps.class)
public class IntegrationTestApp implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestApp.class);

    @Value("${integrationtest.testsuite.threadPoolSize}")
    private int suiteThreadPoolSize;
    @Value("${integrationtest.command:}")
    private String itCommand;
    @Value("${integrationtest.fulltest.regindex:-1}")
    private int fullTestRegionIndex;
    @Value("${integrationtest.fulltest.regnum:-1}")
    private int fullTestRegionNumber;

    @Autowired
    private TestNG testng;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ITProps itProps;

    @Override
    public void run(String... args) throws Exception {
        testng.setSuiteThreadPoolSize(suiteThreadPoolSize);
        testng.setVerbose(2);
        testng.addListener(new ThreadLocalTestListener());
        setupSuites(testng);
        testng.run();
    }

    private void setupSuites(TestNG testng) throws Exception {
        switch (itCommand) {
        case "smoketest":
            List<String> testTypes = itProps.getTestTypes();
            if (!CollectionUtils.isEmpty(testTypes)) {
                Set<String> suitePathes = new LinkedHashSet<>();
                for (String testType : testTypes) {
                    List<String> suites = itProps.getTestSuites(testType);
                    if (suites != null) {
                        suitePathes.addAll(suites);
                    }
                }
                testng.setXmlSuites(loadSuites(suitePathes));
            }
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
        default:
            LOG.info("Unknown command: {}", itCommand);
            break;
        }
    }

    private void setupFullTest(TestNG testng, int salt, int regionNum) throws IOException {
        List<Resource> suites = new ArrayList<>();
        suites.addAll(getProviderSuites("classpath:/testsuites/aws/full/*.yaml", salt, regionNum));
        suites.addAll(getProviderSuites("classpath:/testsuites/azure/full/*.yaml", salt, regionNum));
        suites.addAll(getProviderSuites("classpath:/testsuites/gcp/full/*.yaml", salt, regionNum));
        LOG.info("The following suites will be executed: {}", suites);
        testng.setXmlSuites(loadSuiteResources(suites));
    }

    private List<Resource> getProviderSuites(String providerDirPattern, int salt, int regionNum) throws IOException {
        Resource[] suites = applicationContext.getResources(providerDirPattern);
        List<Resource> providerTests = new ArrayList<>();
        regionNum = Math.min(regionNum, suites.length);
        int regionIndex = salt * regionNum % suites.length;
        for (int i = regionIndex; i < regionIndex + regionNum; i++) {
            providerTests.add(suites[i % suites.length]);
        }
        return providerTests;
    }

    private List<XmlSuite> loadSuiteResources(Collection<Resource> suitePathes) throws IOException {
        List<XmlSuite> suites = new ArrayList<>();
        for (Resource suite: suitePathes) {
            suites.add(loadSuite(suite.getURL().toString(), suite));
        }
        return suites;
    }

    private List<XmlSuite> loadSuites(Collection<String> suitePathes) throws IOException {
        List<XmlSuite> suites = new ArrayList<>();
        for (String suitePath: suitePathes) {
            suites.add(loadSuite(suitePath));
        }
        return suites;
    }

    private XmlSuite loadSuite(String suitePath) throws IOException {
        return loadSuite(suitePath, applicationContext.getResource(suitePath));
    }

    private XmlSuite loadSuite(String suitePath, Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return Yaml.parse(suitePath, inputStream);
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(IntegrationTestApp.class, args);
    }
}
