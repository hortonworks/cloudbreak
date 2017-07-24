package com.sequenceiq.cloudbreak.cloud.byos;

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
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class BYOSConnector implements CloudConnector {

    @Inject
    private BYOSResourceConnector byosResourceConnector;

    @Inject
    private BYOSInstanceConnector byosInstanceConnector;

    @Inject
    private BYOSMetadataCollector byosMetadataCollector;

    @Inject
    private BYOSCredentialConnector byosCredentialConnector;

    @Inject
    private BYOSPlatformParameters byosPlatformParameters;

    @Inject
    private BYOSPlatformResources byosPlatformResources;

    @Inject
    private BYOSSetup byosSetup;

    @Inject
    private BYOSAuthenticator byosAuthenticator;

    @Inject
    private BYOSPlatformParameters platformParameters;

    @Override
    public Authenticator authentication() {
        return byosAuthenticator;
    }

    @Override
    public Setup setup() {
        return byosSetup;
    }

    @Override
    public List<Validator> validators() {
        return Collections.emptyList();
    }

    @Override
    public CredentialConnector credentials() {
        return byosCredentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return byosResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return byosInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return byosMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return platformParameters;
    }

    @Override
    public PlatformResources platformResources() {
        return byosPlatformResources;
    }

    @Override
    public Platform platform() {
        return BYOSConstants.BYOS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return BYOSConstants.BYOS_VARIANT;
    }
}
