package com.sequenceiq.cloudbreak.cloud.mock;

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
public class MockAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockAuthenticator.class);

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to mock ...");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    @Override
    public Platform platform() {
        return MockConstants.MOCK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return MockConstants.MOCK_VARIANT;
    }
}