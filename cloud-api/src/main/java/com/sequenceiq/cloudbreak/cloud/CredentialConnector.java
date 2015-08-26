package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;

public interface CredentialConnector {


    CloudCredentialStatus create(AuthenticatedContext authenticatedContext);

    CloudCredentialStatus delete(AuthenticatedContext authenticatedContext);


}
