package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsFailure;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class UpdateAmbariHostsFailureHandler implements Consumer<Event<UpdateAmbariHostsFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsFailureHandler.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public void accept(Event<UpdateAmbariHostsFailure> event) {
        UpdateAmbariHostsFailure data = event.getData();
        Cluster cluster = clusterRepository.findById(data.getClusterId());
        CbLoggerFactory.buildMdcContext(cluster);
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT);
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason(data.getDetailedMessage());
        clusterRepository.save(cluster);
        Stack stack = stackRepository.findStackForCluster(cluster.getId());
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE);
        websocketService.sendToTopicUser(cluster.getOwner(), WebsocketEndPoint.CLUSTER, new StatusMessage(data.getClusterId(), cluster.getName(),
                "UPDATE_FAILED"));
    }

}
