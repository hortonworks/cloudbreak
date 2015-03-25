package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClientPollerObject;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHealthCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHosts;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHostsJoinStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.ConsulPluginEvent;
import com.sequenceiq.cloudbreak.service.cluster.flow.PluginManager;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;

@Service
public class StackStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private PollingService<AmbariHosts> ambariHostJoin;

    @Autowired
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private AmbariHealthCheckerTask ambariHealthCheckerTask;

    @Autowired
    private AmbariHostsJoinStatusCheckerTask ambariHostsJoinStatusCheckerTask;

    @Autowired
    private AmbariClientProvider ambariClientProvider;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    public FlowContext start(FlowContext context) throws CloudbreakException {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;

        long stackId = stackStatusUpdateContext.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        CloudPlatform cloudPlatform = stack.cloudPlatform();
        MDCBuilder.buildMdcContext(stack);

        boolean started;
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
        started = connector.startAll(stack);
        if (started) {
            final Stack updatedStack = stackRepository.findOneWithLists(stack.getId());
            PollingResult pollingResult = waitForAmbariToStart(updatedStack);
            if (isSuccess(pollingResult)) {
                handlePollingResultForStart(stackStatusUpdateContext, updatedStack, pollingResult);
            }
        } else {
            throw new CloudbreakException("Unfortunately the cluster infrastructure could not start.");
        }

        return stackStatusUpdateContext;
    }

    public FlowContext handleStackStartFailure(FlowContext context) {
        StackStatusUpdateContext stackStatusUpdateContext = (StackStatusUpdateContext) context;
        if (stackStatusUpdateContext.isPollingError()) {
            Stack stack = stackRepository.findOneWithLists(stackStatusUpdateContext.getStackId());
            Cluster cluster = stack.getCluster();
            cluster.setStatus(Status.START_FAILED);
            clusterRepository.save(cluster);
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), Status.AVAILABLE, stackStatusUpdateContext.getErrorReason());
        } else {
            LOGGER.info("Update stack state to: {}", Status.START_FAILED);
            stackUpdater.updateStackStatus(stackStatusUpdateContext.getStackId(), Status.START_FAILED, stackStatusUpdateContext.getErrorReason());
        }
        return context;
    }

    private void handlePollingResultForStart(StackStatusUpdateContext stackStatusUpdateContext, Stack updatedStack, PollingResult pollingResult)
            throws CloudbreakException {
        Cluster cluster = clusterRepository.findOneWithLists(updatedStack.getCluster().getId());
        pluginManager.triggerAndWaitForPlugins(updatedStack, ConsulPluginEvent.START_AMBARI_EVENT);
        PollingResult hostsJoined = restartAmbariAgentsIfNeeded(updatedStack, waitForHostsToJoin(updatedStack));
        if (isSuccess(pollingResult)) {
            String statusReason = "Cluster infrastructure is available, starting of services has been requested. AMBARI_IP:" + updatedStack.getAmbariIp();
            LOGGER.info("Update stack state to: {}", Status.AVAILABLE);
            stackUpdater.updateStackStatus(updatedStack.getId(), Status.AVAILABLE, statusReason);
        }
        if (!(isSuccess(pollingResult) && !isExited(hostsJoined))
                && cluster != null
                && Status.START_REQUESTED.equals(cluster.getStatus())) {
            stackStatusUpdateContext.setPollingError(true);
            throw new CloudbreakException("Services could not start because host(s) could not join.");
        }
    }

    private PollingResult waitForAmbariToStart(Stack stack) {
        return ambariHealthChecker.pollWithTimeout(
                ambariHealthCheckerTask,
                new AmbariClientPollerObject(stack, ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(), stack.getPassword())),
                AmbariClusterConnector.POLLING_INTERVAL,
                AmbariClusterConnector.MAX_ATTEMPTS_FOR_HOSTS);
    }

    private PollingResult waitForHostsToJoin(Stack stack) {
        AmbariHosts ambariHosts = new AmbariHosts(stack,  ambariClientProvider.getAmbariClient(stack.getAmbariIp(), stack.getUserName(),
                stack.getPassword()), stack.getFullNodeCount() - stack.getGateWayNodeCount());
        try {
            return ambariHostJoin.pollWithTimeout(
                    ambariHostsJoinStatusCheckerTask,
                    ambariHosts,
                    AmbariClusterConnector.POLLING_INTERVAL,
                    AmbariClusterConnector.MAX_ATTEMPTS_FOR_HOSTS);
        } catch (AmbariHostsUnavailableException ex) {
            LOGGER.error(ex.getMessage());
            return PollingResult.EXIT;
        }
    }

    private PollingResult restartAmbariAgentsIfNeeded(Stack stack, PollingResult started) {
        if (!isSuccess(started)) {
            LOGGER.info("Ambari agents couldn't join. Restart ambari agents...");
            pluginManager.triggerAndWaitForPlugins(stack, ConsulPluginEvent.RESTART_AMBARI_EVENT);
            return waitForHostsToJoin(stack);
        }
        return PollingResult.SUCCESS;
    }
}
