package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;

public abstract class AbstractCloudbreakIntegrationTest extends AbstractTestNGSpringContextTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest.class);
    private IntegrationTestContext itContext;

    @Autowired
    private SuiteContext suiteContext;

    @BeforeClass
    public void checkContextParameters(ITestContext testContext) throws Exception {
        itContext = suiteContext.getItContext(testContext.getSuite().getName());
        Assert.assertNotNull(itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "Access token cannot be null.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER), "Cloudbreak server endpoint must be given!");
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

    protected void checkResponse(Response entityCreationResponse, HttpStatus httpStatus, ContentType contentType) {
        entityCreationResponse.then().statusCode(httpStatus.value()).contentType(contentType);
    }

    protected IntegrationTestContext getItContext() {
        return itContext;
    }
}
