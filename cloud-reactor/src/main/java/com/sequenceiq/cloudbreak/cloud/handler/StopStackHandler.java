package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStackHandler implements CloudPlatformEventHandler<StopInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<StopInstancesRequest> type() {
        return StopInstancesRequest.class;
    }

    @Override
    public void accept(Event<StopInstancesRequest> event) {
        LOGGER.info("Received event: {}", event);
        StopInstancesRequest<StopInstancesResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            List<CloudInstance> instances = request.getCloudInstances();
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = connector.instances().stop(authenticatedContext, request.getResources(), instances);
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
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
