package com.sequenceiq.it.cloudbreak.newway.action;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class ClusterRepairAction implements Action<StackTestDto> {

    public static ClusterRepairAction valid() {
        return new ClusterRepairAction();
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) {

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .repairCluster(client.getWorkspaceId(), testDto.getName(), getClusterRepairRequest(testDto));
        return testDto;
    }

    private ClusterRepairV4Request getClusterRepairRequest(StackTestDto entity) {
        ClusterRepairV4Request clusterRepairRequest = new ClusterRepairV4Request();
        ClusterRepairNodesV4Request clusterRepairNodesRequest = new ClusterRepairNodesV4Request();
        clusterRepairNodesRequest.setIds(List.of(entity.getInstanceId(HostGroupType.MASTER.getName())));
        clusterRepairRequest.setNodes(clusterRepairNodesRequest);
        return clusterRepairRequest;
    }
}
