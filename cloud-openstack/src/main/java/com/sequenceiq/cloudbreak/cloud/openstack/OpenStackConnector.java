package com.sequenceiq.cloudbreak.cloud.openstack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;

@Service
public class OpenStackConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    @Inject
    private OpenStackCredentialConnector credentialConnector;
    @Inject
    private OpenStackAuthenticator authenticator;
    @Inject
    private OpenStackClient openStackClient;
    @Inject
    private OpenStackResourceConnector resourceConnector;
    @Inject
    private OpenStackInstanceConnector instanceConnector;
    @Inject
    private OpenStackSetup openStackSetup;
    @Inject
    private OpenStackParameters openStackParameters;

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }

    @Override
    public String variant() {
        return OpenStackConstants.Variant.HEAT.name();
    }

    @Override
    public Authenticator authentication() {
        return authenticator;
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
        return openStackParameters;
    }

    @Override
    public Setup setup() {
        return openStackSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }
}
