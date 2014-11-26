package com.sequenceiq.cloudbreak.service.cluster.handler;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.event.StackStatusUpdateRequest;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterStatusUpdateHandler implements Consumer<Event<ClusterStatusUpdateRequest>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusUpdateHandler.class);

    @Autowired
    private AmbariClusterConnector ambariClusterConnector;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    @Override
    public void accept(Event<ClusterStatusUpdateRequest> event) {
        try {
            handleStatusUpdate(event);
        } catch (Exception ex) {
            LOGGER.error("Status update event could not be handled.", ex);
        }
    }

    private void handleStatusUpdate(Event<ClusterStatusUpdateRequest> event) {
        ClusterStatusUpdateRequest statusUpdateRequest = event.getData();
        StatusRequest statusRequest = statusUpdateRequest.getStatusRequest();
        long stackId = statusUpdateRequest.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDCBuilder.buildMdcContext(cluster);
        if (StatusRequest.STOPPED.equals(statusRequest)) {
            cloudbreakEventService.fireCloudbreakEvent(stackId, Status.STOP_IN_PROGRESS.name(), "Services are stopping.");
            ambariClusterConnector.stopCluster(stack);
            cluster.setStatus(Status.STOPPED);
            cloudbreakEventService.fireCloudbreakEvent(stackId, Status.AVAILABLE.name(), "Services have been stopped successfully.");
            if (Status.STOP_REQUESTED.equals(stackRepository.findOneWithLists(stackId).getStatus())) {
                LOGGER.info("Hadoop services stopped, stopping.");
                reactor.notify(ReactorConfig.STACK_STATUS_UPDATE_EVENT,
                        Event.wrap(new StackStatusUpdateRequest(stack.getTemplate().cloudPlatform(), stackId, statusRequest)));
            }
        } else {
            boolean started = ambariClusterConnector.startCluster(stack);
            if (started) {
                LOGGER.info("Successfully started Hadoop services, setting cluster state to: {}", Status.AVAILABLE);
                cluster.setUpSince(new Date().getTime());
                cluster.setStatus(Status.AVAILABLE);
                cloudbreakEventService.fireCloudbreakEvent(stackId, Status.AVAILABLE.name(), "Cluster started successfully.");
            } else {
                LOGGER.info("Failed to start Hadoop services, setting cluster state to: {}", Status.STOPPED);
                cluster.setStatus(Status.STOPPED);
                stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, "Failed to start cluster, some of the services could not started.");
            }
        }
        clusterRepository.save(cluster);
    }
}