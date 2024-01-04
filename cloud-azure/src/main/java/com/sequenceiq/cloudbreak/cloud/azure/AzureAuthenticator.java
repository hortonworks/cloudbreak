package com.sequenceiq.cloudbreak.cloud.azure;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AzureAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAuthenticator.class);

    @Inject
    private AzureClientService azureClient;

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        try {
            LOGGER.debug("Authenticating to Azure ...");
            return azureClient.createAuthenticatedContext(cloudContext, cloudCredential);
        } catch (Exception e) {
            throw new CloudConnectorException("Could not authenticate to Azure!", e);
        }
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }
}
