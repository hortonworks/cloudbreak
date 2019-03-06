package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStackHandler implements CloudPlatformEventHandler<StopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<InstancesStatusResult> syncPollingScheduler;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopInstancesRequest> type() {
        return StopInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopInstancesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        StopInstancesRequest<StopInstancesResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            List<CloudInstance> instances = request.getCloudInstances();
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = connector.instances().stop(authenticatedContext, request.getResources(), instances);
            PollTask<InstancesStatusResult> task = statusCheckFactory.newPollInstanceStateTask(authenticatedContext, instances,
                    Sets.newHashSet(InstanceStatus.STOPPED, InstanceStatus.FAILED));
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
            if (!task.completed(statusResult)) {
                statusResult = syncPollingScheduler.schedule(task);
            }
            StopInstancesResult result = new StopInstancesResult(request, cloudContext, statusResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            StopInstancesResult failure = new StopInstancesResult("Failed to stop stack", e, request);
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
