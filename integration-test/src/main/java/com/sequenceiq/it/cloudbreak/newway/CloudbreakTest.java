package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;

@Listeners({CustomInvocationHandler.class})
public class CloudbreakTest extends GherkinTest {
    public static final String CLOUDBREAK_SERVER_ROOT = "CLOUDBREAK_SERVER_ROOT";

    public static final String IDENTITY_URL = "IDENTITY_URL";

    public static final String USER = "USER";

    public static final String PASSWORD = "PASSWORD";

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

    public CloudbreakTest() {
        try {
            super.springTestContextBeforeTestClass();
            super.springTestContextPrepareTestInstance();
        } catch (Exception e) {
        }

        LOGGER.info("CloudbreakTest default values ::: ");
        IntegrationTestContext testContext = getItContext();
        testContext.putContextParam(CLOUDBREAK_SERVER_ROOT, server + cbRootContextPath);
        testContext.putContextParam(IDENTITY_URL, uaaServer);
        testContext.putContextParam(USER, defaultUaaUser);
        testContext.putContextParam(PASSWORD, defaultUaaPassword);
    }

    @BeforeTest
    public void digestParameters(ITestContext testngContext) {
        TestParameter.init();

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("application.yml"));
        yaml.getObject().entrySet().stream().forEach(
                entry -> TestParameter.put((String) entry.getKey(), entry.getValue().toString())
        );

        LOGGER.info("CloudbreakTest load test parameters ::: ");
        if (testngContext != null) {
            TestParameter.putAll(testngContext.getCurrentXmlTest().getAllParameters());
        }
    }
}
