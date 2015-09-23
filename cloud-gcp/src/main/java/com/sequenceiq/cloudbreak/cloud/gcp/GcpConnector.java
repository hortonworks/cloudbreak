package com.sequenceiq.cloudbreak.cloud.gcp;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Service
public class GcpConnector implements CloudConnector {

    @Inject
    private GcpAuthenticator authenticator;
    @Inject
    private GcpProvisionSetup provisionSetup;
    @Inject
    private GcpInstanceConnector instanceConnector;
    @Inject
    private GcpResourceConnector resourceConnector;
    @Inject
    private GcpCredentialConnector gcpCredentialConnector;
    @Inject
    private GcpPlatformParameters gcpPlatformParameters;

    @Override
    public Authenticator authentication() {
        return authenticator;
    }

    @Override
    public Setup setup() {
        return provisionSetup;
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
    public PlatformParameters parameters() {
        return gcpPlatformParameters;
    }

    @Override
    public String platform() {
        return CloudPlatform.GCP.name();
    }

    @Override
    public String variant() {
        return CloudPlatform.GCP.name();
    }

}
