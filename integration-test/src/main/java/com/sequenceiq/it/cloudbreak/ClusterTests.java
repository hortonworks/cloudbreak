package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperation;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;

public class ClusterTests extends CloudbreakTest {

    private static final String BLUEPRINT_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final int DESIRED_COUNT = 4;

    private static final String COMPUTE_HOST_GROUP = "compute";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTests.class);

    private CloudProvider cloudProvider;

    public ClusterTests() {
    }

    public ClusterTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider != null) {
            LOGGER.info("cloud provider already set - running from factory test");
            return;
        }
        cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter())[0];
    }

    @Priority(10)
    @Test
    public void testCreateNewCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(BLUEPRINT_HDP26_NAME)),
                "a cluster request");
        given(cloudProvider.aValidStackRequest(), "a stack request");
        when(Stack.post(), "post the stack request");
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
    public void testScaleCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        given(StackOperation.request()
                .withGroupName(COMPUTE_HOST_GROUP)
                .withDesiredCount(DESIRED_COUNT), "a scale request to " + COMPUTE_HOST_GROUP);
        when(StackOperation.scale(), "scale");
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait for availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari");
    }

    @Priority(30)
    @Test
    public void testStopCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        given(StackOperation.request());
        when(StackOperation.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Priority(40)
    @Test
    public void testStartCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        given(StackOperation.request());
        when(StackOperation.start());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "stack has been started");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "ambari check");
    }

    @Priority(50)
    @Test
    public void testTerminateCluster() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }
}
