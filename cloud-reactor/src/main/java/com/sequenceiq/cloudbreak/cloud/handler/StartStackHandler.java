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
import com.sequenceiq.cloudbreak.cloud.event.resource.StartStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.StartStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Instance;

import reactor.bus.Event;

@Component
public class StartStackHandler implements CloudPlatformEventHandler<StartStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<StartStackRequest> type() {
        return StartStackRequest.class;
    }

    @Override
    public void accept(Event<StartStackRequest> event) {
        LOGGER.info("Received event: {}", event);
        StartStackRequest request = event.getData();
        try {
            String platform = request.getCloudContext().getPlatform();
            CloudConnector connector = cloudPlatformConnectors.get(platform);
            AuthenticatedContext authenticatedContext = connector.authenticate(request.getCloudContext(), request.getCloudCredential());
            connector.instances().start(authenticatedContext, getInstances(request.getCloudStack()));
            //TODO poll
            request.getResult().onNext(new StartStackResult(request.getCloudContext(), "Stack successfully started"));
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
