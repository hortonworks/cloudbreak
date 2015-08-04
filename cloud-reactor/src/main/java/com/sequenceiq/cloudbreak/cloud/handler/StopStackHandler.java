package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.ProvisionSetupResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.StopStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.StopStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Instance;

import reactor.bus.Event;

@Component
public class StopStackHandler implements CloudPlatformEventHandler<StopStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<StopStackRequest> type() {
        return StopStackRequest.class;
    }

    @Override
    public void accept(Event<StopStackRequest> event) {
        LOGGER.info("Received event: {}", event);
        StopStackRequest request = event.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getCloudContext(), request.getCloudCredential());
            connector.instances().stop(authenticatedContext, getInstances(request.getCloudStack()));
            //TODO poll
            request.getResult().onNext(new StopStackResult(request.getCloudContext(), "Stack successfully stopped"));
        } catch (Exception e) {
            LOGGER.error("Failed to handle StopStackRequest.", e);
            request.getResult().onNext(new ProvisionSetupResult(e));
        }
        LOGGER.info("StopStackHandler finished");
    }

    private List<Instance> getInstances(CloudStack cloudStack) {
        List<Instance> instances = new ArrayList<>();
        for (Group group : cloudStack.getGroups()) {
            instances.addAll(group.getInstances());
        }
        return instances;
    }
}
