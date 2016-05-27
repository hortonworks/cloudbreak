package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.api.model.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_FAILED;

import java.util.Arrays;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;

@Service
public class SimpleStackFacade implements StackFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStackFacade.class);
    private static final String UPDATED_SUBNETS = "updated";
    private static final String REMOVED_SUBNETS = "removed";

    @Inject
    private StackService stackService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private StackScalingService stackScalingService;
    @Inject
    private MetadataSetupService metadataSetupService;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private ClusterBootstrapper clusterBootstrapper;
    @Inject
    private ClusterService clusterService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private SecurityRuleRepository securityRuleRepository;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private StackSyncService stackSyncService;
    @Inject
    private SecurityGroupService securityGroupService;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isDeleteInProgress()) {
                stackSyncService.sync(stack.getId(), !(actualContext instanceof StackScalingContext));
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the stack sync process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleStatusUpdateFailure(FlowContext flowContext) throws CloudbreakException {
        StackStatusUpdateContext context = (StackStatusUpdateContext) flowContext;
        Stack stack = stackService.getById(context.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (context.isStart()) {
            stackUpdater.updateStackStatus(context.getStackId(), START_FAILED, "Start failed: " + context.getErrorReason());
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_START_FAILED, START_FAILED.name(), context.getErrorReason());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
                if (stack.getCluster().getEmailNeeded()) {
                    emailSenderService.sendStartFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                    fireEventAndLog(context.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, START_FAILED.name());
                }
            }
        } else {
            stackUpdater.updateStackStatus(context.getStackId(), STOP_FAILED, "Stop failed: " + context.getErrorReason());
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STOP_FAILED, STOP_FAILED.name(), context.getErrorReason());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
                if (stack.getCluster().getEmailNeeded()) {
                    emailSenderService.sendStopFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                    fireEventAndLog(context.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, STOP_FAILED.name());
                }
            }
        }
        return context;
    }

    private void fireEventAndLog(Long stackId, FlowContext context, Msg msgCode, String eventType, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", msgCode, context);
        String message = messagesService.getMessage(msgCode.code(), Arrays.asList(args));
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message);
    }

    private enum Msg {
        STACK_INFRASTRUCTURE_BOOTSTRAP("stack.infrastructure.bootstrap"),
        STACK_INFRASTRUCTURE_METADATA_SETUP("stack.infrastructure.metadata.setup"),
        STACK_INFRASTRUCTURE_STARTING("stack.infrastructure.starting"),
        STACK_INFRASTRUCTURE_STARTED("stack.infrastructure.started"),
        STACK_BILLING_STARTED("stack.billing.started"),
        STACK_BILLING_STOPPED("stack.billing.stopped"),
        STACK_INFRASTRUCTURE_STOPPING("stack.infrastructure.stopping"),
        STACK_INFRASTRUCTURE_STOPPED("stack.infrastructure.stopped"),
        STACK_NOTIFICATION_EMAIL("stack.notification.email"),
        STACK_DELETE_IN_PROGRESS("stack.delete.in.progress"),
        STACK_DELETE_COMPLETED("stack.delete.completed"),
        STACK_FORCED_DELETE_COMPLETED("stack.forced.delete.completed"),
        STACK_ADDING_INSTANCES("stack.adding.instances"),
        STACK_REMOVING_INSTANCE("stack.removing.instance"),
        STACK_REMOVING_INSTANCE_FINISHED("stack.removing.instance.finished"),
        STACK_METADATA_EXTEND("stack.metadata.extend"),
        STACK_BOOTSTRAP_NEW_NODES("stack.bootstrap.new.nodes"),
        STACK_UPSCALE_FINISHED("stack.upscale.finished"),
        STACK_DOWNSCALE_INSTANCES("stack.downscale.instances"),
        STACK_DOWNSCALE_SUCCESS("stack.downscale.success"),
        STACK_PROVISIONING("stack.provisioning"),
        STACK_INFRASTRUCTURE_TIME("stack.infrastructure.time"),
        STACK_INFRASTRUCTURE_SUBNETS_UPDATING("stack.infrastructure.subnets.updating"),
        STACK_INFRASTRUCTURE_SUBNETS_UPDATED("stack.infrastructure.subnets.updated"),
        STACK_INFRASTRUCTURE_UPDATE_FAILED("stack.infrastructure.update.failed"),
        STACK_INFRASTRUCTURE_CREATE_FAILED("stack.infrastructure.create.failed"),
        STACK_INFRASTRUCTURE_ROLLBACK_FAILED("stack.infrastructure.rollback.failed"),
        STACK_INFRASTRUCTURE_DELETE_FAILED("stack.infrastructure.delete.failed"),
        STACK_INFRASTRUCTURE_START_FAILED("stack.infrastructure.start.failed"),
        STACK_INFRASTRUCTURE_STOP_FAILED("stack.infrastructure.stop.failed");

        private String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
