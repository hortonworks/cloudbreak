package com.sequenceiq.cloudbreak.service.history.converter;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackHistory;

public class StackHistoryConverter extends AbstractHistoryConverter<Stack, StackHistory> {
    @Override
    public StackHistory convert(Stack entity) {
        StackHistory stackHistory = new StackHistory();
        stackHistory.setName(entity.getName());
        stackHistory.setEntityId(entity.getId());
        stackHistory.setAmbariIp(entity.getAmbariIp());
        stackHistory.setClusterId(entity.getCluster().getId());
        stackHistory.setCredentialId(entity.getCredential().getId());
        stackHistory.setHash(entity.getHash());
        stackHistory.setMetadataReady(entity.isMetadataReady());
        stackHistory.setNodeCount(entity.getNodeCount());
        stackHistory.setStackCompleted(entity.isStackCompleted());
        stackHistory.setStatus(entity.getStatus().name());
        stackHistory.setStatusReason(entity.getStatusReason());
        stackHistory.setTemplateId(entity.getTemplate().getId());
        stackHistory.setTerminated(entity.getTerminated());
        stackHistory.setVersion(entity.getVersion());
        stackHistory.setDescription(entity.getDescription());
        stackHistory.setUserId(entity.getUser().getId());
        return stackHistory;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return Stack.class.equals(entity.getClass());
    }
}