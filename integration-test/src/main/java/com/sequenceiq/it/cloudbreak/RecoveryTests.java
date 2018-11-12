package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;

public class RecoveryTests extends ClusterTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryTests.class);

    @Test(dataProvider = "providernameblueprintimage", priority = 15)
    public void testManualRecovery(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName));
        when(Stack.makeNodeUnhealthy(HostGroupType.WORKER.getName(), 1));
        when(Stack.get());
        then(Stack.assertThis((stack, integrationTestContext) -> {
            List<HostGroupResponse> hostgroupList = stack.getResponse().getCluster().getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(HostGroupType.WORKER.getName()))
                    .collect(Collectors.toList());
            Assert.assertTrue(hostgroupList.size() == 1);
            List<HostMetadataResponse> nodeList = hostgroupList.get(0).getMetadata().stream()
                    .filter(node -> node.getState().equals("UNHEALTHY"))
                    .collect(Collectors.toList());
            Assert.assertTrue(nodeList.size() == 1);
        }));
        when(Stack.repair(HostGroupType.WORKER.getName()));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 15)
    public void testManualRecoveryWithNodeIds(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName));
        when(Stack.get());
        then(Stack.assertThis((stack, integrationTestContext) -> {
            List<HostGroupResponse> hostgroupList = stack.getResponse().getCluster().getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(HostGroupType.WORKER.getName()))
                    .collect(Collectors.toList());
            Assert.assertTrue(hostgroupList.size() == 1);
        }));

        Set<String> workerGroupInstanceIds =
                Stack.getTestContextStack()
                        .apply(getItContext())
                        .getInstanceMetaData(HostGroupType.WORKER.getName())
                        .stream()
                        .map(InstanceMetaDataJson::getInstanceId)
                        .collect(Collectors.toSet());
        String oneInstance = workerGroupInstanceIds.stream().findFirst().get();
        when(Stack.repairNodes(List.of(oneInstance)));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());

        when(Stack.get());
        then(Stack.assertThis((stack, itContext) -> {
            Set<String> newInstanceIds = stack.getInstanceMetaData(HostGroupType.WORKER.getName())
                    .stream()
                    .map(InstanceMetaDataJson::getInstanceId)
                    .collect(Collectors.toSet());
            Assert.assertEquals(workerGroupInstanceIds.size(), newInstanceIds.size());
            Assert.assertFalse(newInstanceIds.contains(oneInstance));
        }));
    }

    @Test(dataProvider = "providernameblueprintimage", priority = 15)
    public void testAutoRecovery(CloudProvider cloudProvider, String clusterName, String blueprintName, String imageId) throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackCreated()
                .withName(clusterName));
        when(Stack.makeNodeUnhealthy(HostGroupType.WORKER.getName(), 1));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());
    }
}
