package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.domain.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.domain.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.STOPPED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.domain.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.domain.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.service.SimpleSecurityService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Service
public class AmbariClusterFacade implements ClusterFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterFacade.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;
    private static final String ADMIN = "admin";

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private AmbariStartupListenerTask ambariStartupListenerTask;

    @Inject
    private RetryingStackUpdater stackUpdater;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterSecurityService securityService;

    @Inject
    private ClusterContainerRunner containerRunner;

    @Inject
    private SimpleSecurityService simpleSecurityService;

    @Override
    public FlowContext startAmbari(FlowContext context) throws Exception {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster == null) {
            LOGGER.debug("There is no cluster installed on the stack, skipping start Ambari step");
        } else {
            MDCBuilder.buildMdcContext(cluster);

            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Start ambari cluster", UPDATE_IN_PROGRESS);
            TLSClientConfig clientConfig = simpleSecurityService.buildTLSClientConfig(stack.getId(), actualContext.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(clientConfig);
            AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, actualContext.getAmbariIp(), ambariClient);
            PollingResult pollingResult = ambariStartupPollerObjectPollingService
                    .pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            logAfter(actualContext.getStackId(), context, "Start ambari cluster", UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Check ambari cluster", UPDATE_IN_PROGRESS);
            if (isSuccess(pollingResult)) {
                LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
            } else {
                LOGGER.info("Could not start Ambari. polling result: {},  Context: {}", pollingResult, context);
                throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s',  Context: '%s'", pollingResult, context));
            }
            logAfter(actualContext.getStackId(), context, "Check ambari cluster", UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Update ambari ip", UPDATE_IN_PROGRESS);
            clusterService.updateAmbariIp(cluster.getId(), actualContext.getAmbariIp());
            logAfter(actualContext.getStackId(), context, "Update ambari ip", UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Changing ambari credentials", UPDATE_IN_PROGRESS);
            changeAmbariCredentials(actualContext.getAmbariIp(), stack);
            logAfter(actualContext.getStackId(), context, "Changing ambari credentials", UPDATE_IN_PROGRESS);
        }
        return context;
    }

    @Override
    public FlowContext buildAmbariCluster(FlowContext context) throws Exception {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster == null) {
            LOGGER.debug("There is no cluster installed on the stack, skipping build Ambari step");
        } else {
            MDCBuilder.buildMdcContext(cluster);

            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Build ambari cluster", UPDATE_IN_PROGRESS);
            cluster = ambariClusterConnector.buildAmbariCluster(stack);
            logAfter(actualContext.getStackId(), context, "Build ambari cluster", AVAILABLE);

            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);

            if (cluster.getEmailNeeded()) {
                logBefore(actualContext.getStackId(), context, "Sending cluster installation email", AVAILABLE);
                emailSenderService.sendSuccessEmail(cluster.getOwner(), stack.getAmbariIp());
                logAfter(actualContext.getStackId(), context, "Sending cluster installation email", AVAILABLE);
            }
        }
        return context;
    }

    @Override
    public FlowContext startCluster(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster != null && (cluster.isStartRequested())) {
            MDCBuilder.buildMdcContext(cluster);

            clusterService.updateClusterStatusByStackId(stack.getId(), START_IN_PROGRESS);
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Starting ambari cluster", START_IN_PROGRESS);
            ambariClusterConnector.startCluster(stack);
            logAfter(actualContext.getStackId(), context, "Starting ambari cluster", AVAILABLE);

            cluster.setUpSince(new Date().getTime());
            clusterService.updateCluster(cluster);
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        } else {
            LOGGER.info("Cluster start has not been requested, start cluster later.");
        }
        return context;
    }

    @Override
    public FlowContext stopCluster(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);

        Status stackStatus = stack.getStatus();
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_IN_PROGRESS);

        logBefore(actualContext.getStackId(), context, "Stopping ambari cluster", UPDATE_IN_PROGRESS);
        ambariClusterConnector.stopCluster(stack);
        logAfter(actualContext.getStackId(), context, "Stopping ambari cluster", STOPPED);

        stack = stackService.getById(actualContext.getStackId());
        if (!stackStatus.equals(stack.getStatus())) {
            stackUpdater.updateStackStatus(stack.getId(), stack.isStopRequested() ? STOP_REQUESTED : stackStatus);
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), STOPPED);
        eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), "Services have been stopped successfully.");

        return context;
    }

    @Override
    public FlowContext addClusterContainers(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);

        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);

        logBefore(actualContext.getStackId(), context, "Add new containers to the ambari cluster", UPDATE_IN_PROGRESS);
        containerRunner.addClusterContainers(actualContext);
        logAfter(actualContext.getStackId(), context, "Add new containers to the ambari cluster", UPDATE_IN_PROGRESS);

        return context;
    }

    @Override
    public FlowContext upscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);

        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);

        logBefore(actualContext.getStackId(), context, "Upscale ambari cluster", UPDATE_IN_PROGRESS);
        Set<String> hostNames = ambariClusterConnector.installAmbariNode(stack.getId(), actualContext.getHostGroupAdjustment(), actualContext.getCandidates());
        updateInstanceMetadataAfterScaling(false, hostNames, stack);
        String statusReason = String.format("Upscale of cluster finished successfully. AMBARI_IP:%s", stack.getAmbariIp());
        logAfter(actualContext.getStackId(), context, "Upscale ambari cluster", AVAILABLE);

        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), statusReason);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);

        return context;
    }

    @Override
    public FlowContext runClusterContainers(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(stack);
        if (stack.getCluster() != null && cluster.isRequested()) {
            MDCBuilder.buildMdcContext(cluster);

            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);

            logBefore(actualContext.getStackId(), context, "Run cluster containers in ambari cluster", UPDATE_IN_PROGRESS);
            containerRunner.runClusterContainers(actualContext);
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            logAfter(actualContext.getStackId(), context, "Run cluster containers in ambari cluster", UPDATE_IN_PROGRESS);

            context = new ProvisioningContext.Builder()
                    .setDefaultParams(stack.getId(), actualContext.getCloudPlatform())
                    .setAmbariIp(gatewayInstance.getPublicIp())
                    .build();
        } else {
            LOGGER.info("The stack has started but there were no cluster request, yet. Won't install cluster now.");
            return actualContext;
        }
        return context;
    }

    @Override
    public FlowContext downscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);

        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                String.format("Removing '%s' node(s) from the cluster.", actualContext.getHostGroupAdjustment().getScalingAdjustment()));

        logBefore(actualContext.getStackId(), context, "Downscale ambari cluster", UPDATE_IN_PROGRESS);
        Set<String> hostNames = ambariClusterConnector
                .decommissionAmbariNodes(stack.getId(), actualContext.getHostGroupAdjustment(), actualContext.getCandidates());
        updateInstanceMetadataAfterScaling(true, hostNames, stack);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), actualContext.getHostGroupAdjustment().getHostGroup());
        logAfter(actualContext.getStackId(), context, "Downscale ambari cluster", AVAILABLE);

        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        String statusReason = String.format("Downscale of cluster finished successfully. AMBARI_IP:%s", stack.getAmbariIp());
        eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), statusReason);

        context = new StackScalingContext(stack.getId(),
                actualContext.getCloudPlatform(), actualContext.getCandidates().size() * (-1), hostGroup.getInstanceGroup().getGroupName(),
                null, actualContext.getScalingType(), null);
        return context;
    }

    @Override
    public FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Reset ambari cluster", UPDATE_IN_PROGRESS);
        ambariClusterConnector.resetAmbariCluster(stack.getId());
        logAfter(actualContext.getStackId(), context, "Reset ambari cluster", UPDATE_IN_PROGRESS);

        clusterService.updateClusterStatusByStackId(stack.getId(), REQUESTED);

        context = new ProvisioningContext.Builder()
                .withProvisioningContext(actualContext)
                .setAmbariIp(stack.getAmbariIp())
                .build();
        return context;
    }

    public FlowContext enableSecurity(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(stack);
        if (cluster == null) {
            LOGGER.debug("There is no cluster installed on the stack");
        } else {
            MDCBuilder.buildMdcContext(cluster);

            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS);
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);

            if (cluster.isSecure()) {
                logBefore(actualContext.getStackId(), context, "Enable security on ambari cluster", UPDATE_IN_PROGRESS);
                securityService.enableKerberosSecurity(stack);
                logAfter(actualContext.getStackId(), context, "Enable security on ambari cluster", AVAILABLE);
            } else {
                LOGGER.debug("Cluster security is not requested");
            }

            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        }
        return context;
    }

    @Override
    public FlowContext startRequested(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);

        logBefore(actualContext.getStackId(), context, "Start requested on ambari cluster", UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        logAfter(actualContext.getStackId(), context, "Start requested on ambari cluster", UPDATE_IN_PROGRESS);

        return context;
    }

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);

        Status status = UPDATE_IN_PROGRESS;

        logBefore(actualContext.getStackId(), context, "Start sync on ambari cluster", UPDATE_IN_PROGRESS);
        if (ambariClusterConnector.isAmbariAvailable(stack)) {
            if (!cluster.isDeleteInProgress()) {
                clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
                status = AVAILABLE;
            }
        } else {
            if (stack.getCluster() != null) {
                clusterService.updateClusterStatusByStackId(stack.getId(), STOPPED);
                status = STOPPED;
            }
        }
        logAfter(actualContext.getStackId(), context, "Start sync on ambari cluster", status);

        return context;
    }

    @Override
    public FlowContext handleStartFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Start failed on ambari cluster", UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), START_FAILED);
        stackUpdater.updateStackStatus(actualContext.getStackId(), AVAILABLE, actualContext.getErrorReason());
        logAfter(actualContext.getStackId(), context, "Start failed on ambari cluster", AVAILABLE);

        return context;
    }

    @Override
    public FlowContext handleStopFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Stop failed on ambari cluster", UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, actualContext.getErrorReason());
        logAfter(actualContext.getStackId(), context, "Stop failed on ambari cluster", AVAILABLE);

        return context;
    }

    @Override
    public FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Creation of ambari cluster failed", CREATE_FAILED);
        clusterService.updateClusterStatusByStackId(stack.getId(), CREATE_FAILED, actualContext.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        logAfter(actualContext.getStackId(), context, "Creation of ambari cluster failed", AVAILABLE);

        eventService.fireCloudbreakEvent(stack.getId(), CREATE_FAILED.name(), actualContext.getErrorReason());

        if (cluster.getEmailNeeded()) {
            logBefore(actualContext.getStackId(), context, "Sending cluster creation failed email", AVAILABLE);
            emailSenderService.sendFailureEmail(cluster.getOwner());
            logAfter(actualContext.getStackId(), context, "Sending cluster creation failed email", AVAILABLE);
        }

        return actualContext;
    }

    @Override
    public FlowContext handleSecurityEnableFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Enable security failed on ambari cluster", ENABLE_SECURITY_FAILED);
        clusterService.updateClusterStatusByStackId(stack.getId(), ENABLE_SECURITY_FAILED, actualContext.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        logAfter(actualContext.getStackId(), context, "Enable security failed on ambari cluster", AVAILABLE);

        eventService.fireCloudbreakEvent(stack.getId(), ENABLE_SECURITY_FAILED.name(), actualContext.getErrorReason());

        if (cluster.getEmailNeeded()) {
            logBefore(actualContext.getStackId(), context, "Sending security enable failed email", AVAILABLE);
            emailSenderService.sendFailureEmail(cluster.getOwner());
            logAfter(actualContext.getStackId(), context, "Sending security enable failed email", AVAILABLE);
        }

        return context;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);

        logBefore(actualContext.getStackId(), context, "Scaling failed on ambari cluster", UPDATE_FAILED);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, actualContext.getErrorReason());
        Integer scalingAdjustment = actualContext.getHostGroupAdjustment().getScalingAdjustment();
        String statusMessage = scalingAdjustment > 0 ? "New node(s) could not be added to the cluster:" : "Node(s) could not be removed from the cluster:";
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("%s %s", statusMessage, actualContext.getErrorReason()));
        logAfter(actualContext.getStackId(), context, "Scaling failed on ambari cluster", AVAILABLE);

        return context;
    }

    private void changeAmbariCredentials(String ambariIp, Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        TLSClientConfig clientConfig = simpleSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
        AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(clientConfig);
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariClient.changePassword(ADMIN, ADMIN, password, true);
            }
        } else {
            ambariClient.createUser(userName, password, true);
            ambariClient.deleteUser(ADMIN);
        }
    }

    private void updateInstanceMetadataAfterScaling(boolean decommission, Set<String> hostNames, Stack stack) {
        for (String hostName : hostNames) {
            if (decommission) {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.DECOMMISSIONED);
            } else {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.REGISTERED);
            }
        }
    }

    private void logBefore(Long stackId, FlowContext context, String eventMessage, Status eventType) {
        LOGGER.debug("{} [CLUSTER_FLOW_STEP] [STARTED]. Context: {}", eventMessage, context);
        eventService.fireCloudbreakEvent(stackId, eventType.name(), String.format("%s started.", eventMessage));
    }

    private void logAfter(Long stackId, FlowContext context, String eventMessage, Status eventType) {
        LOGGER.debug("{} [CLUSTER_FLOW_STEP] [FINISHED]. Context: {}", eventMessage, context);
        eventService.fireCloudbreakEvent(stackId, eventType.name(), String.format("%s finished.", eventMessage));
    }

}
