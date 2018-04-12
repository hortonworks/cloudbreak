package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.PrepareClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class PrepareClusterTerminationHandler implements ReactorEventHandler<PrepareClusterTerminationRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(PrepareClusterTerminationRequest.class);
    }

    @Override
    public void accept(Event<PrepareClusterTerminationRequest> event) {
        PrepareClusterTerminationResult result;
        try {
            ambariClusterConnector.prepareSecurity(stackService.getByIdWithLists(event.getData().getStackId()));
            result = new PrepareClusterTerminationResult(event.getData());
        } catch (Exception e) {
            result = new PrepareClusterTerminationResult(e.getMessage(), e, event.getData());
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
