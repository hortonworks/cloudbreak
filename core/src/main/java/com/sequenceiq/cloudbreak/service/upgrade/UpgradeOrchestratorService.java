package com.sequenceiq.cloudbreak.service.upgrade;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;

@Service
public class UpgradeOrchestratorService {

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    public void pushSaltState(Long stackId) {
        pushSaltState(stackId, null);
    }

    public void pushSaltState(Long stackId, Long clusterId) {
        InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
        if (clusterId != null) {
            InMemoryStateStore.putCluster(clusterId, PollGroup.POLLABLE);
        }
        clusterServiceRunner.redeployStates(stackId);
        clusterServiceRunner.redeployGatewayPillar(stackId);
    }
}
