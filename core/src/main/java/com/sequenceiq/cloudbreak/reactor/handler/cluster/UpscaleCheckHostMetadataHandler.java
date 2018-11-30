package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UpscaleCheckHostMetadataResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleCheckHostMetadataHandler implements ReactorEventHandler<UpscaleCheckHostMetadataRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleCheckHostMetadataRequest.class);
    }

    @Override
    public void accept(Event<UpscaleCheckHostMetadataRequest> event) {
        UpscaleCheckHostMetadataRequest request = event.getData();
        UpscaleCheckHostMetadataResult result;
        try {
            StackView stackView = stackService.getViewByIdWithoutAuth(request.getStackId());
            clusterService.removeTerminatedPrimaryGateway(stackView.getClusterView().getId(), request.getPrimaryGatewayHostname(),
                    request.isSinglePrimaryGateway());
            result = new UpscaleCheckHostMetadataResult(request);
        } catch (Exception e) {
            result = new UpscaleCheckHostMetadataResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
