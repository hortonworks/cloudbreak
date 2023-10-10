package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Locale;
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
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.util.TestParameter;

public class CloudbreakTest extends AbstractTestNGSpringContextTests {

    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String IMAGE_CATALOG_MOCK_SERVER_ROOT = "IMAGE_CATALOG_MOCK_SERVER_ROOT";

    public static final String CLOUDBREAK_SERVER_INTERNAL_ROOT = "CLOUDBREAK_SERVER_INTERNAL_ROOT";

    public static final String ACCESS_KEY = "ACCESS_KEY";

    public static final String SECRET_KEY = "SECRET_KEY";

    public static final String USER_CRN = "USER_CRN";

    public static final String USER_NAME = "USER_NAME";

    public static final String WORKLOAD_USER_NAME = "WORKLOAD_USER_NAME";

    public static final String SECONDARY_ACCESS_KEY = "SECONDARY_ACCESS_KEY";

    public static final String SECONDARY_SECRET_KEY = "SECONDARY_SECRET_KEY";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakTest.class);

    @Value("${integrationtest.user.accesskey:}")
    private String accesskey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretkey;

    @Value("${integrationtest.user.crn:}")
    private String userCrn;

    @Value("${integrationtest.user.name:}")
    private String userName;

    @Value("${integrationtest.user.workloadUserName:}")
    private String workloadUserName;

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
        checkNonEmpty("integrationtest.user.accesskey", accesskey);
        checkNonEmpty("integrationtest.user.secretkey", secretkey);

        testParameter.put(ACCESS_KEY, accesskey);
        testParameter.put(SECRET_KEY, secretkey);
        testParameter.put(USER_CRN, userCrn);
        testParameter.put(USER_NAME, userName);
        testParameter.put(WORKLOAD_USER_NAME, workloadUserName);

        LOGGER.info(" Default user details in test parameters:: \nACCESS_KEY: {} \nSECRET_KEY: {} \nUSER_CRN: {} \nUSER_NAME: {} \nWORKLOAD_USER_NAME: {} ",
                testParameter.get(ACCESS_KEY), testParameter.get(SECRET_KEY), testParameter.get(USER_CRN), testParameter.get(USER_NAME),
                testParameter.get(WORKLOAD_USER_NAME));
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
                    name.replaceAll("\\.", "_").toUpperCase(Locale.ROOT)));
        }
    }
}

