package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerPrepareProxyConfigFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerRefreshParcelFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerRefreshParcelRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerRefreshParcelSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ClusterManagerRefreshParcelClusterHandler extends ExceptionCatcherEventHandler<ClusterManagerRefreshParcelRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerRefreshParcelClusterHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerRefreshParcelRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterManagerRefreshParcelRequest> event) {
        LOGGER.error("ClusterManagerPrepareProxyConfigHandler step failed with the following message: {}", e.getMessage());
        return new ClusterManagerPrepareProxyConfigFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.refreshParcelRepos(stackId);
            response = new ClusterManagerRefreshParcelSuccess(stackId);
        } catch (RuntimeException e) {
            LOGGER.error("ClusterManagerRefreshParcelClusterHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerRefreshParcelFailed(stackId, e);
        }
        return response;
    }
}
