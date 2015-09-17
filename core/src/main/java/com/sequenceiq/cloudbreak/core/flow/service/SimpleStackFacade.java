package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.domain.BillingStatus.BILLING_STARTED;
import static com.sequenceiq.cloudbreak.domain.BillingStatus.BILLING_STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackInstanceUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow.context.UpdateAllowedSubnetsContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.OnFailureAction;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.CloudPlatformResolver;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulMetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.ProvisioningService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.service.stack.flow.TlsSetupService;

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
    private TerminationService terminationService;
    @Inject
    private StackStartService stackStartService;
    @Inject
    private StackStopService stackStopService;
    @Inject
    private StackScalingService stackScalingService;
    @Inject
    private MetadataSetupService metadataSetupService;
    @Inject
    private UserDataBuilder userDataBuilder;
    @Inject
    private HostGroupService hostGroupService;
    @Inject
    private ClusterBootstrapper clusterBootstrapper;
    @Inject
    private ConsulMetadataSetup consulMetadataSetup;
    @Inject
    private ProvisioningService provisioningService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private SecurityRuleRepository securityRuleRepository;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private TlsSetupService tlsSetupService;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private StackSyncService stackSyncService;
    @Inject
    private SecurityGroupService securityGroupService;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudPlatformResolver platformResolver;

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
        STACK_ADDING_INSTANCES("stack.adding.instances"),
        STACK_METADATA_EXTEND("stack.metadata.extend"),
        STACK_BOOTSTRAP_NEW_NODES("stack.bootstrap.new.nodes"),
        STACK_UPSCALE_FINISHED("stack.upscale.finished"),
        STACK_DOWNSCALE_INSTANCES("stack.downscale.instances"),
        STACK_DOWNSCALE_SUCCESS("stack.downscale.success"),
        STACK_STOP_REQUESTED("stack.stop.requested"),
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

    @Override
    public FlowContext bootstrapCluster(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS);
            fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_INFRASTRUCTURE_BOOTSTRAP, UPDATE_IN_PROGRESS.name());
            clusterBootstrapper.bootstrapCluster(actualContext);
        } catch (Exception e) {
            LOGGER.error("Error occurred while bootstrapping container orchestrator: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext setupConsulMetadata(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS);
            fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_INFRASTRUCTURE_METADATA_SETUP, UPDATE_IN_PROGRESS.name());
            consulMetadataSetup.setupConsulMetadata(stack.getId());
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        } catch (Exception e) {
            LOGGER.error("Exception during the consul metadata setup process.", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(actualContext.getStackId(), START_IN_PROGRESS, "Cluster infrastructure is now starting.");
            fireEventAndLog(actualContext.getStackId(), actualContext, Msg.STACK_INFRASTRUCTURE_STARTING, START_IN_PROGRESS.name());
            context = stackStartService.start(actualContext);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster infrastructure started successfully.");
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STARTED, AVAILABLE.name());
            fireEventAndLog(stack.getId(), context, Msg.STACK_BILLING_STARTED, BILLING_STARTED.name());
        } catch (Exception e) {
            LOGGER.error("Exception during the stack start process.", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext stop(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            if (stackStopService.isStopPossible(actualContext)) {
                Stack stack = stackService.getById(actualContext.getStackId());
                MDCBuilder.buildMdcContext(stack);
                stackUpdater.updateStackStatus(actualContext.getStackId(), STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
                fireEventAndLog(stack.getId(), actualContext, Msg.STACK_INFRASTRUCTURE_STOPPING, STOP_IN_PROGRESS.name());
                context = stackStopService.stop(actualContext);
                stackUpdater.updateStackStatus(stack.getId(), STOPPED, "Cluster infrastructure stopped successfully.");

                fireEventAndLog(stack.getId(), actualContext, Msg.STACK_INFRASTRUCTURE_STOPPED, STOPPED.name());

                fireEventAndLog(stack.getId(), actualContext, Msg.STACK_BILLING_STOPPED, BILLING_STOPPED.name());

                if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
                    emailSenderService.sendStopSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
                    fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, STOPPED.name());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext terminateStack(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(actualContext.getStackId(), DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS.name());

            if (stack != null && stack.getCredential() != null) {
                terminationService.terminateStack(stack.getId(), actualContext.getCloudPlatform());
            }

            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_BILLING_STOPPED, BILLING_STOPPED.name());

            stackUpdater.updateStackStatus(stack.getId(), DELETE_COMPLETED,
                    "The cluster and its infrastructure have successfully been terminated.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_DELETE_COMPLETED, DELETE_COMPLETED.name());

            if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
                emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
                fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, DELETE_COMPLETED.name());
            }
            terminationService.finalizeTermination(stack.getId());
            clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        } catch (Exception e) {
            LOGGER.error("Exception during the stack termination process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext addInstances(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            String statusReason = String.format("Adding %s new instance(s) to the infrastructure.", actualContext.getScalingAdjustment());
            stackUpdater.updateStackStatus(actualContext.getStackId(), UPDATE_IN_PROGRESS, statusReason);

            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_ADDING_INSTANCES, UPDATE_IN_PROGRESS.name(), actualContext.getScalingAdjustment());
            Set<Resource> resources = stackScalingService.addInstances(stack.getId(), actualContext.getInstanceGroup(), actualContext.getScalingAdjustment());
            context = new StackScalingContext(stack.getId(), actualContext.getCloudPlatform(), actualContext.getScalingAdjustment(),
                    actualContext.getInstanceGroup(), resources, actualContext.getScalingType(), null);
        } catch (Exception e) {
            LOGGER.error("Exception during the upscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext removeInstance(FlowContext context) throws CloudbreakException {
        StackInstanceUpdateContext actualContext = (StackInstanceUpdateContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isDeleteInProgress()) {
                stackScalingService.removeInstance(actualContext.getStackId(), actualContext.getInstanceId());
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the removing instance from the stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext extendMetadata(FlowContext context) throws CloudbreakException {
        StackScalingContext actualCont = (StackScalingContext) context;
        Stack stack = stackService.getById(actualCont.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(stack);
        fireEventAndLog(actualCont.getStackId(), context, Msg.STACK_METADATA_EXTEND, UPDATE_IN_PROGRESS.name());
        Integer scalingAdjustment = actualCont.getScalingAdjustment();
        Set<String> upscaleCandidateAddresses = metadataSetupService.setupNewMetadata(
                stack.getId(), actualCont.getResources(), actualCont.getInstanceGroup(), scalingAdjustment);
        HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
        hostGroupAdjustmentJson.setWithStackUpdate(false);
        hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        if (stack.getCluster() != null) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), actualCont.getInstanceGroup());
            hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
        }
        return new StackScalingContext(stack.getId(), actualCont.getCloudPlatform(), scalingAdjustment, actualCont.getInstanceGroup(),
                actualCont.getResources(), actualCont.getScalingType(), upscaleCandidateAddresses);
    }

    @Override
    public FlowContext bootstrapNewNodes(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_BOOTSTRAP_NEW_NODES, UPDATE_IN_PROGRESS.name());
            clusterBootstrapper.bootstrapNewNodes(actualContext);
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of munchausen setup: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext extendConsulMetadata(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            MDCBuilder.buildMdcContext(stack);
            consulMetadataSetup.setupNewConsulMetadata(stack.getId(), actualContext.getUpscaleCandidateAddresses());
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(actualContext.getScalingAdjustment());
            if (cluster != null) {
                HostGroup hostGroup = hostGroupService.getByClusterIdAndInstanceGroupName(cluster.getId(), actualContext.getInstanceGroup());
                hostGroupAdjustmentJson.setHostGroup(hostGroup.getName());
            }
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack upscale has been finished successfully.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_UPSCALE_FINISHED, AVAILABLE.name());
            context = new ClusterScalingContext(stack.getId(), actualContext.getCloudPlatform(),
                    hostGroupAdjustmentJson, actualContext.getUpscaleCandidateAddresses(), new ArrayList<HostMetadata>(), actualContext.getScalingType());
        } catch (Exception e) {
            LOGGER.error("Exception during the extend consul metadata phase: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext downscaleStack(FlowContext context) throws CloudbreakException {
        StackScalingContext actualContext = (StackScalingContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_DOWNSCALE_INSTANCES, UPDATE_IN_PROGRESS.name(), actualContext.getScalingAdjustment());

            stackScalingService.downscaleStack(stack.getId(), actualContext.getInstanceGroup(), actualContext.getScalingAdjustment());

            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Downscale of the cluster infrastructure finished successfully.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_DOWNSCALE_SUCCESS, AVAILABLE.name());

            if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
                emailSenderService.sendDownScaleSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
                fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, AVAILABLE.name());
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the downscaling of stack: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext stopRequested(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_STOP_REQUESTED, STOP_REQUESTED.name());
        } catch (Exception e) {
            LOGGER.error("Exception during the stack stop requested process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext provision(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Date startDate = new Date();
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), CREATE_IN_PROGRESS, "Creating infrastructure");
        fireEventAndLog(stack.getId(), actualContext, Msg.STACK_PROVISIONING, CREATE_IN_PROGRESS.name());

        ProvisionComplete provisionResult = provisioningService.buildStack(actualContext.getCloudPlatform(), stack, actualContext.getSetupProperties());
        Date endDate = new Date();
        long seconds = (endDate.getTime() - startDate.getTime()) / DateUtils.MILLIS_PER_SECOND;

        fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_TIME, AVAILABLE.name(), seconds);
        context = new ProvisioningContext.Builder()
                .setDefaultParams(provisionResult.getStackId(), provisionResult.getCloudPlatform())
                .setProvisionSetupProperties(actualContext.getSetupProperties())
                .setProvisionedResources(provisionResult.getResources())
                .build();
        return context;
    }

    @Override
    public FlowContext setupTls(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        tlsSetupService.setupTls(actualContext.getCloudPlatform(), stack);
        return actualContext;
    }

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            if (!stack.isDeleteInProgress()) {
                stackSyncService.sync(stack.getId());
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the stack sync process: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext actualContext = (UpdateAllowedSubnetsContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Updating allowed subnets.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_INFRASTRUCTURE_SUBNETS_UPDATING, UPDATE_IN_PROGRESS.name());

            CloudPlatform cloudPlatform = stack.cloudPlatform();
            CloudPlatformConnector connector = platformResolver.connector(cloudPlatform);
            Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(cloudPlatform,
                    tlsSecurityService.readPublicSshKey(stack.getId()), stack.getCredential().getLoginUserName(), connector.getPlatformParameters(stack));
            Map<String, Set<SecurityRule>> modifiedSubnets = getModifiedSubnetList(stack, actualContext.getAllowedSecurityRules());
            Set<SecurityRule> newSecurityRules = modifiedSubnets.get(UPDATED_SUBNETS);
            stack.getSecurityGroup().setSecurityRules(newSecurityRules);
            connector.updateAllowedSubnets(stack,
                    userdata.get(InstanceGroupType.GATEWAY), userdata.get(InstanceGroupType.CORE));
            securityRuleRepository.delete(modifiedSubnets.get(REMOVED_SUBNETS));
            securityRuleRepository.save(newSecurityRules);

            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Allowed subnets successfully updated.");
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_INFRASTRUCTURE_SUBNETS_UPDATED, AVAILABLE.name());
        } catch (Exception e) {
            Stack stack = stackService.getById(actualContext.getStackId());
            SecurityGroup securityGroup = stack.getSecurityGroup();
            String msg = String.format("Failed to update security group with allowed subnets: %s", securityGroup);
            if (stack != null && stack.isStackInDeletionPhase()) {
                msg = String.format("Failed to update security group with allowed subnets: %s; stack is already in deletion phase.",
                        securityGroup);
            }
            LOGGER.error(msg, e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        UpdateAllowedSubnetsContext actualContext = (UpdateAllowedSubnetsContext) context;
        try {
            Stack stack = stackService.getById(actualContext.getStackId());
            MDCBuilder.buildMdcContext(stack);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("Stack update failed. %s", actualContext.getErrorReason()));
            fireEventAndLog(stack.getId(), actualContext, Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, AVAILABLE.name(), actualContext.getErrorReason());
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        try {
            Long id = null;
            String errorReason = null;
            if (context instanceof StackScalingContext) {
                StackScalingContext actualContext = (StackScalingContext) context;
                id = actualContext.getStackId();
                errorReason = actualContext.getErrorReason();
            } else if (context instanceof ClusterScalingContext) {
                ClusterScalingContext actualContext = (ClusterScalingContext) context;
                id = actualContext.getStackId();
                errorReason = actualContext.getErrorReason();
            }
            if (id != null) {
                Stack stack = stackService.getById(id);
                MDCBuilder.buildMdcContext(stack);
                stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack update failed. " + errorReason);
                fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED, AVAILABLE.name(), errorReason);
            }
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of stack scaling failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
        return context;
    }

    @Override
    public FlowContext handleCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        final Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        try {
            String errorReason = actualContext.getErrorReason();
            fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, UPDATE_IN_PROGRESS.name(), errorReason);
            if (!stack.isStackInDeletionPhase()) {
                final CloudPlatform cloudPlatform = actualContext.getCloudPlatform();
                if (!stack.getOnFailureActionAction().equals(OnFailureAction.ROLLBACK)) {
                    LOGGER.debug("Nothing to do. OnFailureAction {}", stack.getOnFailureActionAction());
                } else {
                    stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
                    platformResolver.connector(cloudPlatform).rollback(stack, stack.getResources());
                    fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, BILLING_STOPPED.name(), errorReason);
                }
                stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, errorReason);
                fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_CREATE_FAILED, CREATE_FAILED.name(), errorReason);
            }

            context = new ProvisioningContext.Builder().setDefaultParams(stack.getId(), stack.cloudPlatform()).build();
        } catch (Exception ex) {
            LOGGER.error("Stack rollback failed on stack id : {}. Exception:", stack.getId(), ex);
            stackUpdater.updateStackStatus(stack.getId(), CREATE_FAILED, String.format("Rollback failed: %s", ex.getMessage()));
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_ROLLBACK_FAILED, CREATE_FAILED.name(), ex.getMessage());
            throw new CloudbreakException(String.format("Stack rollback failed on {} stack: ", stack.getId(), ex));
        }
        return context;
    }

    @Override
    public FlowContext handleTerminationFailure(FlowContext context) throws CloudbreakException {
        DefaultFlowContext actualContext = (DefaultFlowContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), DELETE_FAILED, "Termination failed: " + actualContext.getErrorReason());
        fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_DELETE_FAILED, DELETE_FAILED.name(), actualContext.getErrorReason());
        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendTerminationFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
            fireEventAndLog(actualContext.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, DELETE_FAILED.name());
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
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_START_FAILED, START_FAILED.name());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
                if (stack.getCluster().getEmailNeeded()) {
                    emailSenderService.sendStartFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
                    fireEventAndLog(context.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, START_FAILED.name());
                }
            }
        } else {
            stackUpdater.updateStackStatus(context.getStackId(), STOP_FAILED, "Stop failed: " + context.getErrorReason());
            fireEventAndLog(stack.getId(), context, Msg.STACK_INFRASTRUCTURE_STOP_FAILED, STOP_FAILED.name(), context.getErrorReason());
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(context.getStackId(), STOPPED);
                if (stack.getCluster().getEmailNeeded()) {
                    emailSenderService.sendStopFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
                    fireEventAndLog(context.getStackId(), context, Msg.STACK_NOTIFICATION_EMAIL, STOP_FAILED.name());
                }
            }
        }
        return context;
    }

    private Map<String, Set<SecurityRule>> getModifiedSubnetList(Stack stack, List<SecurityRule> securityRuleList) {
        Map<String, Set<SecurityRule>> result = new HashMap<>();
        Set<SecurityRule> removed = new HashSet<>();
        Set<SecurityRule> updated = new HashSet<>();
        Long securityGroupId = stack.getSecurityGroup().getId();
        Set<SecurityRule> securityRules = securityGroupService.get(securityGroupId).getSecurityRules();
        for (SecurityRule securityRule : securityRules) {
            if (!securityRule.isModifiable()) {
                updated.add(securityRule);
                removeFromNewSubnetList(securityRule, securityRuleList);
            } else {
                removed.add(securityRule);
            }
        }
        for (SecurityRule securityRule : securityRuleList) {
            updated.add(securityRule);
        }
        result.put(UPDATED_SUBNETS, updated);
        result.put(REMOVED_SUBNETS, removed);
        return result;
    }

    private void removeFromNewSubnetList(SecurityRule securityRule, List<SecurityRule> securityRuleList) {
        Iterator<SecurityRule> iterator = securityRuleList.iterator();
        String cidr = securityRule.getCidr();
        while (iterator.hasNext()) {
            SecurityRule next = iterator.next();
            if (next.getCidr().equals(cidr)) {
                iterator.remove();
            }
        }
    }

    private void fireEventAndLog(Long stackId, FlowContext context, Msg msgCode, String eventType, Object... args) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", msgCode, context);
        String message = messagesService.getMessage(msgCode.code(), Arrays.asList(args));
        cloudbreakEventService.fireCloudbreakEvent(stackId, eventType, message);
    }
}
