package com.sequenceiq.cloudbreak.reactor;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.UnhealthyInstancesDetectionResult;
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
public class UnhealthyInstancesDetectionHandler implements ClusterEventHandler<UnhealthyInstancesDetectionRequest> {

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
    public Class<UnhealthyInstancesDetectionRequest> type() {
        return UnhealthyInstancesDetectionRequest.class;
    }

    @Override
    public void accept(Event<UnhealthyInstancesDetectionRequest> event) {
        UnhealthyInstancesDetectionRequest request = event.getData();
        UnhealthyInstancesDetectionResult result;

        Long stackId = request.getStackId();
        Stack stack = stackService.getById(stackId);
        try {
            Set<InstanceMetaData> candidateUnhealthyInstances = unhealthyInstanceSelector.selectCandidateUnhealthyInstances(stack);
            if (candidateUnhealthyInstances.isEmpty()) {
                result = new UnhealthyInstancesDetectionResult(request, Collections.EMPTY_SET);
            } else {
                Set<String> unhealthyInstances = unhealthyInstancesFinalizer.finalizeUnhealthyInstances(stack, candidateUnhealthyInstances);
                result = new UnhealthyInstancesDetectionResult(request, unhealthyInstances);
            }
        } catch (CloudbreakSecuritySetupException e) {
            LOG.error("Could not get host statuses from Ambari", e);
            result = new UnhealthyInstancesDetectionResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }

}
