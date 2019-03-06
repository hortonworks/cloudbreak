package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ProvisionSetupHandler implements CloudPlatformEventHandler<SetupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionSetupHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<SetupRequest> type() {
        return SetupRequest.class;
    }

    @Override
    public void accept(Event<SetupRequest> event) {
        LOGGER.debug("Received event: {}", event);
        SetupRequest<SetupResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        SetupResult result;
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            CloudStack cloudStack = request.getCloudStack();
            connector.setup().prerequisites(auth, cloudStack, resourceNotifier);
            result = new SetupResult(request);

            LOGGER.debug("Provision setup finished for {}", cloudContext);
        } catch (RuntimeException e) {
            result = new SetupResult(e, request);
        }
        request.getResult().onNext(result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
