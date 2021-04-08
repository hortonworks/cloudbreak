package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForClusterManagerFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.WaitForClusterManagerSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class WaitClusterManagerHandler extends ExceptionCatcherEventHandler<WaitForClusterManagerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitClusterManagerHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForClusterManagerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitForClusterManagerRequest> event) {
        LOGGER.error("WaitClusterManagerHandler step failed with the following message: {}", e.getMessage());
        return new WaitForClusterManagerFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<WaitForClusterManagerRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.waitForClusterManager(stackId);
            response = new WaitForClusterManagerSuccess(stackId);
        } catch (RuntimeException | ClusterClientInitException | CloudbreakException e) {
            LOGGER.error("WaitClusterManagerHandler step failed with the following message: {}", e.getMessage());
            response = new WaitForClusterManagerFailed(stackId, e);
        }
        return response;
    }
}
