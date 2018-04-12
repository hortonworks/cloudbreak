package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterCreationService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class StartAmbariHandler implements ReactorEventHandler<StartAmbariRequest> {
    @Inject
    private AmbariClusterCreationService ambariClusterCreationService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartAmbariRequest.class);
    }

    @Override
    public void accept(Event<StartAmbariRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            ambariClusterCreationService.startAmbari(stackId);
            response = new StartAmbariSuccess(stackId);
        } catch (Exception e) {
            response = new StartAmbariFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
