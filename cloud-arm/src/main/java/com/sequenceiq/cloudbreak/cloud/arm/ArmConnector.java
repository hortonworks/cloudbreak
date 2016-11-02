package com.sequenceiq.cloudbreak.cloud.arm;

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
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class ArmConnector implements CloudConnector {

    @Inject
    private ArmClient armClient;

    @Inject
    private ArmResourceConnector armResourceConnector;

    @Inject
    private ArmInstanceConnector armInstanceConnector;

    @Inject
    private ArmMetadataCollector armMetadataCollector;

    @Inject
    private ArmCredentialConnector armCredentialConnector;

    @Inject
    private ArmPlatformParameters armPlatformParameters;

    @Inject
    private ArmSetup armSetup;

    @Inject
    private ArmAuthenticator armAuthenticator;

    @Override
    public Platform platform() {
        return ArmConstants.AZURE_RM_PLATFORM;
    }

    @Override
    public Variant variant() {
        return ArmConstants.AZURE_RM_VARIANT;
    }

    @Override
    public Authenticator authentication() {
        return armAuthenticator;
    }

    @Override
    public ResourceConnector resources() {
        return armResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return armInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return armMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return armPlatformParameters;
    }

    @Override
    public Setup setup() {
        return armSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return armCredentialConnector;
    }

}
