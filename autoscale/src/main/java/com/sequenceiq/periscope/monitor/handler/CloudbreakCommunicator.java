package com.sequenceiq.periscope.monitor.handler;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;

@Service
public class CloudbreakCommunicator {

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private RequestLogging requestLogging;

    public StackV4Response getByCrn(String stackCrn) {
        return cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint().get(stackCrn);
    }

    public void decommissionInstancesForCluster(Cluster cluster, List<String> decommissionNodeIds) {
        requestLogging.logResponseTime(() -> {
            cloudbreakInternalCrnClient.withInternalCrn().autoscaleEndpoint()
                    .decommissionInstancesForClusterCrn(cluster.getStackCrn(),
                            cluster.getClusterPertain().getWorkspaceId(),
                            decommissionNodeIds, false);
            return Optional.empty();
        }, String.format("DecommissionInstancesForCluster query for cluster crn %s, NodeIds %s",
                cluster.getStackCrn(), decommissionNodeIds));
    }
}
