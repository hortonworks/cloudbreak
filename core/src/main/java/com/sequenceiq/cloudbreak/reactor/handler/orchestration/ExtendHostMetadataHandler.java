package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.stack.flow.HostMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExtendHostMetadataHandler implements EventHandler<ExtendHostMetadataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendHostMetadataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private HostMetadataSetup hostMetadataSetup;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ExtendHostMetadataRequest.class);
    }

    @Override
    public void accept(Event<ExtendHostMetadataRequest> event) {
        ExtendHostMetadataRequest request = event.getData();
        ExtendHostMetadataResult result;
        try {
            LOGGER.info("ExtendHostMetadataRequest arrived with upscale candidates: {}", request.getUpscaleCandidateAddresses());
            hostMetadataSetup.setupNewHostMetadata(request.getResourceId(), request.getUpscaleCandidateAddresses());
            result = new ExtendHostMetadataResult(request);
        } catch (Exception e) {
            result = new ExtendHostMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
