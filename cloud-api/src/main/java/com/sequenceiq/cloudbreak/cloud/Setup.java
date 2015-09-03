package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

public interface Setup {

    void execute(AuthenticatedContext authenticatedContext, CloudStack stack);

}
