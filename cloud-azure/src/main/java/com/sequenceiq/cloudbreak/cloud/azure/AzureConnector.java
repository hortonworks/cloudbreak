package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AzureConnector implements CloudConnector<Map<String, Map<String, Object>>> {

    @Inject
    private AzureAuthenticator azureAuthenticator;

    @Inject
    private AzureSetup azureSetup;

    @Inject
    private AzureTagValidator azureTagValidator;

    @Inject
    private AzureResourceGroupValidator azureResourceGroupValidator;

    @Inject
    private AzureSubnetValidator azureSubnetValidator;

    @Inject
    private AzureStorageValidator azureStorageValidator;

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

    @Inject
    private AzurePlatformResources azurePlatformResources;

    @Inject
    private AzureConstants azureConstants;

    @Override
    public Authenticator authentication() {
        return azureAuthenticator;
    }

    @Override
    public Setup setup() {
        return azureSetup;
    }

    @Override
    public List<Validator> validators() {
        return Arrays.asList(azureTagValidator, azureResourceGroupValidator, azureSubnetValidator, azureStorageValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return azureCredentialConnector;
    }

    @Override
    public ResourceConnector<Map<String, Map<String, Object>>> resources() {
        return azureResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return azureInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return azureMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return azurePlatformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return azurePlatformResources;
    }

    @Override
    public CloudConstant cloudConstant() {
        return azureConstants;
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
