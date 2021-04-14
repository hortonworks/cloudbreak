package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RebootInstanceHandler implements CloudPlatformEventHandler<RebootInstancesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebootInstanceHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<RebootInstancesRequest> type() {
        return RebootInstancesRequest.class;
    }

    @Override
    public void accept(Event<RebootInstancesRequest> event) {
        LOGGER.debug("Received event: {}", event);
        RebootInstancesRequest<RebootInstancesResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext authenticatedContext = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudVmInstanceStatus> cloudVmInstanceStatuses = connector.instances()
                    .reboot(authenticatedContext,  request.getCloudResources(), request.getCloudInstances());
            InstancesStatusResult statusResult = new InstancesStatusResult(cloudContext, cloudVmInstanceStatuses);
            RebootInstancesResult result = new RebootInstancesResult(request.getResourceId(), statusResult, getInstances(request));
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (Exception e) {
            RebootInstancesResult failure = new RebootInstancesResult("Failed to reboot instances", e, request.getResourceId(),
                    getInstances(request));
            request.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

    private List<String> getInstances(RebootInstancesRequest<RebootInstancesResult> request) {
        return request.getCloudInstances().stream().map(instance -> instance.getInstanceId()).collect(Collectors.toList());
    }
}
