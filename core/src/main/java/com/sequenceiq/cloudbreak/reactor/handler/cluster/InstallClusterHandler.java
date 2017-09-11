package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterCreationService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallClusterHandler implements ReactorEventHandler<InstallClusterRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterCreationService ambariClusterCreationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InstallClusterRequest.class);
    }

    @Override
    public void accept(Event<InstallClusterRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            ambariClusterCreationService.buildAmbariCluster(stackId);
            response = new InstallClusterSuccess(stackId);
        } catch (RuntimeException e) {
            response = new InstallClusterFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
