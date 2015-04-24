package com.sequenceiq.it;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.RestUtil;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class TestSuiteInitializer extends AbstractTestNGSpringContextTests {
    @Value("${integrationtest.uaa.server}")
    private String defaultUaaServer;

    @Value("${integrationtest.uaa.user}")
    private String defaultUaaUser;

    @Value("${integrationtest.uaa.password}")
    private String defaultUaaPassword;

    @Autowired
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
    public void initTestSuite(@Optional("") String uaaServer, @Optional("") String uaaUser, @Optional("") String uaaPassword) throws Exception {
        uaaServer = getString(uaaServer, defaultUaaServer);
        uaaUser = getString(uaaUser, defaultUaaUser);
        uaaPassword = getString(uaaPassword, defaultUaaPassword);

        String accessToken = RestUtil.getToken(uaaServer, uaaUser, uaaPassword);
        Assert.assertNotNull(accessToken, "Access token cannot be null.");
        itContext.putContextParam(IntegrationTestContext.AUTH_TOKEN, accessToken);
    }

    private String getString(String paramValue, String defaultValue) {
        return StringUtils.hasLength(paramValue) ? paramValue : defaultValue;
    }
}
