package com.sequenceiq.it.cloudbreak;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.CollectionUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public abstract class AbstractCloudbreakIntegrationTest extends AbstractTestNGSpringContextTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest.class);

    private IntegrationTestContext itContext;

    private CloudbreakClient cloudbreakClient;

    @Inject
    private SuiteContext suiteContext;

    @BeforeClass
    public void checkContextParameters(ITestContext testContext) {
        itContext = suiteContext.getItContext(testContext.getSuite().getName());
        if (itContext.getContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, Boolean.class)
                && !CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class))) {
            throw new SkipException("Suite contains failed tests, the remaining tests will be skipped.");
        }
        cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);

        Assert.assertNotNull(cloudbreakClient, "CloudbreakClient cannot be null.");
    }

    @AfterMethod
    @Parameters("sleepTime")
    public void sleepAfterTest(@Optional("0") int sleepTime) {
        if (sleepTime > 0) {
            LOGGER.info("Sleeping {}ms after test...", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (Exception ex) {
                LOGGER.warn("Ex during sleep!");
            }
        }
    }

    @AfterMethod
    public void checkResult(ITestContext testContext, ITestResult testResult) {
        if (testResult.getStatus() == ITestResult.FAILURE) {
            List<String> failedTests = itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class);
            if (failedTests == null) {
                failedTests = new ArrayList<>();
                itContext.putContextParam(CloudbreakITContextConstants.FAILED_TESTS, failedTests);
            }
            failedTests.add(testContext.getName());
        }
    }

    protected File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient;
    }
}
