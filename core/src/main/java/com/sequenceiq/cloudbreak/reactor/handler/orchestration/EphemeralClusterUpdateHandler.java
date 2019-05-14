package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClusterUpdateSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EphemeralClusterUpdateHandler implements ReactorEventHandler<EphemeralClusterUpdateRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(EphemeralClusterUpdateRequest.class);
    }

    @Override
    public void accept(Event<EphemeralClusterUpdateRequest> event) {
        EphemeralClusterUpdateRequest request = event.getData();
        Selectable response;
        try {
            clusterServiceRunner.updateSaltState(request.getResourceId());
            response = new EphemeralClusterUpdateSuccess(request.getResourceId());
        } catch (Exception e) {
            response = new EphemeralClusterUpdateFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
