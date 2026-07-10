package com.sequenceiq.cloudbreak.cloud.openstack;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;

@Component
public class OpenStackAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackAuthenticator.class);

    @Inject
    private OpenStackConstants openstackConstants;

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.debug("Authenticating to openstack ...");
        return openStackClient.createAuthenticatedContext(cloudContext, cloudCredential);
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
