package com.sequenceiq.it.cloudbreak;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.cloudbreak.context.CloudbreakITContextConstants;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClient;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClientBuilder;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class AutoscaleTestSuiteInitializer extends AbstractTestNGSpringContextTests {
    private static final Logger LOG = LoggerFactory.getLogger(AutoscaleTestSuiteInitializer.class);

    @Value("${integrationtest.periscope.server}")
    private String defaultPeriscopeServer;

    @Value("${server.contextPath:/as}")
    private String autoscaleRootContextPath;

    @Inject
    private SuiteContext suiteContext;

    private IntegrationTestContext itContext;

    @BeforeSuite(dependsOnGroups = "suiteInit")
    public void initContext(ITestContext testContext) throws Exception {
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        itContext = suiteContext.getItContext(testContext.getSuite().getName());
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters("periscopeServer")
    public void initCloudbreakSuite(@Optional("") String periscopeServer, @Optional("") String caasProtocol, @Optional("") String caasAddress) {
        periscopeServer = StringUtils.hasLength(periscopeServer) ? periscopeServer : defaultPeriscopeServer;
        String userCrn = itContext.getContextParam(IntegrationTestContext.USER_CRN);

        AutoscaleUserCrnClient autoscaleClient = new AutoscaleUserCrnClientBuilder(periscopeServer + autoscaleRootContextPath).build();

        itContext.putContextParam(CloudbreakITContextConstants.AUTOSCALE_CLIENT, autoscaleClient.withCrn(userCrn));
    }

}
