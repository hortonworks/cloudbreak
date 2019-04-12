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

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.logsearch.LogSearchProps;
import com.sequenceiq.it.cloudbreak.newway.logsearch.LogSearchUtil;

@EnableConfigurationProperties(LogSearchProps.class)
public class CloudbreakTest extends GherkinTest {

    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String IDENTITY_URL = "IDENTITY_URL";

    public static final String AUTOSCALE_CLIENT_ID = "AUTOSCALE_CLIENTID";

    public static final String AUTOSCALE_SECRET = "AUTOSCALE_SECRET";

    public static final String USER = "USER";

    public static final String PASSWORD = "PASSWORD";

    public static final String CAAS_PROTOCOL = "CAAS_PROTOCOL";

    public static final String CAAS_ADDRESS = "CAAS_ADDRESS";

    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";

    public static final String SECONDARY_REFRESH_TOKEN = "SECONDARY_REFRESH_TOKEN";

    public static final String WORKSPACE_ID = "WORKSPACE_ID";

    public static final String LOG_SEARCH_QUERY_TYPES = "LOG_SEARCH_QUERY_TYPES";

    public static final String LOG_SEARCH_URL_PREFIX = "LOG_SEARCH_URL_PREFIX";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTest.class);

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.caas.token}")
    private String refreshToken;

    @Value("${integrationtest.caas.secondarytoken:}")
    private String secondaryRefreshToken;

    @Value("${integrationtest.caas.protocol:}")
    private String caasProtocol;

    @Value("${integrationtest.caas.address:}")
    private String caasAddress;

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
        String[] cloudbreakServerSplit = server.split("://");
        if (StringUtils.isEmpty(caasProtocol)) {
            caasProtocol = cloudbreakServerSplit[0];
        }
        if (StringUtils.isEmpty(caasAddress)) {
            caasAddress = cloudbreakServerSplit[1];
        }
        if (StringUtils.isEmpty(refreshToken)) {
            throw new NullPointerException("INTEGRATIONTEST_CAAS_TOKEN should be set");
        }
        testContext.putContextParam(CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testContext.putContextParam(CAAS_PROTOCOL, caasProtocol);
        testContext.putContextParam(CAAS_ADDRESS, caasAddress);
        testContext.putContextParam(REFRESH_TOKEN, refreshToken);
        testContext.putContextParam(SECONDARY_REFRESH_TOKEN, secondaryRefreshToken);
        testContext.putContextParam(LOG_SEARCH_QUERY_TYPES, logSearchProps.getQueryTypes());
        testContext.putContextParam(LOG_SEARCH_URL_PREFIX, logSearchProps.getUrl());

        testContext.putContextParam(IDENTITY_URL, uaaServer);
        testContext.putContextParam(USER, defaultUaaUser);
        testContext.putContextParam(PASSWORD, defaultUaaPassword);
        testContext.putContextParam(AUTOSCALE_CLIENT_ID, autoscaleUaaClientId);
        testContext.putContextParam(AUTOSCALE_SECRET, autoscaleUaaClientSecret);

        testParameter.put("INTEGRATIONTEST_CLOUDBREAK_SERVER", server + cbRootContextPath);

        try {
            CloudbreakClient client = CloudbreakClient.created();
            client.create(testContext);

            UserProfileV4Response profile = CloudbreakClient.getSingletonCloudbreakClient().userProfileV4Endpoint().get();
            LogSearchUtil.addQueryModelForLogSearchUrlToContext(testContext, LogSearchUtil.LOG_SEARCH_CBOWNER_ID,
                    LogSearchUtil.LOG_SEARCH_CBOWNER_QUERY_TYPE, profile.getUsername());

            setWorkspaceByUserProfile(testContext, profile);
        } catch (Exception exception) {
            LOGGER.info("CloudbreakClient error", exception);
        }
    }

    private void setWorkspaceByUserProfile(IntegrationTestContext testContext, UserProfileV4Response profile) {
        LOGGER.info("put WORKSPACE_ID to context: {}", 0L);
        testContext.putContextParam(WORKSPACE_ID, 0L);
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

