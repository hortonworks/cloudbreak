package com.sequenceiq.cloudbreak.cloud.notification;


import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public interface PersistenceNotifier<T> {

    Promise<T> notifyResourceAllocation(CloudResource cloudResource, CloudContext cloudContext);

}
