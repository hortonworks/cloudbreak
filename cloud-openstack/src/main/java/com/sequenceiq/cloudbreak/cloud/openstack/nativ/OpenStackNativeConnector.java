package com.sequenceiq.cloudbreak.cloud.openstack.nativ;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackAuthenticator;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackCredentialConnector;
import com.sequenceiq.cloudbreak.cloud.openstack.OpenStackSetup;

@Service
public class OpenStackNativeConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackNativeConnector.class);

    @Inject
    private OpenStackCredentialConnector credentialConnector;
    @Inject
    private OpenStackAuthenticator authenticator;
    @Inject
    private OpenStackClient openStackClient;
    @Inject
    private OpenStackNativeResourceConnector resourceConnector;
    @Inject
    private OpenStackNativeInstanceConnector instanceConnector;
    @Inject
    private OpenStackSetup openStackSetup;

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK_NATIVE;
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
    public Setup setup() {
        return openStackSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return credentialConnector;
    }
}
