package com.sequenceiq.cloudbreak.cloud.task;


import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectorV2;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public class ScheduledStatusCheckTask {

    private AuthenticatedContext context;

    private CloudPlatformConnectorV2 connector;

    private List<CloudResourceStatus> cloudResourceStatuses;

    private CloudResourceStatus expected;

    public ScheduledStatusCheckTask(AuthenticatedContext context, CloudPlatformConnectorV2 connector,
            List<CloudResourceStatus> cloudResourceStatuses, CloudResourceStatus expected) {
        this.context = context;
        this.connector = connector;
        this.cloudResourceStatuses = cloudResourceStatuses;
        this.expected = expected;
    }
}
