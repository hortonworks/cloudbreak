package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Component
public class ClusterHistoryConverter extends AbstractHistoryConverter<Cluster, ClusterHistory> {
    @Override
    public ClusterHistory convert(Cluster entity) {
        ClusterHistory history = new ClusterHistory();
        history.setEntityId(entity.getId());
        history.setName(entity.getName());
        history.setDescription(entity.getDescription());
        history.setBlueprintId(entity.getBlueprint().getId());
        history.setCreationFinished(entity.getCreationFinished());
        history.setCreationStarted(entity.getCreationStarted());
        history.setStatus(entity.getStatus().name());
        history.setStatusReason(entity.getStatusReason());
        history.setUserId(entity.getUser().getId());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return Cluster.class.equals(entity.getClass());
    }
}
