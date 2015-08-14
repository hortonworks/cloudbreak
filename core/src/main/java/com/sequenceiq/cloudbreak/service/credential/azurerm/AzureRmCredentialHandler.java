package com.sequenceiq.cloudbreak.service.credential.azurerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureRmCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;

@Component
public class AzureRmCredentialHandler implements CredentialHandler<AzureRmCredential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRmCredentialHandler.class);

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE_RM;
    }

    @Override
    public AzureRmCredential init(AzureRmCredential credential) {
        return credential;
    }

    @Override
    public boolean delete(AzureRmCredential credential) {
        return true;
    }

    @Override
    public AzureRmCredential update(AzureRmCredential credential) throws Exception {
        return credential;
    }

}
