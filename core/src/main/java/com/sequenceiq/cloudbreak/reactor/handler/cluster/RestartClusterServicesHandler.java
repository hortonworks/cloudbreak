package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RestartClusterServicesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RestartClusterServicesHandler extends ExceptionCatcherEventHandler<RestartClusterServicesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestartClusterServicesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RestartClusterServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RestartClusterServicesRequest> event) {
        return new StackFailureEvent(event.getData().getFailureSelector(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RestartClusterServicesRequest> event) {
        LOGGER.debug("Accepting Cluster services restart request...");
        RestartClusterServicesRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            clusterApiConnectors.getConnector(stack).restartAll(request.isWithMgmtServices());
            response = new RestartClusterServicesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Cluster services restart failed", e);
            response = new StackFailureEvent(request.getFailureSelector(), request.getResourceId(), e);
        }
        return response;
    }
}
