package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendConsulMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ExtendConsulMetadataResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulMetadataSetup;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ExtendConsulMetadataHandler implements ClusterEventHandler<ExtendConsulMetadataRequest> {
    @Inject
    private EventBus eventBus;
    @Inject
    private ConsulMetadataSetup consulMetadataSetup;

    @Override
    public Class<ExtendConsulMetadataRequest> type() {
        return ExtendConsulMetadataRequest.class;
    }

    @Override
    public void accept(Event<ExtendConsulMetadataRequest> event) {
        ExtendConsulMetadataRequest request = event.getData();
        ExtendConsulMetadataResult result;
        try {
            consulMetadataSetup.setupNewConsulMetadata(request.getStackId(), request.getUpscaleCandidateAddresses());
            result = new ExtendConsulMetadataResult(request);
        } catch (Exception e) {
            result = new ExtendConsulMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));

    }
}
