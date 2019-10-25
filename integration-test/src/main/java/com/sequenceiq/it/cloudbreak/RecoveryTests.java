package com.sequenceiq.it.cloudbreak;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
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
            List<InstanceMetaDataJson> nodeList = stack.getInstanceGroups().get(0).getMetadata().stream()
                    .filter(node -> node.getInstanceStatus().equals(InstanceStatus.SERVICES_UNHEALTHY))
                    .collect(Collectors.toList());
            Assert.assertTrue(nodeList.size() == 1);
        }));
        when(Stack.repair(HostGroupType.WORKER.getName()));
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus());
        then(Stack.waitAndCheckClusterIsAvailable());
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
