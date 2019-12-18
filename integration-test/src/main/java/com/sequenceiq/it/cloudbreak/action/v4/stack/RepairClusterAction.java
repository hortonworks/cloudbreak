package com.sequenceiq.it.cloudbreak.action.v4.stack;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairNodesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class RepairClusterAction implements Action<StackTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairClusterAction.class);

    @Override
    public StackTestDto action(TestContext testContext, StackTestDto testDto, CloudbreakClient client) throws Exception {
        ClusterRepairV4Request request = new ClusterRepairV4Request();
        ClusterRepairNodesV4Request node = new ClusterRepairNodesV4Request();
        node.setIds(List.of(testDto.getInstanceId("master")));
        request.setNodes(node);
        Log.whenJson(LOGGER, " Cluster repair request:\n", request);
        client.getCloudbreakClient()
                .stackV4Endpoint()
                .repairCluster(client.getWorkspaceId(), testDto.getName(), request);
        Log.when(LOGGER, " Cluster repair initiated.");

        return testDto;
    }
}
