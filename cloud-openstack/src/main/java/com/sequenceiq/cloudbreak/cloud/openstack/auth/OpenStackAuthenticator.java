package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;

@Service
public class OpenStackAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackAuthenticator.class);

    @Inject
    private OpenStackClient openStackClient;

    @Override
    public String platform() {
        return OpenStackConstants.OPENSTACK;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to openstack ...");
        return openStackClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }
}
