package com.sequenceiq.cloudbreak.service.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_TEARDOWN_FINISHED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class RecoveryTeardownService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryTeardownService.class);

    @Inject
    private TerminationService terminationService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakMetricService metricService;

    public void handleRecoveryTeardownSuccess(StackView stack, TerminateStackResult payload) {
        LOGGER.debug("Recovery tear-down result: {}", payload);
        terminationService.finalizeRecoveryTeardown(stack.getId());
        metricService.incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_SUCCESSFUL, stack);
        flowMessageService.fireEventAndLog(stack.getId(), DELETE_COMPLETED.name(), DATALAKE_RECOVERY_TEARDOWN_FINISHED);
    }

    public void handleRecoveryTeardownError(StackView stack, Exception errorDetails) {
        Long stackId = stack.getId();
        String stackUpdateMessage = "Recovery failed: " + errorDetails.getMessage();
        DetailedStackStatus status = DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
        stackUpdater.updateStackStatus(stackId, status, stackUpdateMessage);
        LOGGER.info("Error during stack recovery flow: ", errorDetails);
        metricService.incrementMetricCounter(MetricType.STACK_RECOVERY_TEARDOWN_FAILED, stack, errorDetails);
        flowMessageService.fireEventAndLog(stackId, status.name(), DATALAKE_RECOVERY_FAILED, stackUpdateMessage);
    }

}
