package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CredentialHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Component
public class AzureCredentialHistoryConverter extends AbstractHistoryConverter<AzureCredential, CredentialHistory> {

    @Override
    public CredentialHistory convert(AzureCredential entity) {
        CredentialHistory history = new CredentialHistory();
        history.setCloudPlatform(entity.getCloudPlatform().name());
        history.setId(entity.getId());
        history.setUserId(entity.getAzureCredentialOwner().getId());
        history.setName(entity.getCredentialName());
        history.setJks(entity.getJks());
        history.setName(entity.getName());
        history.setUserId(entity.getOwner().getId());
        history.setSubscriptionid(entity.getSubscriptionId());
        history.setDescription(entity.getDescription());
        history.setPublickey(entity.getPublicKey());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return AzureCredential.class.equals(entity.getClass());
    }
}
