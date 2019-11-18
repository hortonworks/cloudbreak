package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;

public class CloudbreakTest extends GherkinTest {

    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String CLOUDBREAK_SERVER_INTERNAL_ROOT = "CLOUDBREAK_SERVER_INTERNAL_ROOT";

    public static final String AUTOSCALE_CLIENT_ID = "AUTOSCALE_CLIENTID";

    public static final String AUTOSCALE_SECRET = "AUTOSCALE_SECRET";

    public static final String ACCESS_KEY = "ACCESS_KEY";

    public static final String SECRET_KEY = "SECRET_KEY";

    public static final String SECONDARY_ACCESS_KEY = "SECONDARY_ACCESS_KEY";

    public static final String SECONDARY_SECRET_KEY = "SECONDARY_SECRET_KEY";

    public static final String WORKSPACE_ID = "WORKSPACE_ID";

    public static final String LOG_SEARCH_QUERY_TYPES = "LOG_SEARCH_QUERY_TYPES";

    public static final String LOG_SEARCH_URL_PREFIX = "LOG_SEARCH_URL_PREFIX";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTest.class);

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.user.accesskey}")
    private String accesskey;

    @Value("${integrationtest.user.secretkey}")
    private String secretkey;

    @Value("${integrationtest.uaa.autoscale.clientId:periscope}")
    private String autoscaleUaaClientId;

    @Value("${integrationtest.uaa.autoscale.clientSecret}")
    private String autoscaleUaaClientSecret;

    @Inject
    private Environment environment;

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
        if (StringUtils.isEmpty(accesskey)) {
            throw new NullPointerException("INTEGRATIONTEST_USER_ACCESSKEY should be set");
        }
        if (StringUtils.isEmpty(secretkey)) {
            throw new NullPointerException("INTEGRATIONTEST_USER_SECRETKEY should be set");
        }
        testContext.putContextParam(CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testContext.putContextParam(ACCESS_KEY, accesskey);
        testContext.putContextParam(SECRET_KEY, secretkey);

        testContext.putContextParam(AUTOSCALE_CLIENT_ID, autoscaleUaaClientId);
        testContext.putContextParam(AUTOSCALE_SECRET, autoscaleUaaClientSecret);

        testParameter.put("INTEGRATIONTEST_CLOUDBREAK_SERVER", server + cbRootContextPath);

        try {
            CloudbreakClient client = CloudbreakClient.created();
            client.create(testContext);

            UserProfileV4Response profile = CloudbreakClient.getSingletonCloudbreakClient().userProfileV4Endpoint().get();

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

