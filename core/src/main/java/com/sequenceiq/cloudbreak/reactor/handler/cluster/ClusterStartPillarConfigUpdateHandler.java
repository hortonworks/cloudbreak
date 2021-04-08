package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterStartPillarConfigUpdateHandler extends ExceptionCatcherEventHandler<ClusterStartPillarConfigUpdateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartPillarConfigUpdateHandler.class);

    @Inject
    private PillarConfigUpdateService pillarConfigUpdateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartPillarConfigUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterStartPillarConfigUpdateRequest> event) {
        return new PillarConfigUpdateFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterStartPillarConfigUpdateRequest> event) {
        ClusterStartPillarConfigUpdateRequest request = event.getData();
        Selectable response;
        try {
            pillarConfigUpdateService.doConfigUpdate(request.getResourceId());
            response = new ClusterStartPillarConfigUpdateResult(request);
        } catch (Exception e) {
            LOGGER.warn("Pillar configuration update failed.", e);
            response = new PillarConfigUpdateFailed(request.getResourceId(), e);
        }
        return response;
    }
}
