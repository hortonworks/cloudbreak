package com.sequenceiq.cloudbreak.service.credential.azure;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Component
public class AzureCredentialHandler implements CredentialHandler<AzureCredential> {
    @Inject
    private AzureStackUtil azureStackUtil;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public AzureCredential init(AzureCredential azureCredential) {
        throw new UnsupportedOperationException("Unsupported operation: init()");
    }

    @Override
    public boolean delete(AzureCredential credential) {
        return true;
    }

    @Override
    public AzureCredential update(AzureCredential credential) throws Exception {
        return azureStackUtil.refreshCerfile(credential);
    }
}
