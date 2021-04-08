package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterManagerSetupMonitoringHandler extends ExceptionCatcherEventHandler<ClusterManagerSetupMonitoringRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerSetupMonitoringHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerSetupMonitoringRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterManagerSetupMonitoringRequest> event) {
        LOGGER.error("ClusterManagerSetupMonitoringHandler step failed with the following message: {}", e.getMessage());
        return new ClusterManagerSetupMonitoringFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterManagerSetupMonitoringRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.setupMonitoring(stackId);
            response = new ClusterManagerSetupMonitoringSuccess(stackId);
        } catch (RuntimeException | CloudbreakException e) {
            LOGGER.error("ClusterManagerSetupMonitoringHandler step failed with the following message: {}", e.getMessage());
            response = new ClusterManagerSetupMonitoringFailed(stackId, e);
        }
        return response;
    }
}
