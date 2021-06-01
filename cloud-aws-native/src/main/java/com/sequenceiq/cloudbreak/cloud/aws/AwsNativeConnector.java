package com.sequenceiq.cloudbreak.cloud.aws;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsNativeConnector implements CloudConnector<List<CloudResource>> {

    @Inject
    private AwsConstants awsConstants;

    @Override
    public Authenticator authentication() {
        return null;
    }

    @Override
    public Setup setup() {
        return null;
    }

    @Override
    public List<Validator> validators() {
        return null;
    }

    @Override
    public CredentialConnector credentials() {
        return null;
    }

    @Override
    public ResourceConnector<List<CloudResource>> resources() {
        return null;
    }

    @Override
    public InstanceConnector instances() {
        return null;
    }

    @Override
    public MetadataCollector metadata() {
        return null;
    }

    @Override
    public PlatformParameters parameters() {
        return null;
    }

    @Override
    public PlatformResources platformResources() {
        return null;
    }

    @Override
    public CloudConstant cloudConstant() {
        return null;
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
    public String regionToDisplayName(String region) {
        return CloudConnector.super.regionToDisplayName(region);
    }

    @Override
    public String displayNameToRegion(String displayName) {
        return CloudConnector.super.displayNameToRegion(displayName);
    }

    @Override
    public IdentityService identityService() {
        return CloudConnector.super.identityService();
    }

    @Override
    public ObjectStorageConnector objectStorage() {
        return CloudConnector.super.objectStorage();
    }

    @Override
    public NoSqlConnector noSql() {
        return CloudConnector.super.noSql();
    }

    @Override
    public PublicKeyConnector publicKey() {
        return CloudConnector.super.publicKey();
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
