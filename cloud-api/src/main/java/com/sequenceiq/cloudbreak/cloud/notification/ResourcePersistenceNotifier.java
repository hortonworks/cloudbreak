package com.sequenceiq.cloudbreak.cloud.notification;


import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public interface ResourcePersistenceNotifier<T> {

    Promise<T> notifyResourceAllocation(CloudResource cloudResource, StackContext stackContext);

}
