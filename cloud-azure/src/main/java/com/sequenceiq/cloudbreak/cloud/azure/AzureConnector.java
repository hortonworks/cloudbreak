package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.AvailabilityZoneConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.IdentityService;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.NoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.azure.util.RegionUtil;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureFileSystemValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureStorageValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureSubnetValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureTagValidator;
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
    private AzureSubnetValidator azureSubnetValidator;

    @Inject
    private AzureStorageValidator azureStorageValidator;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureFileSystemValidator azureFileSystemValidator;

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

    @Inject
    private AzureNetworkConnector azureNetworkConnector;

    @Inject
    private AzureIdentityService azureIdentityService;

    @Inject
    private AzureObjectStorageConnector azureObjectStorageConnector;

    @Inject
    private AzureNoSqlConnector azureNoSqlConnector;

    @Inject
    private AzureEncryptionResources azureEncryptionResources;

    @Inject
    private AzureAvailabilityZoneConnector azureAvailabilityZoneConnector;

    @Override
    public Authenticator authentication() {
        return azureAuthenticator;
    }

    @Override
    public Setup setup() {
        return azureSetup;
    }

    @Override
    public List<Validator> validators(ValidatorType validatorType) {
        if (ValidatorType.IMAGE.equals(validatorType)) {
            return List.of(azureImageFormatValidator);
        }
        return Arrays.asList(azureTagValidator, azureSubnetValidator, azureStorageValidator, azureImageFormatValidator, azureFileSystemValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return azureCredentialConnector;
    }

    @Override
    public ResourceConnector resources() {
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

    @Override
    public NetworkConnector networkConnector() {
        return azureNetworkConnector;
    }

    @Override
    public String regionToDisplayName(String region) {
        return RegionUtil.findByLabelOrName(region).label();
    }

    @Override
    public String displayNameToRegion(String displayName) {
        return RegionUtil.findByLabelOrName(displayName).name();
    }

    @Override
    public IdentityService identityService() {
        return azureIdentityService;
    }

    @Override
    public ObjectStorageConnector objectStorage() {
        return azureObjectStorageConnector;
    }

    @Override
    public NoSqlConnector noSql() {
        return azureNoSqlConnector;
    }

    @Override
    public EncryptionResources encryptionResources() {
        return azureEncryptionResources;
    }

    @Override
    public AvailabilityZoneConnector availabilityZoneConnector() {
        return azureAvailabilityZoneConnector;
    }

}
