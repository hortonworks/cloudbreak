package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AddClusterContainersResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AddClusterContainersHandler implements ClusterEventHandler<AddClusterContainersRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<AddClusterContainersRequest> type() {
        return AddClusterContainersRequest.class;
    }

    @Override
    public void accept(Event<AddClusterContainersRequest> event) {
        AddClusterContainersRequest request = event.getData();
        AddClusterContainersResult result;
        try {
            clusterUpscaleService.addClusterContainers(request.getStackId(), request.getHostGroupName(),
                    request.getScalingAdjustment());
            result = new AddClusterContainersResult(request);
        } catch (Exception e) {
            result = new AddClusterContainersResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
