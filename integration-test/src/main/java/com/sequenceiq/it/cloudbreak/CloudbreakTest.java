package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.responses.UserProfileV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;

public class CloudbreakTest extends GherkinTest {

    public static final String INTEGRATIONTEST_CLOUDBREAK_SERVER = "INTEGRATIONTEST_CLOUDBREAK_SERVER";

    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String IMAGE_CATALOG_MOCK_SERVER_ROOT = "IMAGE_CATALOG_MOCK_SERVER_ROOT";

    public static final String CLOUDBREAK_SERVER_INTERNAL_ROOT = "CLOUDBREAK_SERVER_INTERNAL_ROOT";

    public static final String AUTOSCALE_CLIENT_ID = "AUTOSCALE_CLIENTID";

    public static final String AUTOSCALE_SECRET = "AUTOSCALE_SECRET";

    public static final String ACCESS_KEY = "ACCESS_KEY";

    public static final String SECRET_KEY = "SECRET_KEY";

    public static final String USER_CRN = "USER_CRN";

    public static final String USER_NAME = "USER_NAME";

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

    @Value("${integrationtest.user.accesskey:}")
    private String accesskey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretkey;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.user.name:}")
    private String userName;

    @Value("${mock.imagecatalog.server:localhost}")
    private String mockImageCatalogAddr;

    @Value("${integrationtest.uaa.autoscale.clientId:periscope}")
    private String autoscaleUaaClientId;

    @Value("${integrationtest.uaa.autoscale.clientSecret}")
    private String autoscaleUaaClientSecret;

    @Inject
    private Environment environment;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

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
        checkNonEmpty("integrationtest.user.accesskey", accesskey);
        checkNonEmpty("integrationtest.user.secretkey", secretkey);

        testContext.putContextParam(CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testContext.putContextParam(ACCESS_KEY, accesskey);
        testContext.putContextParam(SECRET_KEY, secretkey);
        testContext.putContextParam(USER_CRN, userCrn);
        testContext.putContextParam(USER_NAME, userName);

        testContext.putContextParam(IMAGE_CATALOG_MOCK_SERVER_ROOT, mockImageCatalogAddr);

        testContext.putContextParam(AUTOSCALE_CLIENT_ID, autoscaleUaaClientId);
        testContext.putContextParam(AUTOSCALE_SECRET, autoscaleUaaClientSecret);

        testParameter.put(INTEGRATIONTEST_CLOUDBREAK_SERVER, server + cbRootContextPath);
        testParameter.put(ACCESS_KEY, accesskey);
        testParameter.put(SECRET_KEY, secretkey);
        testParameter.put(USER_CRN, userCrn);
        testParameter.put(USER_NAME, userName);

        LOGGER.info(" Default user details in test parameters:: \nACCESS_KEY: {} \nSECRET_KEY: {} \nUSER_CRN: {} \nUSER_NAME: {} ",
                testParameter.get(ACCESS_KEY), testParameter.get(SECRET_KEY), testParameter.get(USER_CRN), testParameter.get(USER_NAME));

        try {
            CloudbreakClient client = CloudbreakClient.created(regionAwareInternalCrnGeneratorFactory.iam());
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
                        if (StringUtils.isNotBlank(value)) {
                            rtn.put(key, propertySource.getProperty(key).toString());
                        }
                    }
                }
            }
        }
        return rtn;
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException(String.format("Following variable must be set whether as environment variables or (test) application.yml:: %s",
                    name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}

