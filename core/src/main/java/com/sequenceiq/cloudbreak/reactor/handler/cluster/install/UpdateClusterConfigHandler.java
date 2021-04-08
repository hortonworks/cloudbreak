package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.UpdateClusterConfigSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class UpdateClusterConfigHandler extends ExceptionCatcherEventHandler<UpdateClusterConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateClusterConfigHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateClusterConfigRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateClusterConfigRequest> event) {
        LOGGER.error("UpdateClusterConfigHandler step failed with the following message: {}", e.getMessage());
        return new UpdateClusterConfigFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateClusterConfigRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.updateConfig(stackId);
            response = new UpdateClusterConfigSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("UpdateClusterConfigHandler step failed with the following message: {}", e.getMessage());
            response = new UpdateClusterConfigFailed(stackId, e);
        }
        return response;
    }
}
