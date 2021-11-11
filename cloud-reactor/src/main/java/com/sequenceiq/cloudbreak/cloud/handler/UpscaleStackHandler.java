package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleStackHandler implements CloudPlatformEventHandler<UpscaleStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleStackHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<UpscaleStackRequest> type() {
        return UpscaleStackRequest.class;
    }

    @Override
    public void accept(Event<UpscaleStackRequest> upscaleStackRequestEvent) {
        LOGGER.debug("Received event: {}", upscaleStackRequestEvent);
        UpscaleStackRequest<UpscaleStackResult> request = upscaleStackRequestEvent.getData();
        CloudContext cloudContext = request.getCloudContext();
        try {
            CloudConnector<?> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = getAuthenticatedContext(request, cloudContext, connector);
            List<CloudResourceStatus> resourceStatus = connector.resources().upscale(ac, request.getCloudStack(), request.getResourceList(),
                    request.getAdjustmentWithThreshold());
            LOGGER.info("Upscaled resource statuses: {}", resourceStatus);
            UpscaleStackResult result = new UpscaleStackResult(request.getResourceId(), ResourceStatus.UPDATED, resourceStatus);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
            LOGGER.debug("Upscale successfully finished for {}, and the result is: {}", cloudContext, result);
        } catch (Exception e) {
            LOGGER.error("Upscaling stack failed", e);
            UpscaleStackResult result = new UpscaleStackResult(e.getMessage(), e, request.getResourceId());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(UpscaleStackResult.class), new Event<>(upscaleStackRequestEvent.getHeaders(), result));
        }
    }

    private AuthenticatedContext getAuthenticatedContext(UpscaleStackRequest<UpscaleStackResult> request, CloudContext cloudContext,
            CloudConnector<?> connector) {
        return connector.authentication().authenticate(cloudContext, request.getCloudCredential());
    }

}
