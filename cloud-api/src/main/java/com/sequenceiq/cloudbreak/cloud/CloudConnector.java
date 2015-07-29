package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface CloudConnector extends CloudPlatformAware {

    AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential);

    ResourceConnector resources();

    InstanceConnector instances();

    Setup setup();

}
