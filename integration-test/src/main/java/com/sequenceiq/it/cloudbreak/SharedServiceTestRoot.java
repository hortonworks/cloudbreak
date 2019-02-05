package com.sequenceiq.it.cloudbreak;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.it.cloudbreak.newway.AttachedClusterStackPostStrategy;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DatalakeCluster;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.RdsConfig;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.ResourceHelper;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;

public abstract class SharedServiceTestRoot extends CloudbreakTest {

    private static final String DATALAKE_CLUSTER_NAME = "autotesting-datalake-cluster-%s";

    private static final String ATTACHED_CLUSTER_NAME = "autotesting-attached-cluster-%s";

    private static final String CLEAN_UP_EXCEPTION_MESSAGE = "Error occured during cleanup: %s";

    private ResourceHelper<?> resourceHelper;

    private CloudProvider cloudProvider;

    private final String rangerConfigNameKey;

    private final String hiveConfigNameKey;

    private final String implementation;

    private String optionalClusterPostfix;

    private final Logger logger;

    protected SharedServiceTestRoot(@Nonnull Logger logger, @Nonnull String implementation, String hiveConfigKey, String rangerConfigKey) {
        this(logger, implementation, hiveConfigKey, rangerConfigKey, null);
    }

    protected SharedServiceTestRoot(@Nonnull Logger logger, @Nonnull String implementation, String hiveConfigKey, String rangerConfigKey,
                                    String optionalClusterPostfix) {
        this.logger = logger;
        this.implementation = implementation;
        hiveConfigNameKey = hiveConfigKey;
        rangerConfigNameKey = rangerConfigKey;
        this.optionalClusterPostfix = optionalClusterPostfix;
    }

    @Priority(10)
    @Test
    public void testADatalakeClusterCreation() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(resourceHelper.aValidHiveDatabase());
        given(resourceHelper.aValidRangerDatabase());
        given(resourceHelper.aValidLdap());
        given(cloudProvider.aValidDatalakeCluster(), "a datalake cluster request");
        given(cloudProvider.aValidStackRequest()
                .withInstanceGroups(cloudProvider.instanceGroups(HostGroupType.MASTER))
                .withName(getDatalakeClusterName()));

        when(Stack.post());

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Priority(20)
    @Test
    public void testClusterAttachedToDatalakeCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(DatalakeCluster.isCreatedWithName(getDatalakeClusterName()));
        given(cloudProvider.aValidAttachedCluster(), "an attached cluster request");
        given(cloudProvider.aValidAttachedStackRequest(getDatalakeClusterName())
                .withName(getAttachedClusterName()).withUserDefinedTags(Collections.emptyMap()));

        when(Stack.post(new AttachedClusterStackPostStrategy()));

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Priority(30)
    @Test
    public void testTerminateAttachedCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(getAttachedClusterName()), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Priority(40)
    @Test
    public void testTerminateDatalakeCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(getDatalakeClusterName()), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @BeforeTest
    public void initialize() {
        cloudProvider = CloudProviderHelper.providerFactory(implementation, getTestParameter());
        resourceHelper = cloudProvider.getResourceHelper();
    }

    @AfterSuite
    public void after() throws Exception {
        cleanUpRdsConfigs();
        cleanUpLdap();
    }

    public void setResourceHelper(ResourceHelper<?> resourceHelper) {
        this.resourceHelper = resourceHelper;
    }

    public void setCloudProvider(CloudProvider cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public void setOptionalClusterPostfix(String optionalClusterPostfix) {
        this.optionalClusterPostfix = optionalClusterPostfix;
    }

    public Logger getLogger() {
        return logger;
    }

    public CloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public ResourceHelper<?> getResourceHelper() {
        return resourceHelper;
    }

    protected String getDatalakeClusterName() {
        return optionalClusterPostfix == null
                ? String.format(DATALAKE_CLUSTER_NAME, implementation)
                : String.format("%s-%s", String.format(DATALAKE_CLUSTER_NAME, implementation), optionalClusterPostfix);
    }

    protected String getAttachedClusterName() {
        return optionalClusterPostfix == null
                ? String.format(ATTACHED_CLUSTER_NAME, implementation)
                : String.format("%s-%s", String.format(ATTACHED_CLUSTER_NAME, implementation), optionalClusterPostfix);
    }

    private void cleanUpLdap() throws Exception {
        given(CloudbreakClient.created());
        when(LdapConfig.getAll());
        then(LdapConfig.assertThis((ldapConfig, testContext) -> {
            var responses = ldapConfig.getResponses();
            for (LdapV4Response response : responses) {
                if (response.getName().equals(resourceHelper.getLdapConfigName())) {
                    try {
                        given(LdapConfig.request().withName(response.getName()));
                        when(LdapConfig.delete());
                    } catch (Exception e) {
                        logger.warn(String.format(CLEAN_UP_EXCEPTION_MESSAGE, e.getMessage()));
                    }
                }
            }
        }));
    }

    private void cleanUpRdsConfigs() throws Exception {
        given(CloudbreakClient.created());
        when(RdsConfig.getAll());
        then(RdsConfig.assertThis((rdsConfig, testContext) -> {
            var responses = rdsConfig.getResponses();
            for (DatabaseV4Response response : responses) {
                if (response.getName().equals(getTestParameter().get(hiveConfigNameKey))
                        || response.getName().equals(getTestParameter().get(rangerConfigNameKey))) {
                    try {
                        given(RdsConfig.request().withName(response.getName()));
                        when(RdsConfig.delete());
                    } catch (Exception e) {
                        logger.warn(String.format(CLEAN_UP_EXCEPTION_MESSAGE, e.getMessage()));
                    }
                }
            }
        }));
    }
}
