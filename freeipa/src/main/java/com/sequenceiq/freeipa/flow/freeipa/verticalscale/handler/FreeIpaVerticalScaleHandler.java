package com.sequenceiq.freeipa.flow.freeipa.verticalscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleResult;

@Component
public class FreeIpaVerticalScaleHandler implements CloudPlatformEventHandler<FreeIpaVerticalScaleRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaVerticalScaleHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaVerticalScaleService verticalScale;

    @Override
    public Class<FreeIpaVerticalScaleRequest> type() {
        return FreeIpaVerticalScaleRequest.class;
    }

    @Override
    public void accept(Event<FreeIpaVerticalScaleRequest> event) {
        LOGGER.debug("Received event: {}", event);
        FreeIpaVerticalScaleRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        VerticalScaleRequest freeIPAVerticalScaleV1Request
                = request.getFreeIPAVerticalScaleRequest();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = verticalScale.verticalScale(ac, request, connector);
            LOGGER.info("Vertical scaling resource statuses: {}", resourceStatus);
            FreeIpaVerticalScaleResult result = new FreeIpaVerticalScaleResult(
                    request.getResourceId(),
                    ResourceStatus.UPDATED,
                    resourceStatus,
                    freeIPAVerticalScaleV1Request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Vertical scaling successfully finished for {}, and the result is: {}", cloudContext, result);
        } catch (Exception e) {
            LOGGER.error("Vertical scaling stack failed", e);
            FreeIpaVerticalScaleFailureEvent result = new FreeIpaVerticalScaleFailureEvent(
                    request.getResourceId(),
                    "Failure while trying to vertically upscale on the provider side",
                    Set.of(),
                    ERROR,
                    Map.of(),
                    e);
            eventBus.notify(STACK_VERTICALSCALE_FINISHED_FAILURE_EVENT.event(), new Event<>(event.getHeaders(), result));
        }
    }

}
