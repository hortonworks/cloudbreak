package com.sequenceiq.cloudbreak.cloud.openstack;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClusterProxyService;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@Component
public class OpenStackAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackAuthenticator.class);

    @Inject
    private OpenStackConstants openstackConstants;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private OpenStackClusterProxyService clusterProxyService;

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.debug("Authenticating to openstack ...");
        KeystoneCredentialView credential = openStackClient.createKeystoneCredential(cloudCredential);
        validateCredential(credential);
        ensureClusterProxyRegistration(credential);
        return openStackClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }

    private void ensureClusterProxyRegistration(KeystoneCredentialView credential) {
        if (StringUtils.isNotBlank(credential.getJumpgateEnvironmentCrn())
                && !clusterProxyService.isRegistered(credential.getCredentialCrn())) {
            clusterProxyService.registerServices(credential);
        }
    }

    private void validateCredential(KeystoneCredentialView credential) {
        if (StringUtils.isBlank(credential.getUserName())) {
            throw new CloudConnectorException("OpenStack credential username must not be empty");
        }
        if (StringUtils.isBlank(credential.getPassword())) {
            throw new CloudConnectorException("OpenStack credential password must not be empty");
        }
        if (StringUtils.isBlank(credential.getEndpoint())) {
            throw new CloudConnectorException("OpenStack credential endpoint must not be empty");
        }
    }

    @Override
    public Platform platform() {
        return openstackConstants.platform();
    }

    @Override
    public Variant variant() {
        return openstackConstants.variant();
    }
}
