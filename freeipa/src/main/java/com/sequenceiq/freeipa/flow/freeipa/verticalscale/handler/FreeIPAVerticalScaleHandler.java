package com.sequenceiq.freeipa.flow.freeipa.verticalscale.handler;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIPAVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleRequest;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIPAVerticalScaleResult;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class FreeIPAVerticalScaleHandler implements CloudPlatformEventHandler<FreeIPAVerticalScaleRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPAVerticalScaleHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIPAVerticalScaleService verticalScale;

    @Override
    public Class<FreeIPAVerticalScaleRequest> type() {
        return FreeIPAVerticalScaleRequest.class;
    }

    @Override
    public void accept(Event<FreeIPAVerticalScaleRequest> event) {
        LOGGER.debug("Received event: {}", event);
        FreeIPAVerticalScaleRequest<FreeIPAVerticalScaleResult> request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.FreeIPAVerticalScaleRequest freeIPAVerticalScaleV1Request
                = request.getFreeIPAVerticalScaleRequest();
        try {
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, request.getCloudCredential());
            List<CloudResourceStatus> resourceStatus = verticalScale.verticalScale(ac, request, connector);
            LOGGER.info("Vertical scaling resource statuses: {}", resourceStatus);
            FreeIPAVerticalScaleResult result = new FreeIPAVerticalScaleResult(
                    request.getResourceId(),
                    ResourceStatus.UPDATED,
                    resourceStatus,
                    freeIPAVerticalScaleV1Request);
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
            LOGGER.debug("Vertical scaling successfully finished for {}, and the result is: {}", cloudContext, result);
        } catch (Exception e) {
            LOGGER.error("Vertical scaling stack failed", e);
            FreeIPAVerticalScaleResult result = new FreeIPAVerticalScaleResult(
                    e.getMessage(),
                    e,
                    request.getResourceId(),
                    request.getFreeIPAVerticalScaleRequest());
            request.getResult().onNext(result);
            eventBus.notify(CloudPlatformResult.failureSelector(FreeIPAVerticalScaleResult.class),
                    new Event<>(event.getHeaders(), result));
        }
    }

}
