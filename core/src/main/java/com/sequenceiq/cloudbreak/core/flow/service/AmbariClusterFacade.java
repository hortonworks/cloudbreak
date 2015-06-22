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
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
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
import com.sequenceiq.cloudbreak.repository.StackUpdater;
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
    private StackUpdater stackUpdater;

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
    private TlsSecurityService tlsSecurityService;

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
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Ambari cluster is now starting.");
            clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
            TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), actualContext.getAmbariIp());
            AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(clientConfig);
            AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, actualContext.getAmbariIp(), ambariClient);
            PollingResult pollingResult = ambariStartupPollerObjectPollingService
                    .pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

            if (isSuccess(pollingResult)) {
                LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
            } else {
                LOGGER.info("Could not start Ambari. polling result: {},  Context: {}", pollingResult, context);
                throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s',  Context: '%s'", pollingResult, context));
            }
            clusterService.updateAmbariIp(cluster.getId(), actualContext.getAmbariIp());
            changeAmbariCredentials(actualContext.getAmbariIp(), stack);
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
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, String.format("Building the Ambari cluster. Ambari ip:%s", stack.getAmbariIp()));
            cluster = ambariClusterConnector.buildAmbariCluster(stack);
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster creation finished.");
            if (cluster.getEmailNeeded()) {
                emailSenderService.sendProvisioningSuccessEmail(cluster.getOwner(), stack.getAmbariIp());
                fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about state of the cluster.", AVAILABLE);
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
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, String.format("Starting the Ambari cluster. Ambari ip:%s", stack.getAmbariIp()));
            ambariClusterConnector.startCluster(stack);
            cluster.setUpSince(new Date().getTime());
            clusterService.updateCluster(cluster);
            clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
            stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Ambari cluster started successfully.");
            if (cluster.getEmailNeeded()) {
                emailSenderService.sendStartSuccessEmail(cluster.getOwner(), stack.getAmbariIp());
                fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about state of the cluster.", AVAILABLE);
            }
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
        fireEventAndLog(stack.getId(), context, "Stopping Ambari cluster.", UPDATE_IN_PROGRESS);
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_IN_PROGRESS);
        ambariClusterConnector.stopCluster(stack);
        stack = stackService.getById(actualContext.getStackId());
        if (!stackStatus.equals(stack.getStatus())) {
            stackUpdater.updateStackStatus(stack.getId(), stack.isStopRequested() ? STOP_REQUESTED : stackStatus);
        }
        clusterService.updateClusterStatusByStackId(stack.getId(), STOPPED);
        fireEventAndLog(stack.getId(), context, "Ambari cluster has been stopped successfully.", STOPPED);
        return context;
    }

    @Override
    public FlowContext addClusterContainers(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Adding new containers to the cluster.");
        containerRunner.addClusterContainers(actualContext);
        return context;
    }

    @Override
    public FlowContext upscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Scaling up ambari cluster.");
        Set<String> hostNames = ambariClusterConnector.installAmbariNode(stack.getId(), actualContext.getHostGroupAdjustment(), actualContext.getCandidates());
        updateInstanceMetadataAfterScaling(false, hostNames, stack);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Upscale of cluster finished successfully.");
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendUpscaleSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
            fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about state of the cluster.", AVAILABLE);
        }
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
            stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Running cluster containers.");
            containerRunner.runClusterContainers(actualContext);
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
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
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Scaling down the Ambari cluster.");
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
        Set<String> hostNames = ambariClusterConnector
                .decommissionAmbariNodes(stack.getId(), actualContext.getHostGroupAdjustment(), actualContext.getCandidates());
        updateInstanceMetadataAfterScaling(true, hostNames, stack);
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), actualContext.getHostGroupAdjustment().getHostGroup());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Downscale of cluster finished successfully.");
        clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
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
        fireEventAndLog(stack.getId(), context, "Resetting Ambari cluster", UPDATE_IN_PROGRESS);
        ambariClusterConnector.resetAmbariCluster(stack.getId());
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
            if (cluster.isSecure()) {
                stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Configuring cluster security.");
                clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_IN_PROGRESS);
                securityService.enableKerberosSecurity(stack);
                clusterService.updateClusterStatusByStackId(stack.getId(), AVAILABLE);
                stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Cluster security has been configured.");
            } else {
                LOGGER.info("Cluster security is not requested.");
            }
        }
        return context;
    }

    @Override
    public FlowContext startRequested(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        fireEventAndLog(stack.getId(), context, "Starting of cluster has been requested.", START_REQUESTED);
        clusterService.updateClusterStatusByStackId(stack.getId(), START_REQUESTED);
        return context;
    }

    @Override
    public FlowContext sync(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        Status status = UPDATE_IN_PROGRESS;
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
        fireEventAndLog(stack.getId(), context, "Synced cluster state with Ambari.", status);
        return context;
    }

    @Override
    public FlowContext handleStartFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), START_FAILED);
        stackUpdater.updateStackStatus(actualContext.getStackId(), AVAILABLE, "Cluster could not be started: " + actualContext.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStartFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
            fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about state of the cluster.", START_FAILED);
        }
        return context;
    }

    @Override
    public FlowContext handleStopFailure(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext actualContext = (StackStatusUpdateContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), STOP_FAILED);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "The Ambari cluster could not be stopped: " + actualContext.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendStopFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp());
            fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about state of the cluster.", STOP_FAILED);
        }
        return context;
    }

    @Override
    public FlowContext handleClusterCreationFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(actualContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), CREATE_FAILED, actualContext.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        fireEventAndLog(stack.getId(), context, "Ambari cluster could not be created: " + actualContext.getErrorReason(), CREATE_FAILED);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningFailureEmail(cluster.getOwner());
            fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about the failure of cluster creation.", AVAILABLE);
        }

        return actualContext;
    }

    @Override
    public FlowContext handleSecurityEnableFailure(FlowContext context) throws CloudbreakException {
        ProvisioningContext actualContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), ENABLE_SECURITY_FAILED, actualContext.getErrorReason());
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE);
        fireEventAndLog(stack.getId(), context, "Enabling security failed on Ambari cluster: " + actualContext.getErrorReason(), ENABLE_SECURITY_FAILED);
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendProvisioningFailureEmail(cluster.getOwner());
            fireEventAndLog(actualContext.getStackId(), context, "Notification email has been sent about the failure of cluster creation.", AVAILABLE);
        }

        return context;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        ClusterScalingContext actualContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(actualContext.getStackId());
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
        MDCBuilder.buildMdcContext(cluster);
        clusterService.updateClusterStatusByStackId(stack.getId(), UPDATE_FAILED, actualContext.getErrorReason());
        Integer scalingAdjustment = actualContext.getHostGroupAdjustment().getScalingAdjustment();
        String statusMessage = scalingAdjustment > 0 ? "New node(s) could not be added to the cluster:" : "Node(s) could not be removed from the cluster:";
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("%s %s", statusMessage, actualContext.getErrorReason()));
        return context;
    }

    private void changeAmbariCredentials(String ambariIp, Stack stack) throws CloudbreakSecuritySetupException {
        Cluster cluster = stack.getCluster();
        LOGGER.info("Changing ambari credentials for cluster: {}, ambari ip: {}", cluster.getName(), ambariIp);
        String userName = cluster.getUserName();
        String password = cluster.getPassword();
        TLSClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(stack.getId(), cluster.getAmbariIp());
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

    private void fireEventAndLog(Long stackId, FlowContext context, String eventMessage, Status eventType) {
        LOGGER.debug("{} [STACK_FLOW_STEP]. Context: {}", eventMessage, context);
        eventService.fireCloudbreakEvent(stackId, eventType.name(), eventMessage);
    }
}
