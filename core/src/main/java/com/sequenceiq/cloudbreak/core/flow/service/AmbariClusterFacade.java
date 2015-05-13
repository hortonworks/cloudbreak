package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.ClusterContainerRunner;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
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

@Service
public class AmbariClusterFacade implements ClusterFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterFacade.class);
    private static final int POLLING_INTERVAL = 5000;
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MAX_POLLING_ATTEMPTS = SEC_PER_MIN / (POLLING_INTERVAL / MS_PER_SEC) * 10;
    private static final String ADMIN = "admin";

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    @Autowired
    private AmbariStartupListenerTask ambariStartupListenerTask;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Autowired
    private StackService stackService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private CloudbreakEventService eventService;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private ClusterSecurityService securityService;

    @Autowired
    private ClusterContainerRunner containerRunner;

    @Override
    public FlowContext startAmbari(FlowContext context) throws Exception {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Starting Ambari. Context: {}", context);
        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, provisioningContext.getAmbariIp(),
                ambariClientProvider.getDefaultAmbariClient(provisioningContext.getAmbariIp()));

        PollingResult pollingResult = ambariStartupPollerObjectPollingService.pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

        if (isSuccess(pollingResult)) {
            LOGGER.info("Ambari has successfully started! Polling result: {}", pollingResult);
            assert provisioningContext.getAmbariIp() != null;

        } else {
            LOGGER.info("Could not start Ambari. polling result: {},  Context: {}", pollingResult, context);
            throw new CloudbreakException(String.format("Could not start Ambari. polling result: '%s',  Context: '%s'", pollingResult, context));
        }
        stack = stackUpdater.updateAmbariIp(stack.getId(), provisioningContext.getAmbariIp());
        String statusReason = "Cluster infrastructure and ambari are available on the cloud. AMBARI_IP:" + stack.getAmbariIp();
        stack = stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
        stackUpdater.updateStackStatusReason(stack.getId(), "");
        changeAmbariCredentials(provisioningContext.getAmbariIp(), stack);

        return provisioningContext;
    }

    @Override
    public FlowContext buildAmbariCluster(FlowContext context) throws Exception {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack.getCluster());
        LOGGER.debug("Building Ambari cluster. Context: {}", context);

        if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
            Cluster cluster = ambariClusterConnector.buildAmbariCluster(stack);
            if (cluster.getEmailNeeded()) {
                emailSenderService.sendSuccessEmail(cluster.getOwner(), stack.getAmbariIp());
            }
        } else {
            LOGGER.info("Ambari has started but there were no cluster request to this stack yet. Won't install cluster now.");
        }
        return provisioningContext;
    }

    @Override
    public FlowContext startCluster(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
            Stack stack = stackService.getById(startContext.getStackId());
            Cluster cluster = stack.getCluster();
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Starting cluster. Context: {}", context);
            if (cluster != null && Status.START_REQUESTED.equals(cluster.getStatus())) {
                clusterService.updateClusterStatusByStackId(stack.getId(), Status.START_IN_PROGRESS, "Services are starting.");
                stackUpdater.updateStackStatus(stack.getId(), Status.UPDATE_IN_PROGRESS, "Services are starting.");
                boolean started = ambariClusterConnector.startCluster(stack);
                if (started) {
                    LOGGER.info("Successfully started Hadoop services, setting cluster state to: {}", Status.AVAILABLE);
                    cluster.setUpSince(new Date().getTime());
                    cluster.setStatus(Status.AVAILABLE);
                    clusterService.updateCluster(cluster);
                    stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Cluster started successfully.");
                } else {
                    String statusReason = "Failed to start cluster, some of the services could not start.";
                    stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
                    throw new CloudbreakException(statusReason);
                }
                LOGGER.debug("Custer STARTED.");
            } else {
                LOGGER.info("Cluster start has not been requested, start cluster later.");
            }
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext stopCluster(FlowContext context) throws CloudbreakException {
        try {
            StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
            Cluster cluster = clusterService.retrieveClusterByStackId(startContext.getStackId());
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Stopping cluster. Context: {}", context);
            eventService.fireCloudbreakEvent(startContext.getStackId(), Status.STOP_IN_PROGRESS.name(), "Services are stopping.");
            boolean stopped = ambariClusterConnector.stopCluster(stackService.getById(startContext.getStackId()));
            if (stopped) {
                cluster.setStatus(Status.STOPPED);
                cluster = clusterService.updateCluster(cluster);
                eventService.fireCloudbreakEvent(startContext.getStackId(), Status.AVAILABLE.name(), "Services have been stopped successfully.");
                if (Status.STOP_REQUESTED.equals(stackService.getById(startContext.getStackId()).getStatus())) {
                    LOGGER.info("Hadoop services stopped, stack stop requested.");
                }
            } else {
                throw new CloudbreakException("Failed to stop cluster, some of the services could not stopped.");
            }
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster stop process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext handleStartFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster start failure. Context: {} ", context);
        return updateStackAndClusterStatus(context, Status.START_FAILED);
    }

    @Override
    public FlowContext handleStopFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster stop failure. Context: {} ", context);
        return updateStackAndClusterStatus(context, Status.STOP_FAILED);
    }

    @Override
    public FlowContext handleClusterCreationFailure(FlowContext flowContext) throws CloudbreakException {
        ProvisioningContext context = (ProvisioningContext) flowContext;
        MDCBuilder.buildMdcContext(clusterService.retrieveClusterByStackId(context.getStackId()));
        LOGGER.debug("Handling cluster creation failure. Context: {}", flowContext);
        Cluster cluster = clusterService.updateClusterStatusByStackId(context.getStackId(), Status.CREATE_FAILED, context.getErrorReason());
        stackUpdater.updateStackStatus(context.getStackId(), Status.AVAILABLE, "Cluster installation failed. Error: " + context.getErrorReason());
        eventService.fireCloudbreakEvent(context.getStackId(), Status.CREATE_FAILED.name(), context.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendFailureEmail(cluster.getOwner());
        }
        return context;
    }

    @Override
    public FlowContext handleSecurityEnableFailure(FlowContext flowContext) throws CloudbreakException {
        ProvisioningContext context = (ProvisioningContext) flowContext;
        Cluster cluster = clusterService.updateClusterStatusByStackId(context.getStackId(), Status.ENABLE_SECURITY_FAILED, context.getErrorReason());
        MDCBuilder.buildMdcContext(cluster);
        eventService.fireCloudbreakEvent(context.getStackId(), Status.ENABLE_SECURITY_FAILED.name(), context.getErrorReason());
        if (cluster.getEmailNeeded()) {
            emailSenderService.sendFailureEmail(cluster.getOwner());
        }
        return context;
    }

    @Override
    public FlowContext addClusterContainers(FlowContext context) throws CloudbreakException {
        try {
            containerRunner.addClusterContainers((ClusterScalingContext) context);
            return context;
        } catch (Exception e) {
            LOGGER.error("Error occurred while setting up containers for the cluster: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext upscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        LOGGER.info("Upscaling Cluster. Context: {}", context);
        Set<String> hostNames = ambariClusterConnector.installAmbariNode(scalingContext.getStackId(), scalingContext.getHostGroupAdjustment(),
                scalingContext.getCandidates());
        if (!hostNames.isEmpty()) {
            updateInstanceMetadataAfterScaling(false, hostNames, stack);
        }
        return context;
    }

    @Override
    public FlowContext runClusterContainers(FlowContext context) throws CloudbreakException {
        try {
            containerRunner.runClusterContainers((ProvisioningContext) context);
            return context;
        } catch (Exception e) {
            LOGGER.error("Error occurred while setting up containers for the cluster: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext downscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        MDCBuilder.buildMdcContext(stack.getCluster());
        LOGGER.info("Downscaling cluster. Context: {}", context);
        Set<String> hostNames = ambariClusterConnector.decommissionAmbariNodes(scalingContext.getStackId(), scalingContext.getHostGroupAdjustment(),
                scalingContext.getCandidates());
        if (!hostNames.isEmpty()) {
            updateInstanceMetadataAfterScaling(true, hostNames, stack);
        }
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), scalingContext.getHostGroupAdjustment().getHostGroup());
        StackScalingContext stackScalingContext = new StackScalingContext(scalingContext.getStackId(),
                scalingContext.getCloudPlatform(),
                scalingContext.getCandidates().size() * (-1),
                hostGroup.getInstanceGroup().getGroupName(),
                null,
                scalingContext.getScalingType(),
                null);
        return stackScalingContext;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        cluster.setStatus(Status.UPDATE_FAILED);
        cluster.setStatusReason(scalingContext.getErrorReason());
        stackUpdater.updateStackCluster(stack.getId(), cluster);
        Integer scalingAdjustment = scalingContext.getHostGroupAdjustment().getScalingAdjustment();
        String statusMessage = scalingAdjustment > 0 ? "New node(s) could not be added to the cluster:" : "Node(s) could not be removed from the cluster:";
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusMessage + scalingContext.getErrorReason());
        return context;
    }

    @Override
    public FlowContext resetAmbariCluster(FlowContext context) throws CloudbreakException {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(provisioningContext.getStackId());
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Reset Ambari Cluster. Context: {}", context);
        ambariClusterConnector.resetAmbariCluster(provisioningContext.getStackId());
        cluster.setStatus(Status.REQUESTED);
        stackUpdater.updateStackCluster(stack.getId(), cluster);
        return new ProvisioningContext.Builder()
                .withProvisioningContext(provisioningContext)
                .setAmbariIp(stack.getAmbariIp())
                .build();
    }

    public FlowContext enableSecurity(FlowContext context) throws CloudbreakException {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(provisioningContext.getStackId());
        if (stack.getCluster() == null) {
            LOGGER.debug("There is no cluster installed on the stack");
        } else {
            if (stack.getCluster().isSecure()) {
                LOGGER.debug("Cluster security is desired, trying to enable kerberos");
                securityService.enableKerberosSecurity(stack);
            } else {
                LOGGER.debug("Cluster security is not requested");
            }
        }
        return provisioningContext;
    }

    private void changeAmbariCredentials(String ambariIp, Stack stack) {
        String userName = stack.getUserName();
        String password = stack.getPassword();
        AmbariClient ambariClient = ambariClientProvider.getDefaultAmbariClient(ambariIp);
        if (ADMIN.equals(userName)) {
            if (!ADMIN.equals(password)) {
                ambariClient.changePassword(ADMIN, ADMIN, password, true);
            }
        } else {
            ambariClient.createUser(userName, password, true);
            ambariClient.deleteUser(ADMIN);
        }
    }

    private FlowContext updateStackAndClusterStatus(FlowContext context, Status clusterStatus) {
        StackStatusUpdateContext updateContext = (StackStatusUpdateContext) context;
        Cluster cluster = clusterService.retrieveClusterByStackId(updateContext.getStackId());
        MDCBuilder.buildMdcContext(cluster);
        cluster.setStatus(clusterStatus);
        clusterService.updateCluster(cluster);
        stackUpdater.updateStackStatus(updateContext.getStackId(), Status.AVAILABLE, updateContext.getErrorReason());
        return context;
    }

    private void updateInstanceMetadataAfterScaling(boolean decommission, Set<String> hostNames, Stack stack) {
        for (String hostName : hostNames) {
            if (decommission) {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.DECOMMISSIONED);
            } else {
                stackService.updateMetaDataStatus(stack.getId(), hostName, InstanceStatus.REGISTERED);
            }
        }
        String cause = decommission ? "Down" : "Up";
        String statusReason = String.format("%sscale of cluster finished successfully. AMBARI_IP:%s", cause, stack.getAmbariIp());
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
    }
}
