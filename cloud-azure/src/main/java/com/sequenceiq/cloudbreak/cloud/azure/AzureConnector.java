package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AzureConnector implements CloudConnector {

    @Inject
    private AzureAuthenticator azureAuthenticator;

    @Inject
    private AzureSetup azureSetup;

    @Inject
    private AzureTagValidator azureTagValidator;

    @Inject
    private AzureCredentialConnector azureCredentialConnector;

    @Inject
    private AzureResourceConnector azureResourceConnector;

    @Inject
    private AzureInstanceConnector azureInstanceConnector;

    @Inject
    private AzureMetadataCollector azureMetadataCollector;

    @Inject
    private AzurePlatformParameters azurePlatformParameters;

    public Authenticator authentication() {
        return azureAuthenticator;
    }

    public Setup setup() {
        return azureSetup;
    }

    @Override
    public List<Validator> validators() {
        return Collections.singletonList(azureTagValidator);
    }

    public CredentialConnector credentials() {
        return azureCredentialConnector;
    }

    public ResourceConnector resources() {
        return azureResourceConnector;
    }

    public InstanceConnector instances() {
        return azureInstanceConnector;
    }

    public MetadataCollector metadata() {
        return azureMetadataCollector;
    }

    public PlatformParameters parameters() {
        return azurePlatformParameters;
    }

    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    public Variant variant() {
        return AzureConstants.VARIANT;
    }
}
