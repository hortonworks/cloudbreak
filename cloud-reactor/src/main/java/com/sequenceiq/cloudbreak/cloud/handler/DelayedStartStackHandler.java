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
import com.sequenceiq.cloudbreak.cloud.event.instance.DelayedStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.executor.DelayedExecutorService;

@Component
@ConditionalOnBean(name = DELAYED_TASK_EXECUTOR, value = DelayedExecutorService.class)
public class DelayedStartStackHandler implements CloudPlatformEventHandler<DelayedStartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedStartStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private DelayedExecutorService delayedExecutorService;

    @Override
    public Class<DelayedStartInstancesRequest> type() {
        return DelayedStartInstancesRequest.class;
    }

    @Override
    public void accept(Event<DelayedStartInstancesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        DelayedStartInstancesRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> instances = request.getCloudInstances();
            List<CloudVmInstanceStatus> instanceStatuses = startFirstInstance(request.getResources(), connector, authenticatedContext, instances);
            if (instances.size() > 1) {
                for (CloudInstance instance : instances.subList(1, instances.size())) {
                    List<CloudVmInstanceStatus> startInstanceResult = delayedExecutorService.runWithDelay(
                            () -> startInstance(request.getResources(), connector, authenticatedContext, instance), request.getDelayInSec(), TimeUnit.SECONDS);
                    instanceStatuses.addAll(startInstanceResult);
                }
            }
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, instanceStatuses);
            StartInstancesResult result = new StartInstancesResult(request.getResourceId(), statusResult);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            StartInstancesResult failure = new StartInstancesResult("Failed to start stack", e, request.getResourceId());
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

    private List<CloudVmInstanceStatus> startFirstInstance(List<CloudResource> resources, CloudConnector connector,
            AuthenticatedContext authenticatedContext, List<CloudInstance> instances) {
        return instances.isEmpty() ? new ArrayList<>(0) : startInstance(resources, connector, authenticatedContext, instances.get(0));
    }

    private List<CloudVmInstanceStatus> startInstance(List<CloudResource> resources, CloudConnector connector,
            AuthenticatedContext authenticatedContext, CloudInstance instance) {
        LOGGER.info("Start instance: {}", instance);
        List<CloudInstance> vms = new ArrayList<>();
        vms.add(instance);
        return connector.instances().start(authenticatedContext, resources, vms);
    }
}
