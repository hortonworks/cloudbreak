package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
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

@Service
public class SimpleAmbariFlowFacade implements AmbariFlowFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleAmbariFlowFacade.class);
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
    private PollingService<AmbariStartupPollerObject> ambariStartupPollerObjectPollingService;

    @Override
    public ProvisioningContext allocateAmbariRoles(ProvisioningContext context) throws Exception {
        LOGGER.debug("Allocating Ambari roles. Context: {}", context);
        AmbariRoleAllocationComplete ambariRoleAllocationComplete = ambariRoleAllocator.allocateRoles(context.getStackId(), context.getCoreInstanceMetaData());
        return ProvisioningContextFactory.createAmbariStartContext(ambariRoleAllocationComplete.getStack().getId(), ambariRoleAllocationComplete.getAmbariIp());
    }

    @Override
    public ProvisioningContext startAmbari(ProvisioningContext context) throws Exception {
        ProvisioningContext retContext = null;
        Stack stack = stackService.getById(context.getStackId());
        MDCBuilder.buildMdcContext(stack);

        AmbariStartupPollerObject ambariStartupPollerObject = new AmbariStartupPollerObject(stack, context.getAmbariIp(),
                clientService.createDefault(context.getAmbariIp()));

        PollingResult pollingResult = ambariStartupPollerObjectPollingService.pollWithTimeout(ambariStartupListenerTask, ambariStartupPollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

        if (isSuccess(pollingResult)) {
            LOGGER.info("Stack has been successfully created!");
            assert context.getAmbariIp() != null;
            retContext = context;
        } else {
            throw new CloudbreakException("Stack creation failed. Context:" + context);
        }
        stack = stackUpdater.updateAmbariIp(stack.getId(), context.getAmbariIp());
        String statusReason = "Cluster infrastructure and ambari are available on the cloud. AMBARI_IP:" + stack.getAmbariIp();
        stack = stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
        stackUpdater.updateStackStatusReason(stack.getId(), "");
        changeAmbariCredentials(context.getAmbariIp(), stack);

        return retContext;
    }

    @Override
    public ProvisioningContext buildAmbariCluster(ProvisioningContext context) throws Exception {
        Stack stack = stackService.getById(context.getStackId());
        MDCBuilder.buildMdcContext(stack);

        if (stack.getCluster() != null && stack.getCluster().getStatus().equals(Status.REQUESTED)) {
            ambariClusterConnector.buildAmbariCluster(stack);
        } else {
            LOGGER.info("Ambari has started but there were no cluster request to this stack yet. Won't install cluster now.");
        }

        if (stack.getStatus().equals(Status.AVAILABLE)) {
            ambariClusterConnector.buildAmbariCluster(stack);
        } else {
            LOGGER.info("Cluster install requested but the stack is not completed yet. Installation will start after the stack is ready.");
        }
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
}
