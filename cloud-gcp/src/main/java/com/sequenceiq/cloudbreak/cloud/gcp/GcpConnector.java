package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.Collections;
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
import com.sequenceiq.cloudbreak.cloud.ObjectStorageConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.ScriptResources;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.gcp.setup.GcpProvisionSetup;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class GcpConnector implements CloudConnector {

    @Inject
    private GcpAuthenticator authenticator;

    @Inject
    private GcpProvisionSetup provisionSetup;

    @Inject
    private GcpTagValidator gcpTagValidator;

    @Inject
    private GcpInstanceConnector instanceConnector;

    @Inject
    private GcpResourceConnector resourceConnector;

    @Inject
    private GcpPlatformResources gcpPlatformResources;

    @Inject
    private GcpCredentialConnector gcpCredentialConnector;

    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Inject
    private GcpMetadataCollector metadataCollector;

    @Inject
    private GcpNetworkConnector gcpNetworkConnector;

    @Inject
    private GcpConstants gcpConstants;

    @Inject
    private GcpIdentityService gcpIdentityService;

    @Inject
    private GcpObjectStorageConnector gcpObjectStorageConnector;

    @Inject
    private GcpAvailabilityZoneConnector gcpAvailabilityZoneConnector;

    @Inject
    private GcpScriptResources gcpScriptResources;

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public Setup setup() {
        return provisionSetup;
    }

    @Override
    public List<Validator> validators(ValidatorType validatorType) {
        if (ValidatorType.IMAGE.equals(validatorType)) {
            return List.of();
        }
        return Collections.singletonList(gcpTagValidator);
    }

    @Override
    public CredentialConnector credentials() {
        return gcpCredentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return resourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return instanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return metadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return gcpPlatformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return gcpPlatformResources;
    }

    @Override
    public CloudConstant cloudConstant() {
        return gcpConstants;
    }

    @Override
    public IdentityService identityService() {
        return gcpIdentityService;
    }

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }

    @Override
    public NetworkConnector networkConnector() {
        return gcpNetworkConnector;
    }

    @Override
    public ObjectStorageConnector objectStorage() {
        return gcpObjectStorageConnector;
    }

    @Override
    public ResourceVolumeConnector volumeConnector() {
        throw new UnsupportedOperationException("This connector is not implemented for GCP!");
    }

    @Override
    public EncryptionResources encryptionResources() {
        return null;
    }

    @Override
    public ScriptResources scriptResources() {
        return gcpScriptResources;
    }

    @Override
    public AvailabilityZoneConnector availabilityZoneConnector() {
        return gcpAvailabilityZoneConnector;
    }
}
