package com.sequenceiq.cloudbreak.core.flow.service;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import com.sequenceiq.cloudbreak.core.flow.ClusterStartService;
import com.sequenceiq.cloudbreak.core.flow.ClusterStopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowContextFactory;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.event.AmbariRoleAllocationComplete;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariRoleAllocator;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupListenerTask;
import com.sequenceiq.cloudbreak.service.stack.flow.AmbariStartupPollerObject;
import org.springframework.stereotype.Service;

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
    private ClusterStartService clusterStartService;

    @Autowired
    private ClusterStopService clusterStopService;

    @Autowired
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

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
            context = clusterStartService.start(context);
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
            context = clusterStopService.stop(context);
            LOGGER.debug("Stopping cluster is DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during the cluster stop process: {}", e.getMessage());
            throw new CloudbreakException(e.getMessage(), e);
        }
    }

    @Override
    public FlowContext clusterStartError(FlowContext context) throws CloudbreakException {
        return clusterStartService.handleClusterStartError(context);
    }

    @Override
    public FlowContext clusterStopError(FlowContext context) throws CloudbreakException {
        return clusterStopService.handleClusterStopError(context);
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
}
