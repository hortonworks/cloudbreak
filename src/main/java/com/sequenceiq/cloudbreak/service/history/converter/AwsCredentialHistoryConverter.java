package com.sequenceiq.cloudbreak.service.history.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CredentialHistory;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Component
public class AwsCredentialHistoryConverter extends AbstractHistoryConverter<AwsCredential, CredentialHistory> {

    @Override
    public CredentialHistory convert(AwsCredential entity) {
        CredentialHistory history = new CredentialHistory();
        history.setCloudPlatform(entity.getCloudPlatform().name());
        history.setEntityId(entity.getId());
        history.setUserId(entity.getOwner().getId());
        history.setName(entity.getName());
        history.setPublickey(entity.getPublicKey());
        history.setDescription(entity.getDescription());
        history.setName(entity.getCredentialName());
        history.setKeyPairName(entity.getKeyPairName());
        history.setRoleArn(entity.getRoleArn());
        history.setTemporaryAccessKeyId(entity.getTemporaryAwsCredentials().getAccessKeyId());
        return history;
    }

    @Override
    public boolean supportsEntity(ProvisionEntity entity) {
        return AwsCredential.class.equals(entity.getClass());
    }
}
