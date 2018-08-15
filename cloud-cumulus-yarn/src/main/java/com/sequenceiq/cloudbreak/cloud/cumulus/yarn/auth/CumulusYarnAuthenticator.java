package com.sequenceiq.cloudbreak.cloud.cumulus.yarn.auth;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.cumulus.yarn.CumulusYarnConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class CumulusYarnAuthenticator implements Authenticator {
    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    @Override
    public Platform platform() {
        return CumulusYarnConstants.CUMULUS_YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return CumulusYarnConstants.CUMULUS_YARN_VARIANT;
    }
}
