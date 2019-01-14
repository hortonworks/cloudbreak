package com.sequenceiq.it;

import javax.inject.Inject;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class TestSuiteInitializer extends AbstractTestNGSpringContextTests {

    @Value("${integrationtest.caas.token}")
    private String defaultRefreshToken;

    @Inject
    private SuiteContext suiteContext;

    private IntegrationTestContext itContext;

    @BeforeSuite
    public void initSuiteMap(ITestContext testContext) throws Exception {
        String suiteName = testContext.getSuite().getName();
        MDC.put("testlabel", suiteName);

        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        suiteContext.putItContext(suiteName, new IntegrationTestContext());
        itContext = suiteContext.getItContext(suiteName);
    }

    @BeforeSuite(dependsOnMethods = "initSuiteMap", groups = "suiteInit")
    @Parameters({ "refreshToken" })
    public void initTestSuite(@Optional("") String refreshToken) {
        refreshToken = getString(refreshToken, defaultRefreshToken);

        itContext.putContextParam(IntegrationTestContext.REFRESH_TOKEN, refreshToken);
    }

    private String getString(String paramValue, String defaultValue) {
        return StringUtils.hasLength(paramValue) ? paramValue : defaultValue;
    }
}
