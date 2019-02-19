package com.sequenceiq.it.cloudbreak;

import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;

public class RecoveryTests extends ClusterTests {

    @Test(dataProvider = "providernameblueprintimage", priority = 15)
    public void testManualRecovery(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName));
        when(Stack.makeNodeUnhealthy(HostGroupType.WORKER.getName(), 1));
        when(Stack.get());
        then(Stack.assertThis((stack, integrationTestContext) -> {
            var instanceGroupList = stack.getResponse().getInstanceGroups()
                    .stream()
                    .filter(resp -> resp.getName().equals(HostGroupType.WORKER.getName()))
                    .collect(Collectors.toList());
            var nodeList = instanceGroupList.get(0).getMetadata()
                    .stream()
                    .filter(resp -> "UNHEALTHY".equals(resp.getState()))
                    .collect(Collectors.toList());
            Assert.assertEquals(1, instanceGroupList.size(), "Hostgroup list size does not match.");
            Assert.assertEquals(1, nodeList.size(), "Host metadata list size does not match.");
        }));
        when(Stack.repair(HostGroupType.WORKER.getName()));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 15)
    public void testAutoRecovery(CloudProvider cloudProvider, String clusterName, String clusterDefinitionName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName));
        when(Stack.makeNodeUnhealthy(HostGroupType.WORKER.getName(), 1));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());
    }
}
