package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders;
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory;

@Component
public class ResourceActionFactory {

    @Inject
    private ResourceBuilders resourceBuilders;

    @Inject
    private SyncPollingScheduler<List<CloudResourceStatus>> syncPollingScheduler;

    @Inject
    private ResourcePollTaskFactory resourcePollTaskFactory;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    public ResourceCreationCallable buildCreationCallable(ResourceCreationCallablePayload payload) {
        return new ResourceCreationCallable(payload, resourceBuilders, syncPollingScheduler,
                resourcePollTaskFactory, persistenceNotifier);
    }

    public ResourceDeletionCallable buildDeletionCallable(ResourceDeletionCallablePayload payload) {
        return new ResourceDeletionCallable(payload, syncPollingScheduler, resourcePollTaskFactory, persistenceNotifier);
    }

    public ResourceStopStartCallable buildStopStartCallable(ResourceStopStartCallablePayload payload) {
        return new ResourceStopStartCallable(payload);
    }
}
