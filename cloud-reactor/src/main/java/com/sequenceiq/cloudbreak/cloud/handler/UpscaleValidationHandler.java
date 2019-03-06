package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleValidationHandler implements CloudPlatformEventHandler<UpscaleStackValidationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<UpscaleStackValidationRequest> type() {
        return UpscaleStackValidationRequest.class;
    }

    @Override
    public void accept(Event<UpscaleStackValidationRequest> upscaleStackValidationRequestEvent) {
        LOGGER.debug("Received event: {}", upscaleStackValidationRequestEvent);
        UpscaleStackValidationRequest<UpscaleStackValidationResult> request = upscaleStackValidationRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            connector.setup().scalingPrerequisites(ac, request.getCloudStack(), true);
            UpscaleStackValidationResult result = new UpscaleStackValidationResult(request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(upscaleStackValidationRequestEvent.getHeaders(), result));
            LOGGER.debug("Upscale validation successfully finished for {}", cloudContext);
        } catch (Exception e) {
            UpscaleStackValidationResult result = new UpscaleStackValidationResult(e.getMessage(), e, request);
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(UpscaleStackValidationResult.class),
                    new Event<>(upscaleStackValidationRequestEvent.getHeaders(), result));
        }
    }
}
