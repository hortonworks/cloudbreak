package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.ITestContext;
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
    private CloudbreakClient client;

    @Autowired
    private SuiteContext suiteContext;

    @BeforeClass
    public void checkContextParameters(ITestContext testContext) throws Exception {
        itContext = suiteContext.getItContext(testContext.getSuite().getName());
        client = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Assert.assertNotNull(client, "CloudbreakClient cannot be null.");
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

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected CloudbreakClient getClient() {
        return client;
    }
}
