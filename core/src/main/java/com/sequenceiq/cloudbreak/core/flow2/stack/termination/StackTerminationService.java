package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_FORCED_DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_DELETE_FAILED;

@Service
public class StackTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationService.class);

    @Inject
    private TerminationService terminationService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakMetricService metricService;

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    public void finishStackTermination(StackTerminationContext context, TerminateStackResult payload, Boolean forcedTermination) {
        LOGGER.debug("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeTermination(stack.getId(), forcedTermination);
        flowMessageService.fireEventAndLog(stack.getId(), DELETE_COMPLETED.name(), STACK_DELETE_COMPLETED);
        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        metricService.incrementMetricCounter(MetricType.STACK_TERMINATION_SUCCESSFUL, stack);
    }

    public void handleStackTerminationError(StackView stackView, StackFailureEvent payload, boolean forced) {
        String stackUpdateMessage;
        ResourceEvent resourceEvent;
        DetailedStackStatus status;
        if (!forced) {
            Exception errorDetails = payload.getException();
            stackUpdateMessage = "Termination failed: " + errorDetails.getMessage();
            status = DetailedStackStatus.DELETE_FAILED;
            resourceEvent = STACK_INFRASTRUCTURE_DELETE_FAILED;
            stackUpdater.updateStackStatus(stackView.getId(), status, stackUpdateMessage);
            LOGGER.debug("Error during stack termination flow: ", errorDetails);
        } else {
            terminationService.finalizeTermination(stackView.getId(), true);
            clusterService.updateClusterStatusByStackId(stackView.getId(), DELETE_COMPLETED);
            stackUpdateMessage = "Stack was force terminated.";
            status = DetailedStackStatus.DELETE_COMPLETED;
            resourceEvent = STACK_FORCED_DELETE_COMPLETED;
        }
        flowMessageService.fireEventAndLog(stackView.getId(), status.name(), resourceEvent, stackUpdateMessage);
    }

    public void deleteDnsEntry(Stack stack) {
        clusterPublicEndpointManagementService.terminate(stack);
    }
}
