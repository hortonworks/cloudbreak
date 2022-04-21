package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ClusterManagerSetupMonitoringSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterManagerSetupMonitoringHandler extends ExceptionCatcherEventHandler<ClusterManagerSetupMonitoringRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerSetupMonitoringHandler.class);

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
        // TODO: remove this step - no need for that anymore
        return  new ClusterManagerSetupMonitoringSuccess(stackId);
    }
}
