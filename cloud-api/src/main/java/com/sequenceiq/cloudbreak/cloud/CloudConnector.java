package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface CloudConnector {

    AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential);

    Setup setup();

    CredentialConnector credentials();

    ResourceConnector resources();

    InstanceConnector instances();


    String platform();

    String sshUser();

}
