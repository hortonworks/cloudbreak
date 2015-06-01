package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public interface PollTaskFactory {

    PollTask<LaunchStackResult> newPollResourcesStateTask(AuthenticatedContext authenticatedContext,
            List<CloudResource> cloudResource);

}
