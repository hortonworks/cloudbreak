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
    @Value("${integrationtest.uaa.server}")
    private String defaultUaaServer;

    @Value("${integrationtest.uaa.user}")
    private String defaultUaaUser;

    @Value("${integrationtest.uaa.password}")
    private String defaultUaaPassword;

    @Inject
    private SuiteContext suiteContext;

    private IntegrationTestContext itContext;

    @BeforeSuite
    public void initSuiteMap(ITestContext testContext) throws Exception {
        String suiteName = testContext.getSuite().getName();
        MDC.put("suite", suiteName);

        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        suiteContext.putItContext(suiteName, new IntegrationTestContext());
        itContext = suiteContext.getItContext(suiteName);
    }

    @BeforeSuite(dependsOnMethods = "initSuiteMap", groups = "suiteInit")
    @Parameters({ "uaaServer", "uaaUser", "uaaPassword" })
    public void initTestSuite(@Optional("") String uaaServer, @Optional("") String uaaUser, @Optional("") String uaaPassword) {
        uaaServer = getString(uaaServer, defaultUaaServer);
        uaaUser = getString(uaaUser, defaultUaaUser);
        uaaPassword = getString(uaaPassword, defaultUaaPassword);

        itContext.putContextParam(IntegrationTestContext.IDENTITY_URL, uaaServer);
        itContext.putContextParam(IntegrationTestContext.AUTH_USER, uaaUser);
        itContext.putContextParam(IntegrationTestContext.AUTH_PASSWORD, uaaPassword);
    }

    private String getString(String paramValue, String defaultValue) {
        return StringUtils.hasLength(paramValue) ? paramValue : defaultValue;
    }
}
