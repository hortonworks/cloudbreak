package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Service
public class OpenStackAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackAuthenticator.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return Variant.EMPTY;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential, boolean verification) {
        LOGGER.info("Authenticating to openstack ...");
        KeystoneCredentialView keystoneCredentialView = openStackClient.createKeystoneCredential(cloudCredential);
        if (verification && KeystoneCredentialView.CB_KEYSTONE_V3.equals(keystoneCredentialView.getVersion())
                && KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE.equals(keystoneCredentialView.getScope())) {
            throw new CloudConnectorException("Creation of Openstack Keystone credentials with default scope is not supported");
        }
        return openStackClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }

}
