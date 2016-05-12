package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateInstanceMetadataResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterDownscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpdateInstanceMetadataHandler implements ClusterEventHandler<UpdateInstanceMetadataRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterDownscaleService clusterDownscaleService;

    @Override
    public Class<UpdateInstanceMetadataRequest> type() {
        return UpdateInstanceMetadataRequest.class;
    }

    @Override
    public void accept(Event<UpdateInstanceMetadataRequest> event) {
        UpdateInstanceMetadataRequest request = event.getData();
        UpdateInstanceMetadataResult result;
        try {
            clusterDownscaleService.updateMetadata(request.getStackId(), request.getHostNames());
            result = new UpdateInstanceMetadataResult(request);
        } catch (Exception e) {
            result = new UpdateInstanceMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
