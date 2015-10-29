package com.sequenceiq.cloudbreak.cloud.gcp;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class GcpAuthenticator implements Authenticator {

    @Override
    public Platform platform() {
        return GcpConstants.GCP_PLATFORM;
    }

    @Override
    public Variant variant() {
        return GcpConstants.GCP_VARIANT;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }
}
