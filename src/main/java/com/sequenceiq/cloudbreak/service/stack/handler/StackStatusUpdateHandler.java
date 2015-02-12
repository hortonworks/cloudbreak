package com.sequenceiq.cloudbreak.service.stack.handler;

import static com.sequenceiq.cloudbreak.service.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.service.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariHostsUnavailableException;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClientPollerObject;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHealthCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHosts;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariHostsJoinStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.cluster.flow.ConsulPluginEvent;
import com.sequenceiq.cloudbreak.service.cluster.flow.PluginManager;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackStatusUpdateHandler implements Consumer<Event<StackStatusUpdateRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusUpdateHandler.class);

    @javax.annotation.Resource
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
    private PollingService<AmbariClientPollerObject> ambariHealthChecker;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Autowired
    private AmbariClientService clientService;

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private AmbariHealthCheckerTask ambariHealthCheckerTask;

    @Autowired
    private AmbariHostsJoinStatusCheckerTask ambariHostsJoinStatusCheckerTask;

    @Override
    public void accept(Event<StackStatusUpdateRequest> event) {
        StackStatusUpdateRequest statusUpdateRequest = event.getData();
        final CloudPlatform cloudPlatform = statusUpdateRequest.getCloudPlatform();

        StatusRequest statusRequest = statusUpdateRequest.getStatusRequest();
        long stackId = statusUpdateRequest.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        if (StatusRequest.STOPPED.equals(statusRequest)) {
            cloudbreakEventService.fireCloudbreakEvent(stackId, Status.STOP_IN_PROGRESS.name(), "Cluster infrastructure is stopping.");
            triggerConsulEvent(stack, ConsulPluginEvent.STOP_AMBARI_EVENT);
            boolean stopped;
            if (cloudPlatform.isWithTemplate()) {
                CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
                stopped = connector.stopAll(stack);
            } else {
                stopped = startStopResources(cloudPlatform, stack, false);
            }
            if (stopped) {
                LOGGER.info("Update stack state to: {}", Status.STOPPED);
                stackUpdater.updateStackStatus(stackId, Status.STOPPED, "Cluster infrastructure stopped successfully.");
                cloudbreakEventService.fireCloudbreakEvent(stackId, BillingStatus.BILLING_STOPPED.name(), "Cluster infrastructure stopped.");
            } else {
                LOGGER.info("Update stack state to: {}", Status.STOP_FAILED);
                stackUpdater.updateStackStatus(stackId, Status.STOP_FAILED, "Unfortunately the cluster infrastructure could not be stopped.");
            }
        } else {
            boolean started;
            if (cloudPlatform.isWithTemplate()) {
                CloudPlatformConnector connector = cloudPlatformConnectors.get(cloudPlatform);
                started = connector.startAll(stack);
            } else {
                started = startStopResources(cloudPlatform, stack, true);
            }
            if (started) {
                final Stack updatedStack = stackRepository.findOneWithLists(stack.getId());
                PollingResult pollingResult = waitForAmbariToStart(updatedStack);
                if (isSuccess(pollingResult)) {
                    Cluster cluster = clusterRepository.findOneWithLists(updatedStack.getCluster().getId());
                    triggerConsulEvent(updatedStack, ConsulPluginEvent.START_AMBARI_EVENT);
                    PollingResult hostsJoined = restartAmbariAgentsIfNeeded(updatedStack, waitForHostsToJoin(updatedStack));
                    if (isSuccess(pollingResult)) {
                        String statusReason = "Cluster infrastructure is available, starting of services has been requested. AMBARI_IP:" + stack.getAmbariIp();
                        LOGGER.info("Update stack state to: {}", Status.AVAILABLE);
                        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, statusReason);
                    }
                    if (isSuccess(pollingResult) && cluster != null && Status.START_REQUESTED.equals(cluster.getStatus())) {
                        cloudbreakEventService.fireCloudbreakEvent(stackId, Status.START_IN_PROGRESS.name(), "Services are starting.");
                        reactor.notify(ReactorConfig.CLUSTER_STATUS_UPDATE_EVENT,
                                Event.wrap(new ClusterStatusUpdateRequest(updatedStack.getId(), statusRequest)));
                    } else if (!(isSuccess(pollingResult) && !isExited(hostsJoined))
                            && cluster != null
                            && Status.START_REQUESTED.equals(cluster.getStatus())) {
                        cluster.setStatus(Status.START_FAILED);
                        clusterRepository.save(cluster);
                        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, "Services could not start because host(s) could not join.");
                    }
                }
            } else {
                LOGGER.info("Update stack state to: {}", Status.START_FAILED);
                stackUpdater.updateStackStatus(stackId, Status.START_FAILED, "Unfortunately the cluster infrastructure could not start.");
            }
        }
    }

    private boolean startStopResources(CloudPlatform cloudPlatform, Stack stack, final boolean start) {
        boolean finished = true;
        try {
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
            final StartStopContextObject sSCO = resourceBuilderInit.startStopInit(stack);

            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                for (Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                    if (start) {
                        resourceBuilder.start(sSCO, resource, stack.getRegion());
                    } else {
                        resourceBuilder.stop(sSCO, resource, stack.getRegion());
                    }
                }
            }
            List<Future<Boolean>> futures = new ArrayList<>();
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                for (final Resource resource : resourceByType) {
                    final Stack finalStack = stack;
                    Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            if (start) {
                                return resourceBuilder.start(sSCO, resource, finalStack.getRegion());
                            } else {
                                return resourceBuilder.stop(sSCO, resource, finalStack.getRegion());
                            }
                        }
                    });
                    futures.add(submit);
                }
            }
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    finished = false;
                }
            }
        } catch (Exception ex) {
            finished = false;
        }
        return finished;
    }

    private PollingResult waitForAmbariToStart(Stack stack) {
        return ambariHealthChecker.pollWithTimeout(
                ambariHealthCheckerTask,
                new AmbariClientPollerObject(stack, clientService.create(stack)),
                AmbariClusterConnector.POLLING_INTERVAL,
                AmbariClusterConnector.MAX_ATTEMPTS_FOR_HOSTS);
    }

    private PollingResult waitForHostsToJoin(Stack stack) {
        AmbariHosts ambariHosts = new AmbariHosts(stack, clientService.create(stack), stack.getFullNodeCount());
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
            triggerConsulEvent(stack, ConsulPluginEvent.RESTART_AMBARI_EVENT);
            return waitForHostsToJoin(stack);
        }
        return PollingResult.SUCCESS;
    }

    private void triggerConsulEvent(Stack stack, ConsulPluginEvent event) {
        Set<String> eventIds = pluginManager.triggerPlugins(stack.getAllInstanceMetaData(), event);
        pluginManager.waitForEventFinish(stack, stack.getAllInstanceMetaData(), eventIds);
    }
}