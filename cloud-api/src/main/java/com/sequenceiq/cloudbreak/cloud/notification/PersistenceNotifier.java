package com.sequenceiq.cloudbreak.cloud.notification;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

import reactor.rx.Promise;

public interface PersistenceNotifier<T> {

    Promise<T> notifyAllocation(CloudResource cloudResource, CloudContext cloudContext);

    Promise<T> notifyDeletion(CloudResource cloudResource, CloudContext cloudContext);

}
