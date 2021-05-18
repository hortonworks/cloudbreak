package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsNativeConnector implements CloudConnector<List<CloudResource>> {

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
    private AwsNativeSetup awsNativeSetup;

    @Inject
    private AwsTagValidator awsTagValidator;

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
        return Collections.singletonList(awsTagValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return awsCredentialConnector;
    }

    @Override
    public ResourceConnector<List<CloudResource>> resources() {
        return null;
    }

    @Override
    public InstanceConnector instances() {
        return awsInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return null;
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
        return null;
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
    public Platform platform() {
        return awsConstants.platform();
    }

    @Override
    public Variant variant() {
        return AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
    }
}
