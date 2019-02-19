package com.sequenceiq.it.cloudbreak.test;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakMockClusterTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackOperationEntity;

public class MockScaleClusterTest extends CloudbreakMockClusterTest {
    @Test
    public void testScaleCluster() throws Exception {
        String clusterDefinitionName = "Data Science: Apache Spark 2, Apache Zeppelin";

        String clusterName = "mockcluster22";

        String hostgroupName = "compute";

        int desiredCount = 5;

        given(Cluster.request()
                        .withAmbariRequest(getMockProvider().ambariRequestWithBlueprintName(clusterDefinitionName)),
                "a cluster request");
        given(getMockProvider().aValidStackCreated()
                .withName(clusterName), "a stack request");
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability before scale");
        given(StackOperationEntity.request()
                .withGroupName(hostgroupName)
                .withDesiredCount(desiredCount), "a scale request to " + hostgroupName);
        when(StackOperationEntity.scale(), "scale");
        when(Stack.get());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
    }
}
