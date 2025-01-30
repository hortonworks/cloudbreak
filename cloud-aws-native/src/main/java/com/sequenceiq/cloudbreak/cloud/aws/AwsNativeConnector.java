package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.cloud.PublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.ScriptResources;
import com.sequenceiq.cloudbreak.cloud.SecretConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsAuthenticator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsIdentityService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsNoSqlConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsScriptResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSecretsManagerConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsStorageValidator;
import com.sequenceiq.cloudbreak.cloud.aws.metadata.AwsNativeMetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.validator.AwsGatewaySubnetMultiAzValidator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsNativeConnector implements CloudConnector {

    @Inject
    private AwsConstants awsConstants;

    @Inject
    private AwsCredentialConnector awsCredentialConnector;

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsIdentityService awsIdentityService;

    @Inject
    private AwsObjectStorageConnector awsObjectStorageConnector;

    @Inject
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Inject
    private AwsNoSqlConnector awsNoSqlConnector;

    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Inject
    private AwsInstanceConnector awsInstanceConnector;

    @Inject
    private AwsNativeResourceVolumeConnector resourceVolumeConnector;

    @Inject
    private AwsNativeSetup awsNativeSetup;

    @Inject
    private AwsTagValidator awsTagValidator;

    @Inject
    private AwsGatewaySubnetMultiAzValidator awsGatewaySubnetMultiAzValidator;

    @Inject
    private AwsNativeMetadataCollector nativeMetadataCollector;

    @Inject
    private AwsNativeResourceConnector awsNativeResourceConnector;

    @Inject
    private AwsStorageValidator awsStorageValidator;

    @Inject
    private AwsSecretsManagerConnector awsSecretsManagerConnector;

    @Inject
    private AwsEncryptionResources awsEncryptionResources;

    @Inject
    private AwsScriptResources awsScriptResources;

    @Inject
    private AwsNativeAvailabilityZoneConnector awsNativeAvailabilityZoneConnector;

    @Override
    public Authenticator authentication() {
        return awsAuthenticator;
    }

    @Override
    public Setup setup() {
        return awsNativeSetup;
    }

    @Override
    public List<Validator> validators(ValidatorType validatorType) {
        if (ValidatorType.IMAGE.equals(validatorType)) {
            return List.of();
        }
        return List.of(awsTagValidator, awsGatewaySubnetMultiAzValidator, awsStorageValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return awsCredentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return awsNativeResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return awsInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return nativeMetadataCollector;
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
    public NetworkConnector networkConnector() {
        return null;
    }

    @Override
    public EncryptionResources encryptionResources() {
        return awsEncryptionResources;
    }

    @Override
    public AvailabilityZoneConnector availabilityZoneConnector() {
        return awsNativeAvailabilityZoneConnector;
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
    public SecretConnector secretConnector() {
        return awsSecretsManagerConnector;
    }

    @Override
    public ScriptResources scriptResources() {
        return awsScriptResources;
    }

    @Override
    public Platform platform() {
        return awsConstants.platform();
    }

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }

}
