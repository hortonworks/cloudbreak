package com.sequenceiq.cloudbreak.cloud.byos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class BYOSAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BYOSAuthenticator.class);

    @Override
    public Platform platform() {
        return BYOSConstants.BYOS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return BYOSConstants.BYOS_VARIANT;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to byos ...");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }
}
