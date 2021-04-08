package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterManagerPrepareProxyConfigHandler extends ExceptionCatcherEventHandler<ClusterManagerPrepareProxyConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerPrepareProxyConfigHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerPrepareProxyConfigRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterManagerPrepareProxyConfigRequest> event) {
        LOGGER.error("ClusterManagerPrepareProxyConfigHandler step failed with the following message: {}", e.getMessage());
        return new ClusterManagerPrepareProxyConfigFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterManagerPrepareProxyConfigRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.prepareProxyConfig(stackId);
            response = new ClusterManagerPrepareProxyConfigSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ClusterManagerPrepareProxyConfigHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerPrepareProxyConfigFailed(stackId, e);
        }
        return response;
    }
}
