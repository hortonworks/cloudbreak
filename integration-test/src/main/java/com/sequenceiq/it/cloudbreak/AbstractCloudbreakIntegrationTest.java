package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.CollectionUtils;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.api.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.EventEndpoint;
import com.sequenceiq.cloudbreak.api.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.StackEndpoint;
import com.sequenceiq.cloudbreak.api.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.UserEndpoint;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public abstract class AbstractCloudbreakIntegrationTest extends AbstractTestNGSpringContextTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest.class);
    private IntegrationTestContext itContext;

    private BlueprintEndpoint blueprintEndpoint;
    private AccountPreferencesEndpoint accountPreferencesEndpoint;
    private ClusterEndpoint clusterEndpoint;
    private ConnectorEndpoint connectorEndpoint;
    private CredentialEndpoint credentialEndpoint;
    private EventEndpoint eventEndpoint;
    private NetworkEndpoint networkEndpoint;
    private RecipeEndpoint recipeEndpoint;
    private SecurityGroupEndpoint securityGroupEndpoint;
    private StackEndpoint stackEndpoint;
    private SubscriptionEndpoint subscriptionEndpoint;
    private TemplateEndpoint templateEndpoint;
    private UsageEndpoint usageEndpoint;
    private UserEndpoint userEndpoint;

    @Inject
    private SuiteContext suiteContext;

    @BeforeClass
    public void checkContextParameters(ITestContext testContext) throws Exception {
        itContext = suiteContext.getItContext(testContext.getSuite().getName());
        if (itContext.getContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, Boolean.class)
                && !CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class))) {
            throw new SkipException("Suite contains failed tests, the remaining tests will be skipped.");
        }
        blueprintEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_BLUEPRINT, BlueprintEndpoint.class);
        accountPreferencesEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_ACCOUNTPREFERENCES, AccountPreferencesEndpoint.class);
        clusterEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_CLUSTER, ClusterEndpoint.class);
        connectorEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_CONNECTOR, ConnectorEndpoint.class);
        credentialEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_CREDENTIAL, CredentialEndpoint.class);
        eventEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_EVENT, EventEndpoint.class);
        networkEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_NETWORK, NetworkEndpoint.class);
        recipeEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_RECIPE, RecipeEndpoint.class);
        securityGroupEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_SECURITYGROUP, SecurityGroupEndpoint.class);
        stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_STACK, StackEndpoint.class);
        subscriptionEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_SUBSCRIPTION, SubscriptionEndpoint.class);
        templateEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_TEMPLATE, TemplateEndpoint.class);
        usageEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_USAGE, UsageEndpoint.class);
        userEndpoint = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_USER, UserEndpoint.class);

        Assert.assertNotNull(blueprintEndpoint, "BlueprintEndpoint cannot be null.");
        Assert.assertNotNull(accountPreferencesEndpoint, "AccountPreferencesEndpoint cannot be null.");
        Assert.assertNotNull(clusterEndpoint, "ClusterEndpoint cannot be null.");
        Assert.assertNotNull(connectorEndpoint, "ConnectorEndpoint cannot be null.");
        Assert.assertNotNull(credentialEndpoint, "CredentialEndpoint cannot be null.");
        Assert.assertNotNull(eventEndpoint, "EventEndpoint cannot be null.");
        Assert.assertNotNull(networkEndpoint, "NetworkEndpoint cannot be null.");
        Assert.assertNotNull(recipeEndpoint, "RecipeEndpoint cannot be null.");
        Assert.assertNotNull(securityGroupEndpoint, "SecurityGroupEndpoint cannot be null.");
        Assert.assertNotNull(stackEndpoint, "StackEndpoint cannot be null.");
        Assert.assertNotNull(subscriptionEndpoint, "SubscriptionEndpoint cannot be null.");
        Assert.assertNotNull(templateEndpoint, "TemplateEndpoint cannot be null.");
        Assert.assertNotNull(usageEndpoint, "UsageEndpoint cannot be null.");
        Assert.assertNotNull(userEndpoint, "UserEndpoint cannot be null.");
    }

    @AfterMethod
    @Parameters("sleepTime")
    public void sleepAfterTest(@Optional("0") int sleepTime) {
        if (sleepTime > 0) {
            LOGGER.info("Sleeping {}ms after test...", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (Exception ex) {
                LOGGER.warn("Ex during sleep!");
            }
        }
    }

    @AfterMethod
    public void checkResult(ITestContext testContext, ITestResult testResult) {
        if (testResult.getStatus() == ITestResult.FAILURE) {
            List<String> failedTests = itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class);
            if (failedTests == null) {
                failedTests = new ArrayList<>();
                itContext.putContextParam(CloudbreakITContextConstants.FAILED_TESTS, failedTests);
            }
            failedTests.add(testContext.getName());
        }
    }

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected BlueprintEndpoint getBlueprintEndpoint() {
        return blueprintEndpoint;
    }

    protected AccountPreferencesEndpoint getAccountPreferencesEndpoint() {
        return accountPreferencesEndpoint;
    }

    protected ClusterEndpoint getClusterEndpoint() {
        return clusterEndpoint;
    }

    protected ConnectorEndpoint getConnectorEndpoint() {
        return connectorEndpoint;
    }

    protected CredentialEndpoint getCredentialEndpoint() {
        return credentialEndpoint;
    }

    protected EventEndpoint getEventEndpoint() {
        return eventEndpoint;
    }

    protected NetworkEndpoint getNetworkEndpoint() {
        return networkEndpoint;
    }

    protected RecipeEndpoint getRecipeEndpoint() {
        return recipeEndpoint;
    }

    protected SecurityGroupEndpoint getSecurityGroupEndpoint() {
        return securityGroupEndpoint;
    }

    protected StackEndpoint getStackEndpoint() {
        return stackEndpoint;
    }

    protected SubscriptionEndpoint getSubscriptionEndpoint() {
        return subscriptionEndpoint;
    }

    protected TemplateEndpoint getTemplateEndpoint() {
        return templateEndpoint;
    }

    protected UsageEndpoint getUsageEndpoint() {
        return usageEndpoint;
    }

    protected UserEndpoint getUserEndpoint() {
        return userEndpoint;
    }
}
