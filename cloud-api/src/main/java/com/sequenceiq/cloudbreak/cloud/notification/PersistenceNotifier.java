package com.sequenceiq.cloudbreak.cloud.notification;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DynamicModel;

import reactor.rx.Promise;

public interface PersistenceNotifier<E extends DynamicModel, T> {

    Promise<T> notifyAllocation(E cloudResource, CloudContext cloudContext);

    Promise<T> notifyUpdate(E cloudResource, CloudContext cloudContext);

    Promise<T> notifyDeletion(E cloudResource, CloudContext cloudContext);

    String topic();
}
