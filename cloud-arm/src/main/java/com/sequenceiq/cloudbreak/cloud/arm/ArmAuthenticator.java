package com.sequenceiq.cloudbreak.cloud.arm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class ArmAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArmAuthenticator.class);

    @Inject
    private ArmClient armClient;

    @Override
    public String platform() {
        return ArmConstants.AZURE_RM;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to azure ...");
        return armClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }
}
