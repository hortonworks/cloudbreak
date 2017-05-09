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
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.periscope.client.AutoscaleClient;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class AutoscaleTestSuiteInitializer extends AbstractTestNGSpringContextTests {
    private static final Logger LOG = LoggerFactory.getLogger(CloudbreakTestSuiteInitializer.class);

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
    @Parameters({"periscopeServer"})
    public void initCloudbreakSuite(@Optional("") String periscopeServer) {
        periscopeServer = StringUtils.hasLength(periscopeServer) ? periscopeServer : defaultPeriscopeServer;
        String identity = itContext.getContextParam(IntegrationTestContext.IDENTITY_URL);
        String user = itContext.getContextParam(IntegrationTestContext.AUTH_USER);
        String password = itContext.getContextParam(IntegrationTestContext.AUTH_PASSWORD);

        AutoscaleClient autoscaleClient = new AutoscaleClient.AutoscaleClientBuilder(periscopeServer + autoscaleRootContextPath,
                        identity, "cloudbreak_shell").withCertificateValidation(false).withDebug(true).withCredential(user, password).build();

        itContext.putContextParam(CloudbreakITContextConstants.AUTOSCALE_CLIENT, autoscaleClient);
    }

}
