package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationFailedException;
import com.sequenceiq.cloudbreak.service.gateway.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

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
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    public void finishStackTermination(StackTerminationContext context, TerminateStackResult payload, Boolean forcedTermination) {
        LOGGER.debug("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        cleanupFreeIpa(forcedTermination, stack);
        terminationService.finalizeTermination(stack.getId(), true);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DELETE_COMPLETED, DELETE_COMPLETED.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        if (stack.getType() == StackType.DATALAKE) {
            datalakeResourcesService.deleteWithDependenciesByStackId(stack.getId());
        }
        metricService.incrementMetricCounter(MetricType.STACK_TERMINATION_SUCCESSFUL, stack);
    }

    private void cleanupFreeIpa(Boolean forcedTermination, Stack stack) {
        try {
            freeIpaCleanupService.cleanup(stack, false, null);
        } catch (FreeIpaOperationFailedException e) {
            LOGGER.error("Failed to cleanup", e);
            if (!forcedTermination) {
                throw e;
            }
        }
    }

    public void handleStackTerminationError(StackView stackView, StackFailureEvent payload, boolean forced) {
        String stackUpdateMessage;
        Msg eventMessage;
        DetailedStackStatus status;
        if (!forced) {
            Exception errorDetails = payload.getException();
            stackUpdateMessage = "Termination failed: " + errorDetails.getMessage();
            status = DetailedStackStatus.DELETE_FAILED;
            eventMessage = Msg.STACK_INFRASTRUCTURE_DELETE_FAILED;
            stackUpdater.updateStackStatus(stackView.getId(), status, stackUpdateMessage);
            LOGGER.debug("Error during stack termination flow: ", errorDetails);
        } else {
            terminationService.finalizeTermination(stackView.getId(), true);
            clusterService.updateClusterStatusByStackId(stackView.getId(), DELETE_COMPLETED);
            stackUpdateMessage = "Stack was force terminated.";
            status = DetailedStackStatus.DELETE_COMPLETED;
            eventMessage = Msg.STACK_FORCED_DELETE_COMPLETED;
        }
        flowMessageService.fireEventAndLog(stackView.getId(), eventMessage, status.name(), stackUpdateMessage);
    }

    public void deleteDnsEntry(Stack stack) {
        gatewayPublicEndpointManagementService.deleteDnsEntry(stack, null);
    }
}
