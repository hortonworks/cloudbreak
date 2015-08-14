package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public interface BooleanStateConnector {

    Boolean check(AuthenticatedContext authenticatedContext);

}
