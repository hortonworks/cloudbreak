package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperation;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider;
import org.testng.annotations.Test;

public class UpgradeTests extends CloudbreakTest {

    public static final int DESIRED_COUNT = 5;

    private CloudProvider cloudProvider;

    public UpgradeTests() {
        this.cloudProvider = new GcpCloudProvider();
    }

    @Test
    public void testGetClusterExistingAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Stack.request().withName(cloudProvider.getClusterDefaultName()));
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Test
    public void testScaleExistingClusterAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Stack.request()
                .withName(cloudProvider.getClusterDefaultName()));
        given(StackOperation.request()
                .withGroupName("host_group_slave_1")
                .withDesiredCount(DESIRED_COUNT));
        when(StackOperation.scale());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Test
    public void testStopExistingClusterAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Stack.request()
                .withName(cloudProvider.getClusterDefaultName()));
        given(StackOperation.request());
        when(StackOperation.stop());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackStoppedStatus());
    }

    @Test
    public void testStartExistingClusterAfterUpgrade() throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Stack.request()
                .withName(cloudProvider.getClusterDefaultName()));
        given(StackOperation.request());
        when(StackOperation.start());
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.checkClusterHasAmbariRunning());
    }

    @Test
    public void terminateCluster() throws Exception {
        if ("false".equals(TestParameter.get("cleanUp"))) {
            return;
        }
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Stack.request()
                .withName(cloudProvider.getClusterDefaultName()));
        given(cloudProvider.aValidStackIsCreated());
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted());
    }
}
