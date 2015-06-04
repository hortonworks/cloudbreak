package com.sequenceiq.cloudbreak.cloud.notification;


import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationPersisted;

import reactor.rx.Promise;

public interface ResourcePersistenceNotifier {

    Promise<ResourceAllocationPersisted> notifyResourceAllocation(CloudResource cloudResource, StackContext stackContext);

}
