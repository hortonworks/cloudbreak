package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class RepairNodeStrategy implements Strategy {
    private final String hostgroup;

    public RepairNodeStrategy(String hostgroup) {
        this.hostgroup = hostgroup;
    }

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) {
        Stack stack = (Stack) entity;
        StackResponse response = Objects.requireNonNull(stack.getResponse(), "Stack response is null; should get it before");
        Long id = Objects.requireNonNull(response.getId());

        CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(integrationTestContext);

        ClusterRepairRequest repairRequest = new ClusterRepairRequest();
        repairRequest.setHostGroups(List.of(hostgroup));
        client.getCloudbreakClient().clusterEndpoint().repairCluster(id, repairRequest);
    }
}
