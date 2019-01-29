package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.it.IntegrationTestContext;

public class RepairNodeStrategy implements Strategy {
    private final String hostgroup;

    public RepairNodeStrategy(String hostgroup) {
        this.hostgroup = hostgroup;
    }

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        Stack stack = (Stack) entity;
        var response = Objects.requireNonNull(stack.getResponse(), "Stack response is null; should get it before");
        Long workspaceId = Objects.requireNonNull(response.getWorkspace().getId());
        String name = Objects.requireNonNull(response.getCluster().getName());

        CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);

        ClusterRepairV4Request repairRequest = new ClusterRepairV4Request();
        repairRequest.setHostGroups(List.of(hostgroup));
        client.getCloudbreakClient().stackV4Endpoint().repairCluster(workspaceId, name, repairRequest);
    }
}
