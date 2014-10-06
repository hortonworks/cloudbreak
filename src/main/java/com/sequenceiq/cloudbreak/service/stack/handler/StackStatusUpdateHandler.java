package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHealthCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHosts;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHostsJoinStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackStatusUpdateHandler implements Consumer<Event<StackStatusUpdateRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusUpdateHandler.class);

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private PollingService<AmbariHosts> ambariHostJoin;

    @Autowired
    private PollingService<AmbariClient> ambariHealthChecker;

    @Override
    public void accept(Event<StackStatusUpdateRequest> event) {
        StackStatusUpdateRequest statusUpdateRequest = event.getData();
        CloudPlatform cloudPlatform = statusUpdateRequest.getCloudPlatform();
        CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
        StatusRequest statusRequest = statusUpdateRequest.getStatusRequest();
        long stackId = statusUpdateRequest.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        User user = statusUpdateRequest.getUser();
        if (StatusRequest.STOPPED.equals(statusRequest)) {
            boolean stopped = connector.stopAll(user, stack);
            if (stopped) {
                LOGGER.info("Update stack {} state to: {}", stackId, Status.STOPPED);
                stackUpdater.updateStackStatus(stackId, Status.STOPPED);
            } else {
                LOGGER.info("Update stack {} state to: {}", stackId, Status.STOP_FAILED);
                stackUpdater.updateStackStatus(stackId, Status.STOP_FAILED);
            }
        } else {
            boolean started = connector.startAll(user, stack);
            if (started) {
                waitForAmbariToStart(stack);
                Cluster cluster = clusterRepository.findOneWithLists(stack.getCluster().getId());
                LOGGER.info("Update stack {} state to: {}", stackId, Status.AVAILABLE);
                stackUpdater.updateStackStatus(stackId, Status.AVAILABLE);
                if (cluster != null && Status.START_REQUESTED.equals(cluster.getStatus())) {
                    waitForHostsToJoin(stack);
                    reactor.notify(ReactorConfig.CLUSTER_STATUS_UPDATE_EVENT,
                            Event.wrap(new ClusterStatusUpdateRequest(user, stack.getId(), statusRequest)));
                }
            } else {
                LOGGER.info("Update stack {} state to: {}", stackId, Status.START_FAILED);
                stackUpdater.updateStackStatus(stackId, Status.START_FAILED);
            }
        }
    }

    private void waitForAmbariToStart(Stack stack) {
        ambariHealthChecker.pollWithTimeout(
                new AmbariHealthCheckerTask(),
                new AmbariClient(stack.getAmbariIp()),
                AmbariClusterConnector.POLLING_INTERVAL,
                AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
    }

    private void waitForHostsToJoin(Stack stack) {
        ambariHostJoin.pollWithTimeout(
                new AmbariHostsJoinStatusCheckerTask(),
                new AmbariHosts(stack.getId(), new AmbariClient(stack.getAmbariIp()), stack.getNodeCount() * stack.getMultiplier()),
                AmbariClusterConnector.POLLING_INTERVAL,
                AmbariClusterConnector.MAX_ATTEMPTS_FOR_AMBARI_OPS);
    }
}