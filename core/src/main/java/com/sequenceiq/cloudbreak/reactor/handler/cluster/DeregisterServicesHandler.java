package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.DeregisterServicesResult;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class DeregisterServicesHandler implements EventHandler<DeregisterServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeregisterServicesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeregisterServicesRequest.class);
    }

    @Override
    public void accept(Event<DeregisterServicesRequest> event) {
        DeregisterServicesResult result = new DeregisterServicesResult(event.getData());
        try {
            LOGGER.info("Received DeregisterServicesRequest event: {}", event.getData());
            StackView stackView = stackDtoService.getStackViewById(event.getData().getStackId());
            Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stackView.getEnvironmentCrn());
            sdxBasicView.ifPresent(sdx -> platformAwareSdxConnector.tearDownDatahub(sdx.crn(), stackView.getResourceCrn()));
            LOGGER.info("Finished disabling Security");
        } catch (Exception e) {
            LOGGER.warn("An error has occured during deregistering services.", e);
            if (!event.getData().isForced()) {
                LOGGER.warn("Ignoring error during deregistering services because forced flag.");
                result = new DeregisterServicesResult(e.getMessage(), e, event.getData());
            }
        }
        LOGGER.info("Sending out DeregisterServicesResult: {}", result);
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        LOGGER.info("DeregisterServicesResult has been sent");
    }
}
