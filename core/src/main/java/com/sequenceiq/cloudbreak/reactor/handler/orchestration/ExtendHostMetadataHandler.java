package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendHostMetadataResult;
import com.sequenceiq.cloudbreak.service.stack.flow.HostMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExtendHostMetadataHandler implements ClusterEventHandler<ExtendHostMetadataRequest> {
    @Inject
    private EventBus eventBus;
    @Inject
    private HostMetadataSetup hostMetadataSetup;

    @Override
    public Class<ExtendHostMetadataRequest> type() {
        return ExtendHostMetadataRequest.class;
    }

    @Override
    public void accept(Event<ExtendHostMetadataRequest> event) {
        ExtendHostMetadataRequest request = event.getData();
        ExtendHostMetadataResult result;
        try {
            hostMetadataSetup.setupNewHostMetadata(request.getStackId(), request.getUpscaleCandidateAddresses());
            result = new ExtendHostMetadataResult(request);
        } catch (Exception e) {
            result = new ExtendHostMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));

    }
}
