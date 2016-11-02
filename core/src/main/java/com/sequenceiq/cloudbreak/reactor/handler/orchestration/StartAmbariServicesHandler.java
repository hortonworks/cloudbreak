package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StartAmbariServicesHandler implements ReactorEventHandler<StartAmbariServicesRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartAmbariServicesRequest.class);
    }

    @Override
    public void accept(Event<StartAmbariServicesRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            clusterServiceRunner.runAmbariServices(stackId);
            response = new StartAmbariServicesSuccess(stackId);
        } catch (Exception e) {
            response = new StartAmbariServicesFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
