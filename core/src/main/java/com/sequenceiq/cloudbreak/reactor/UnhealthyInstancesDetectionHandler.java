package com.sequenceiq.cloudbreak.reactor;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.repair.CandidateUnhealthyInstanceSelector;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstancesFinalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

@Component
public class UnhealthyInstancesDetectionHandler implements ReactorEventHandler<UnhealthyInstancesDetectionRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(UnhealthyInstancesDetectionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private CandidateUnhealthyInstanceSelector unhealthyInstanceSelector;

    @Inject
    private UnhealthyInstancesFinalizer unhealthyInstancesFinalizer;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UnhealthyInstancesDetectionRequest.class);
    }

    @Override
    public void accept(Event<UnhealthyInstancesDetectionRequest> event) {
        UnhealthyInstancesDetectionRequest request = event.getData();
        UnhealthyInstancesDetectionResult result;

        Long stackId = request.getStackId();
        Stack stack = stackService.getById(stackId);
        try {
            Set<InstanceMetaData> candidateUnhealthyInstances = unhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack.getId());
            if (candidateUnhealthyInstances.isEmpty()) {
                result = new UnhealthyInstancesDetectionResult(request, Collections.emptySet());
            } else {
                Set<String> unhealthyInstances = unhealthyInstancesFinalizer.finalizeUnhealthyInstances(stack, candidateUnhealthyInstances);
                result = new UnhealthyInstancesDetectionResult(request, unhealthyInstances);
            }
        } catch (RuntimeException e) {
            String msg = String.format("Could not get statuses for unhealty instances: %s", e.getMessage());
            LOG.error(msg, e);
            result = new UnhealthyInstancesDetectionResult(msg, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
