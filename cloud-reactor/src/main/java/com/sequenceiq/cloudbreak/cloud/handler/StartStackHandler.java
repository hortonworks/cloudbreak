package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

@Component
public class StartStackHandler implements CloudPlatformEventHandler<StartInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StartInstancesRequest> type() {
        return StartInstancesRequest.class;
    }

    @Override
    public void accept(Event<StartInstancesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        StartInstancesRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            LOGGER.debug("Start the stack with platform variant: {}", cloudContext.getPlatformVariant());
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> instances = request.getCloudInstances();
            LOGGER.debug("Requesting start for the following instances: {}", String.join(",",
                    instances.stream().map(CloudInstance::getInstanceId).collect(Collectors.toList())));
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().start(authenticatedContext, request.getResources(), instances);
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, instanceStatuses);
            StartInstancesResult result = new StartInstancesResult(request.getResourceId(), statusResult);
            request.getResult().onNext(result);
            LOGGER.debug("Stack successfully started");
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.warn("Failed to start stack", e);
            StartInstancesResult failure = new StartInstancesResult("Failed to start stack", e, request.getResourceId());
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }
}
