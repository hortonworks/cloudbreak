package com.sequenceiq.it.cloudbreak.newway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.logsearch.LogSearchProps;
import com.sequenceiq.it.cloudbreak.newway.logsearch.LogSearchUtil;

@EnableConfigurationProperties(LogSearchProps.class)
public class CloudbreakTest extends GherkinTest {
    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String IDENTITY_URL = "IDENTITY_URL";

    public static final String USER = "USER";

    public static final String PASSWORD = "PASSWORD";

    public static final String SECOND_USER = "SECOND_USER";

    public static final String SECOND_PASSWORD = "SECOND_PASSWORD";

    public static final String AUTOSCALE_CLIENTID = "AUTOSCALE_CLIENTID";

    public static final String AUTOSCALE_SECRET = "AUTOSCALE_SECRET";

    public static final String WORKSPACE_ID = "WORKSPACE_ID";

    public static final String LOG_SEARCH_QUERY_TYPES = "LOG_SEARCH_QUERY_TYPES";

    public static final String LOG_SEARCH_URL_PREFIX = "LOG_SEARCH_URL_PREFIX";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTest.class);

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.uaa.server}")
    private String uaaServer;

    @Value("${integrationtest.uaa.user}")
    private String defaultUaaUser;

    @Value("${integrationtest.uaa.password}")
    private String defaultUaaPassword;

    @Value("${integrationtest.uaa.autoscale.clientId:periscope}")
    private String autoscaleUaaClientId;

    @Value("${integrationtest.uaa.autoscale.clientSecret}")
    private String autoscaleUaaClientSecret;

    @Inject
    private Environment environment;

    @Inject
    private LogSearchProps logSearchProps;

    private TestParameter testParameter;

    public CloudbreakTest() {
        try {
            springTestContextBeforeTestClass();
            springTestContextPrepareTestInstance();
        } catch (Exception ignored) {
        }

        testParameter = new TestParameter();

        LOGGER.info("CloudbreakTest default values ::: ");
        IntegrationTestContext testContext = getItContext();
        testContext.putContextParam(CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testContext.putContextParam(IDENTITY_URL, uaaServer);
        testContext.putContextParam(USER, defaultUaaUser);
        testContext.putContextParam(PASSWORD, defaultUaaPassword);
        testContext.putContextParam(AUTOSCALE_CLIENTID, autoscaleUaaClientId);
        testContext.putContextParam(AUTOSCALE_SECRET, autoscaleUaaClientSecret);
        testContext.putContextParam(LOG_SEARCH_QUERY_TYPES, logSearchProps.getQueryTypes());
        testContext.putContextParam(LOG_SEARCH_URL_PREFIX, logSearchProps.getUrl());

        LogSearchUtil.addQueryModelForLogSearchUrlToContext(testContext, LogSearchUtil.LOG_SEARCH_CBOWNER_ID,
                LogSearchUtil.LOG_SEARCH_CBOWNER_QUERY_TYPE, defaultUaaUser);

        testParameter.put("INTEGRATIONTEST_CLOUDBREAK_SERVER", server + cbRootContextPath);
        testParameter.put("INTEGRATIONTEST_UAA_SERVER", uaaServer);
        testParameter.put("INTEGRATIONTEST_UAA_USER", defaultUaaUser);
        testParameter.put("INTEGRATIONTEST_UAA_PASSWORD", defaultUaaPassword);

        try {
            CloudbreakClient client = CloudbreakClient.created();
            client.create(testContext);
            testContext.putContextParam(WORKSPACE_ID,
                    client.getCloudbreakClient().workspaceV3Endpoint().getByName(defaultUaaUser).getId());
        } catch (Exception ignored) {
        }
    }

    public TestParameter getTestParameter() {
        return testParameter;
    }

    public final void setTestParameter(TestParameter tp) {
        testParameter = tp;
    }

    @BeforeSuite
    @BeforeClass
    @BeforeTest(alwaysRun = true)
    public void digestParameters(ITestContext testngContext) {
        LOGGER.info("CloudbreakTest load test parameters ::: ");
        if (testngContext != null) {
            getTestParameter().putAll(testngContext.getCurrentXmlTest().getAllParameters());
        }

        LOGGER.info("Application.yml based parameters ::: ");
        getTestParameter().putAll(getAllKnownProperties(environment));
    }

    @AfterMethod(alwaysRun = true)
    public void addLogSearchUrlToParameters(ITestResult testResult) {
        Optional<String> url = LogSearchUtil.createLogSearchUrl(getItContext(),
                logSearchProps.getTimeRangeInterval(), logSearchProps.getComponents());
        if (url.isPresent()) {
            LOGGER.info("Logsearch URL of {} method is {}", testResult.getName(), url.get());
            testResult.getTestContext().setAttribute(testResult.getName() + "logSearchUrl", url.get());
        }
    }

    private Map<String, String> getAllKnownProperties(Environment env) {
        Map<String, String> rtn = new HashMap<>();
        if (env instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    LOGGER.info("processing property source ::: " + propertySource.getName());
                    for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                        String value = propertySource.getProperty(key).toString();
                        if (!StringUtils.isEmpty(value)) {
                            rtn.put(key, propertySource.getProperty(key).toString());
                        }
                    }
                }
            }
        }
        return rtn;
    }
}

