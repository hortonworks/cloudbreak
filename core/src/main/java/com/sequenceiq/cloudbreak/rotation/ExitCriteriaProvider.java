package com.sequenceiq.cloudbreak.rotation;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.converter.scheduler.StatusToPollGroupConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@Component
public class ExitCriteriaProvider {

    @Inject
    private StatusToPollGroupConverter statusToPollGroupConverter;

    public ExitCriteriaModel get(StackDto stack) {
        InMemoryStateStore.putStack(stack.getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        InMemoryStateStore.putCluster(stack.getCluster().getId(), statusToPollGroupConverter.convert(stack.getStatus()));
        return ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId());
    }
}
