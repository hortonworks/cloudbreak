package com.sequenceiq.cloudbreak.cloud.handler;

import static com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService.DELAYED_TASK_EXECUTOR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.DelayedStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;

@Component
@ConditionalOnBean(name = DELAYED_TASK_EXECUTOR, value = DelayedExecutorService.class)
public class DelayedStopStackHandler implements CloudPlatformEventHandler<DelayedStopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedStopStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private DelayedExecutorService delayedExecutorService;

    @Override
    public Class<DelayedStopInstancesRequest> type() {
        return DelayedStopInstancesRequest.class;
    }

    @Override
    public void accept(Event<DelayedStopInstancesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        DelayedStopInstancesRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            List<CloudInstance> instances = request.getCloudInstances();
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = stopFirstInstance(request, connector, instances, authenticatedContext);
            if (instances.size() > 1) {
                for (CloudInstance instance : instances.subList(1, instances.size())) {
                    List<CloudVmInstanceStatus> stopInstanceResult = delayedExecutorService.runWithDelay(
                            () -> stopInstance(connector, authenticatedContext, request.getResources(), instance), request.getDelayInSec(), TimeUnit.SECONDS);
                    cloudVmInstanceStatuses.addAll(stopInstanceResult);
                }
            }
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
            StopInstancesResult result = new StopInstancesResult(request.getResourceId(), statusResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            StopInstancesResult failure = new StopInstancesResult("Failed to stop stack", e, request.getResourceId());
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

    private List<CloudVmInstanceStatus> stopFirstInstance(DelayedStopInstancesRequest request, CloudConnector connector,
            List<CloudInstance> instances, AuthenticatedContext authenticatedContext) {
        return instances.isEmpty() ? new ArrayList<>(0) : stopInstance(connector, authenticatedContext, request.getResources(), instances.get(0));
    }

    private List<CloudVmInstanceStatus> stopInstance(CloudConnector connector, AuthenticatedContext authenticatedContext, List<CloudResource> resources,
            CloudInstance instance) {
        LOGGER.info("Stop instance: {}", instance);
        return connector.instances().stop(authenticatedContext, resources, List.of(instance));
    }
}
