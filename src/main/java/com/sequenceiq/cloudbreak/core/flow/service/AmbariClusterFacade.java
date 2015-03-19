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
import com.sequenceiq.cloudbreak.core.flow.FlowContextFactory;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
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
    private AmbariRoleAllocator ambariRoleAllocator;

    @Autowired
    private AmbariClientService clientService;

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
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public FlowContext allocateAmbariRoles(FlowContext context) throws Exception {
        LOGGER.debug("Allocating Ambari roles. Context: {}", context);
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        AmbariRoleAllocationComplete ambariRoleAllocationComplete = ambariRoleAllocator
                .allocateRoles(provisioningContext.getStackId(), provisioningContext.getCoreInstanceMetaData());
        return FlowContextFactory.createAmbariStartContext(ambariRoleAllocationComplete.getStack().getId(), ambariRoleAllocationComplete.getAmbariIp());
    }

    @Override
    public FlowContext startAmbari(FlowContext context) throws Exception {
        ProvisioningContext provisioningContext = (ProvisioningContext) context;
        Stack stack = stackService.getById(provisioningContext.getStackId());
        MDCBuilder.buildMdcContext(stack);

        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, provisioningContext.getAmbariIp(),
                clientService.createDefault(provisioningContext.getAmbariIp()));

        PollingResult pollingResult = ambariStartupPollerObjectPollingService.pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

        if (isSuccess(pollingResult)) {
            LOGGER.info("Stack has been successfully created!");
            assert provisioningContext.getAmbariIp() != null;

        } else {
            throw new CloudbreakException("Stack creation failed. Context:" + context);
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
        MDCBuilder.buildMdcContext(stack);

        if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
            ambariClusterConnector.buildAmbariCluster(stack);
        } else {
            LOGGER.info("Ambari has started but there were no cluster request to this stack yet. Won't install cluster now.");
        }
        return provisioningContext;
    }

    @Override
    public FlowContext startCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Starting cluster. Context: {}", context);
        try {
            StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
            Stack stack = stackService.getById(startContext.getStackId());
            Cluster cluster = stack.getCluster();
            MDCBuilder.buildMdcContext(cluster);
            boolean started = ambariClusterConnector.startCluster(stack);
            if (started) {
                LOGGER.info("Successfully started Hadoop services, setting cluster state to: {}", Status.AVAILABLE);
                cluster.setUpSince(new Date().getTime());
                cluster.setStatus(Status.AVAILABLE);
                clusterService.updateCluster(cluster);
                eventService.fireCloudbreakEvent(startContext.getStackId(), Status.AVAILABLE.name(), "Cluster started successfully.");
            } else {
                throw new CloudbreakException("Failed to start cluster, some of the services could not start.");
            }
            LOGGER.debug("Starting cluster is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster start process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext stopCluster(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stopping cluster. Context: {}", context);
        try {
            StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
            Cluster cluster = clusterService.retrieveCluster(startContext.getStackId());
            MDCBuilder.buildMdcContext(cluster);
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
    public FlowContext clusterStartError(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster start failure. Context: {} ", context);
        try {
            StackStatusUpdateContext startContext = (StackStatusUpdateContext) context;
            Cluster cluster = clusterService.retrieveCluster(startContext.getStackId());
            cluster.setStatus(Status.STOPPED);
            clusterService.updateCluster(cluster);
            stackUpdater.updateStackStatus(startContext.getStackId(), Status.AVAILABLE, startContext.getStatusReason());
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during handling cluster start failure", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext clusterStopError(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling cluster stop failure. Context: {} ", context);
        try {
            StackStatusUpdateContext stopContext = (StackStatusUpdateContext) context;
            Cluster cluster = clusterService.retrieveCluster(stopContext.getStackId());
            MDCBuilder.buildMdcContext(cluster);

            eventService.fireCloudbreakEvent(stopContext.getStackId(), Status.STOP_IN_PROGRESS.name(), "Services are stopping.");
            boolean stopped = ambariClusterConnector.stopCluster(stackService.getById(stopContext.getStackId()));
            if (stopped) {
                cluster.setStatus(Status.STOPPED);
                cluster = clusterService.updateCluster(cluster);
                eventService.fireCloudbreakEvent(stopContext.getStackId(), Status.AVAILABLE.name(), "Services have been stopped successfully.");
                if (Status.STOP_REQUESTED.equals(stackService.getById(stopContext.getStackId()).getStatus())) {
                    LOGGER.info("Hadoop services stopped, stack stop requested.");
                }
            } else {
                throw new CloudbreakException("Failed to stop cluster, some of the services could not stopped.");
            }
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during handling cluster stop failure", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext clusterCreationFailed(FlowContext flowContext) throws CloudbreakException {
        LOGGER.debug("Handling cluster creation failure. Context: {}", flowContext);
        ProvisioningContext context = (ProvisioningContext) flowContext;

        Cluster cluster = clusterService.updateClusterStatus(context.getClusterId(), Status.CREATE_FAILED, context.getMessage());
        MDCBuilder.buildMdcContext(cluster);

        stackUpdater.updateStackStatus(context.getStackId(), Status.AVAILABLE, "Cluster installation failed. Error: " + context.getMessage());
        eventService.fireCloudbreakEvent(context.getStackId(), "CLUSTER_CREATION_FAILED", context.getMessage());
        if (cluster.getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendFailEmail(cluster.getOwner());
        }
        return context;
    }

    @Override
    public FlowContext upscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        Set<String> hostNames = ambariClusterConnector.installAmbariNode(scalingContext.getStackId(), scalingContext.getHostGroupAdjustment());
        if (!hostNames.isEmpty()) {
            updateInstanceMetadataAfterScaling(false, hostNames, stack);
        }
        return context;
    }

    @Override
    public FlowContext downscaleCluster(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        Set<String> hostNames = ambariClusterConnector.decommissionAmbariNodes(scalingContext.getStackId(), scalingContext.getHostGroupAdjustment(),
                scalingContext.getDecommissionCandidates());
        if (!hostNames.isEmpty()) {
            updateInstanceMetadataAfterScaling(true, hostNames, stack);
        }
        return context;
    }

    @Override
    public FlowContext handleScalingFailure(FlowContext context) throws CloudbreakException {
        ClusterScalingContext scalingContext = (ClusterScalingContext) context;
        Stack stack = stackService.getById(scalingContext.getStackId());
        Cluster cluster = stack.getCluster();
        cluster.setStatus(Status.UPDATE_FAILED);
        cluster.setStatusReason(scalingContext.getErrorReason());
        stackUpdater.updateStackCluster(stack.getId(), cluster);
        Integer scalingAdjustment = scalingContext.getHostGroupAdjustment().getScalingAdjustment();
        String statusMessage = scalingAdjustment > 0 ? "new node(s) could not be added." : "node(s) could not be removed.";
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Failed to update cluster because " + statusMessage);
        return context;
    }

    private void changeAmbariCredentials(String ambariIp, Stack stack) {
        String userName = stack.getUserName();
        String password = stack.getPassword();
        AmbariClient ambariClient = clientService.createDefault(ambariIp);
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
        String cause = decommission ? "Down" : "Up";
        String statusReason = String.format("%sscale of cluster finished successfully. AMBARI_IP:%s", cause, stack.getAmbariIp());
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
    }
}
