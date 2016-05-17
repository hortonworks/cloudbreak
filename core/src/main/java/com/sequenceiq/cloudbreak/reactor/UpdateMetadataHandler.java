package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpdateMetadataResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpdateMetadataHandler implements ClusterEventHandler<UpdateMetadataRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<UpdateMetadataRequest> type() {
        return UpdateMetadataRequest.class;
    }

    @Override
    public void accept(Event<UpdateMetadataRequest> event) {
        UpdateMetadataRequest request = event.getData();
        UpdateMetadataResult result;
        try {
            int failedHosts = clusterUpscaleService.updateMetadata(request.getStackId(), request.getHostGroupName());
            result = new UpdateMetadataResult(request, failedHosts);
        } catch (Exception e) {
            result = new UpdateMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
