package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface Authenticator extends CloudPlatformAware {

    AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential);

}
