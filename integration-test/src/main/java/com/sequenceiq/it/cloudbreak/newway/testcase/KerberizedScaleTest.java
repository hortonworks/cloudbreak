package com.sequenceiq.it.cloudbreak.newway.testcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class KerberizedScaleTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberizedScaleTest.class);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        LOGGER.info("All routes added: {}", testContext.getSparkServer().getSparkService().getPaths());
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext", enabled = false)
    public void testScaling(TestContext testContext) {

    }
}
