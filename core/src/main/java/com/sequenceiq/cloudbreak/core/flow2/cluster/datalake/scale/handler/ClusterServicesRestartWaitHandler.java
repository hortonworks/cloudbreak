package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ServicesRollingRestartEvent.SERVICES_ROLLING_RESTART_IN_PROGRESS_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.scale.ClusterServicesRestartEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class ClusterServicesRestartWaitHandler extends EventSenderAwareHandler<StackEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServicesRestartWaitHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    protected ClusterServicesRestartWaitHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return SERVICES_ROLLING_RESTART_IN_PROGRESS_EVENT.event();
    }

    @Override
    public void accept(Event<StackEvent> event) {
        StackEvent data = event.getData();
        Stack stack = stackService.get(data.getResourceId());
        try {
            clusterApiConnectors.getConnector(stack).clusterModificationService().rollingRestartServices();
        } catch (Exception e) {
            LOGGER.error("Cluster services rolling restart failed with exception", e);
            ClusterServicesRestartEvent result = new ClusterServicesRestartEvent(SERVICES_ROLLING_RESTART_FAILURE_EVENT.event(),
                    data.getResourceId(), stack.getName(), stack.getResourceCrn(), e);
            eventSender().sendEvent(result, event.getHeaders());
            throw new RuntimeException(e);
        }
        ClusterServicesRestartEvent result = new ClusterServicesRestartEvent(SERVICES_ROLLING_RESTART_FINISHED_EVENT.event(),
                data.getResourceId(), stack.getName(), stack.getResourceCrn());
        eventSender().sendEvent(result, event.getHeaders());
    }
}
