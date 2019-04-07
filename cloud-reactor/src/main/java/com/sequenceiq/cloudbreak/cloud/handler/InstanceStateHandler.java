package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstanceStateHandler implements CloudPlatformEventHandler<GetInstancesStateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceStateHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private InstanceStateQuery instanceStateQuery;

    @Override
    public Class<GetInstancesStateRequest> type() {
        return GetInstancesStateRequest.class;
    }

    @Override
    public void accept(Event<GetInstancesStateRequest> event) {
        LOGGER.debug("Received event: {}", event);
        GetInstancesStateRequest<GetInstancesStateResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        GetInstancesStateResult result;
        try {
            List<CloudVmInstanceStatus> instanceStatuses =
                    instanceStateQuery.getCloudVmInstanceStatuses(
                            request.getCloudCredential(), cloudContext, request.getInstances());
            result = new GetInstancesStateResult(request, instanceStatuses);
        } catch (CloudOperationNotSupportedException e) {
            List<CloudVmInstanceStatus> unknownStatuses = request.getInstances().stream()
                    .map(instance -> new CloudVmInstanceStatus(instance, InstanceStatus.UNKNOWN))
                    .collect(Collectors.toList());
            result = new GetInstancesStateResult(request, unknownStatuses);
        } catch (RuntimeException e) {
            result = new GetInstancesStateResult("Instance state synchronizing failed", e, request);
        }
        request.getResult().onNext(result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
