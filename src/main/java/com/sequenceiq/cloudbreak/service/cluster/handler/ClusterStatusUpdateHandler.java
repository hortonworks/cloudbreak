package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
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

    @Override
    public void accept(Event<ClusterStatusUpdateRequest> event) {
        ClusterStatusUpdateRequest statusUpdateRequest = event.getData();
        StatusRequest statusRequest = statusUpdateRequest.getStatusRequest();
        long stackId = statusUpdateRequest.getStackId();
        Stack stack = stackRepository.findOneWithLists(stackId);
        Cluster cluster = stack.getCluster();
        MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cluster.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CLUSTER_ID.toString());
        if (StatusRequest.STOPPED.equals(statusRequest)) {
            ambariClusterConnector.stopCluster(stack);
            cluster.setStatus(Status.STOPPED);
            if (Status.STOP_REQUESTED.equals(stackRepository.findOneWithLists(stackId).getStatus())) {
                LOGGER.info("Hadoop services stopped, stopping stack: {}", stackId);
                reactor.notify(ReactorConfig.STACK_STATUS_UPDATE_EVENT,
                        Event.wrap(new StackStatusUpdateRequest(stack.getTemplate().cloudPlatform(), stackId, statusRequest)));
            }
        } else {
            boolean started = ambariClusterConnector.startCluster(stack);
            if (started) {
                LOGGER.info("Successfully started Hadoop services on stack: {}, setting cluster state to: {}", stackId, Status.AVAILABLE);
                cluster.setStatus(Status.AVAILABLE);
            } else {
                LOGGER.info("Failed to start Hadoop services on stack: {}, setting cluster state to: {}", stackId, Status.STOPPED);
                cluster.setStatus(Status.STOPPED);
            }
        }
        clusterRepository.save(cluster);
    }
}