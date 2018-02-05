package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperation;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.AwsCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class ClusterTests extends CloudbreakTest {

    public static final String BLUEPRINT_HDP26_EDWANALYTICS_NAME = "EDW-Analytics: Apache Hive 2 LLAP, Apache Zeppelin 0.7.0";

    private static final int DESIRED_COUNT = 4;

    private static final String COMPUTE_HOST_GROUP = "compute";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTests.class);

    private CloudProvider cloudProvider;

    public ClusterTests() {
        this.cloudProvider = new AwsCloudProvider(getTestParameter());
    }

    public ClusterTests(CloudProvider cp, TestParameter tp) {
        this.cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters({ "provider" })
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (this.cloudProvider != null) {
            LOGGER.info("cloud provider already set - running from factory test");
            return;
        }
        this.cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter())[0];
    }

    @Priority(10)
    @Test
    public void testCreateNewCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_EDWANALYTICS_NAME)));
        given(cloudProvider.aValidStackRequest());
        when(Stack.post());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Priority(20)
    @Test
    public void testScaleCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        given(StackOperation.request()
                .withGroupName(COMPUTE_HOST_GROUP)
                .withDesiredCount(DESIRED_COUNT));
        when(StackOperation.scale());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Priority(30)
    @Test
    public void testStopClusterAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        given(StackOperation.request());
        when(StackOperation.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus());
    }

    @Priority(40)
    @Test
    public void testStartClusterAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        given(StackOperation.request());
        when(StackOperation.start());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Priority(50)
    @Test
    public void testTerminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated());
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted());
    }
}
