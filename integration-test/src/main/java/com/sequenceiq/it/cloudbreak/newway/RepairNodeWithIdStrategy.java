package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairNodesRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class RepairNodeWithIdStrategy implements Strategy {
    private final List<String> nodeIds;

    public RepairNodeWithIdStrategy(List<String> nodeIds) {
        this.nodeIds = nodeIds;
    }

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) {
        Stack stack = (Stack) entity;
        StackResponse response = Objects.requireNonNull(stack.getResponse(), "Stack response is null; should get it before");

        CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);

        ClusterRepairNodesRequest clusterRepairNodesRequest = new ClusterRepairNodesRequest();
        clusterRepairNodesRequest.setIds(nodeIds);
        clusterRepairNodesRequest.setDeleteVolumes(false);
        ClusterRepairRequest repairRequest = new ClusterRepairRequest();
        repairRequest.setNodes(clusterRepairNodesRequest);
        client.getCloudbreakClient().stackV3Endpoint().repairClusterInWorkspace(response.getWorkspace().getId(), response.getName(), repairRequest);
    }
}
