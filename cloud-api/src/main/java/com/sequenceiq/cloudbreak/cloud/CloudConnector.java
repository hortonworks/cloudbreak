package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface CloudConnector {

    String platform();

    AuthenticatedContext authenticate(StackContext stackContext, CloudCredential cloudCredential);

    ResourceConnector resources();

    InstanceConnector instances();


}
