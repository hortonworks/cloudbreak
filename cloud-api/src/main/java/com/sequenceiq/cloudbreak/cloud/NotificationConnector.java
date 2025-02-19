package com.sequenceiq.cloudbreak.cloud;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface NotificationConnector {

    default Optional<ResourceEvent> preStartNotificiationEvent(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        return Optional.empty();
    }
}
