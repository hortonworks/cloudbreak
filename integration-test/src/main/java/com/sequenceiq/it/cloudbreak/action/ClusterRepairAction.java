package com.sequenceiq.it.cloudbreak.action;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ClusterRepairAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairAction.class);

    public static ClusterRepairAction valid() {
        return new ClusterRepairAction();
    }

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws IOException {
        Log.whenJson(LOGGER, "cluster repair request:\n", getClusterRepairRequest(testDto));

        client.getDefaultClient(testContext)
                .stackV4Endpoint()
                .repairCluster(client.getWorkspaceId(), testDto.getName(), getClusterRepairRequest(testDto),
                        testContext.getActingUserCrn().getAccountId());
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
