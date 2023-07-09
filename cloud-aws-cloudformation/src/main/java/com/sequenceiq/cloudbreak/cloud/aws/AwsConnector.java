package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.ConsumptionCalculator;
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
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsIdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsNoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.StorageType;

@Service
public class AwsConnector implements CloudConnector {

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsInstanceConnector awsInstanceConnector;

    @Inject
    private AwsMetadataCollector awsMetadataCollector;

    @Inject
    private AwsCredentialConnector awsCredentialConnector;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private AwsCloudFormationSetup awsSetup;

    @Inject
    private AwsTagValidator awsTagValidator;

    @Inject
    private AwsStackValidator awsStackValidator;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private AwsConstants awsConstants;

    @Inject
    private AwsNetworkConnector awsNetworkConnector;

    @Inject
    private AwsIdentityService awsIdentityService;

    @Inject
    private AwsObjectStorageConnector awsObjectStorageConnector;

    @Inject
    private AwsNoSqlConnector awsNoSqlConnector;

    @Inject
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Inject
    private AwsStorageValidator awsStorageValidator;

    @Inject
    private ResourceVolumeConnector resourceVolumeConnector;

    @Inject
    private List<ConsumptionCalculator> consumptionCalculators;

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    @Override
    public Authenticator authentication() {
        return awsAuthenticator;
    }

    @Override
    public ResourceConnector resources() {
        return awsResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return awsInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return awsMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return awsPlatformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return awsPlatformResources;
    }

    @Override
    public CloudConstant cloudConstant() {
        return awsConstants;
    }

    @Override
    public Setup setup() {
        return awsSetup;
    }

    @Override
    public List<Validator> validators(ValidatorType validatorType) {
        if (ValidatorType.IMAGE.equals(validatorType)) {
            return List.of();
        }
        return Arrays.asList(awsTagValidator, awsStackValidator, awsStorageValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return awsCredentialConnector;
    }

    @Override
    public NetworkConnector networkConnector() {
        return awsNetworkConnector;
    }

    @Override
    public IdentityService identityService() {
        return awsIdentityService;
    }

    @Override
    public ObjectStorageConnector objectStorage() {
        return awsObjectStorageConnector;
    }

    @Override
    public NoSqlConnector noSql() {
        return awsNoSqlConnector;
    }

    @Override
    public PublicKeyConnector publicKey() {
        return awsPublicKeyConnector;
    }

    @Override
    public ResourceVolumeConnector volumeConnector() {
        return resourceVolumeConnector;
    }

    @Override
    public Optional<ConsumptionCalculator> consumptionCalculator(StorageType storageType) {
        return consumptionCalculators.stream()
                .filter(c -> c.storageType().equals(storageType))
                .findFirst();
    }

    @Override
    public EncryptionResources encryptionResources() {
        return null;
    }
}
