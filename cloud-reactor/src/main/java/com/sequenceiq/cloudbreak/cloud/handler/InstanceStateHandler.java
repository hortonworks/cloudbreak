package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

import reactor.bus.Event;

@Component
public class InstanceStateHandler implements CloudPlatformEventHandler<GetInstancesStateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceStateHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetInstancesStateRequest> type() {
        return GetInstancesStateRequest.class;
    }

    @Override
    public void accept(Event<GetInstancesStateRequest> event) {
        LOGGER.info("Received event: {}", event);
        GetInstancesStateRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            String platform = cloudContext.getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(cloudContext, request.getCloudCredential());
            List<CloudInstance> instances = request.getInstances();
            List<CloudVmInstanceStatus> instanceStatuses = connector.instances().check(authenticatedContext, instances);
            request.getResult().onNext(new GetInstancesStateResult(cloudContext, instanceStatuses));
        } catch (Exception e) {
            request.getResult().onNext(new GetInstancesStateResult(cloudContext, e));
        }
    }

}
