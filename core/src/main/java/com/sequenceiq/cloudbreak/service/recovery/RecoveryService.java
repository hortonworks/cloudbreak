package com.sequenceiq.cloudbreak.service.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DELETE_COMPLETED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Service
public class RecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryService.class);

    @Inject
    private TerminationService terminationService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakMetricService metricService;

    public void finishRecoveryTeardown(StackTerminationContext context, TerminateStackResult payload) {
        LOGGER.debug("Recovery tear-down result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeRecoveryTeardown(stack.getId());
        metricService.incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_SUCCESSFUL, stack);
        flowMessageService.fireEventAndLog(stack.getId(), DELETE_COMPLETED.name(), STACK_DELETE_COMPLETED);
    }

    public void handleRecoveryError(StackView stack, Exception errorDetails) {
        DetailedStackStatus status;
        Long stackId = stack.getId();
        String stackUpdateMessage = "Recovery failed: " + errorDetails.getMessage();
        status = DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
        stackUpdater.updateStackStatus(stackId, status, stackUpdateMessage);
        LOGGER.info("Error during stack recovery flow: ", errorDetails);
        metricService.incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_FAILED, stack, errorDetails);
        flowMessageService.fireEventAndLog(stackId, status.name(), DATALAKE_RECOVERY_FAILED, stackUpdateMessage);
    }

}
