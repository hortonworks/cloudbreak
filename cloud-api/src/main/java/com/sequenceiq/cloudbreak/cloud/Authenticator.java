package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface Authenticator extends CloudPlatformAware {

    AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential);

}
