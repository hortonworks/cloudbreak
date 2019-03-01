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
import com.sequenceiq.it.cloudbreak.newway.StackOperationEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.priority.Priority;

public class ClusterTestsSimple extends CloudbreakTest {

    private static final String CLUSTER_DEFINITION_HDP26_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final int DESIRED_COUNT = 4;

    private static final String COMPUTE_HOST_GROUP = "compute";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTestsSimple.class);

    private CloudProvider cloudProvider;

    public ClusterTestsSimple() {
    }

    public ClusterTestsSimple(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @Priority(10)
    @Test
    public void testCreateNewCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withUsername(cloudProvider.getUsername())
                        .withPassword(cloudProvider.getPassword())
                .withAmbariRequest(cloudProvider.ambariRequestWithClusterDefinitionName(CLUSTER_DEFINITION_HDP26_NAME)),
                "a cluster request");
        given(cloudProvider.aValidStackRequest(), "a stack request");
        when(Stack.postV3(), "post the stack request");
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
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        given(StackOperationEntity.request()
                .withGroupName(COMPUTE_HOST_GROUP)
                .withDesiredCount(DESIRED_COUNT), "a scale request to " + COMPUTE_HOST_GROUP);
        when(StackOperationEntity.scale(), "scale");
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
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        given(StackOperationEntity.request());
        when(StackOperationEntity.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus(), "stack has been stopped");
    }

    @Priority(40)
    @Test
    public void testStartCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        given(StackOperationEntity.request());
        when(StackOperationEntity.start());
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
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated(), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }
}
