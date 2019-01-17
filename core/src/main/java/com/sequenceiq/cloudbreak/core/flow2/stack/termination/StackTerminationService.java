package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.metrics.MetricService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.service.usages.UsageService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class StackTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationService.class);

    @Inject
    private TerminationService terminationService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private UsageService usageService;

    @Inject
    private DependecyDeletionService dependecyDeletionService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private MetricService metricService;

    public void finishStackTermination(StackTerminationContext context, TerminateStackResult payload, Boolean deleteDependencies) {
        LOGGER.info("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeTermination(stack.getId(), true);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_TERMINATED, BillingStatus.BILLING_TERMINATED.name());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DELETE_COMPLETED, DELETE_COMPLETED.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        clusterService.cleanupKerberosCredential(stack.getCluster());
        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stackUtil.extractAmbariIp(stack), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_NOTIFICATION_EMAIL, DELETE_COMPLETED.name());
        }
        usageService.closeUsagesForStack(stack.getId());
        if (deleteDependencies) {
            dependecyDeletionService.deleteDependencies(stack);
        }
        metricService.incrementMetricCounter(MetricType.STACK_TERMINATION_SUCCESSFUL, stack);
    }

    public void handleStackTerminationError(StackView stackView, StackFailureEvent payload, boolean forced, Boolean deleteDependencies) {
        String stackUpdateMessage;
        Msg eventMessage;
        DetailedStackStatus status;
        if (!forced) {
            Exception errorDetails = payload.getException();
            stackUpdateMessage = "Termination failed: " + errorDetails.getMessage();
            status = DetailedStackStatus.DELETE_FAILED;
            eventMessage = Msg.STACK_INFRASTRUCTURE_DELETE_FAILED;
            stackUpdater.updateStackStatus(stackView.getId(), status, stackUpdateMessage);
            LOGGER.warn("Error during stack termination flow: ", errorDetails);
        } else {
            terminationService.finalizeTermination(stackView.getId(), true);
            clusterService.updateClusterStatusByStackId(stackView.getId(), DELETE_COMPLETED);
            stackUpdateMessage = "Stack was force terminated.";
            status = DetailedStackStatus.DELETE_COMPLETED;
            eventMessage = Msg.STACK_FORCED_DELETE_COMPLETED;
            if (deleteDependencies) {
                dependecyDeletionService.deleteDependencies(stackView);
            }
        }
        flowMessageService.fireEventAndLog(stackView.getId(), eventMessage, status.name(), stackUpdateMessage);
        if (stackView.getClusterView() != null && stackView.getClusterView().getEmailNeeded()) {
            String ambariIp = stackUtil.extractAmbariIp(stackView);
            if (forced) {
                emailSenderService.sendTerminationSuccessEmail(stackView.getClusterView().getOwner(), stackView.getClusterView().getEmailTo(),
                        ambariIp, stackView.getClusterView().getName());
            } else {
                emailSenderService.sendTerminationFailureEmail(stackView.getClusterView().getOwner(), stackView.getClusterView().getEmailTo(),
                        ambariIp, stackView.getClusterView().getName());
            }
            flowMessageService.fireEventAndLog(stackView.getId(), Msg.STACK_NOTIFICATION_EMAIL, status.name());
        }
    }
}
