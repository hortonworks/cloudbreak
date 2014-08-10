package com.sequenceiq.cloudbreak.service.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterCreationSuccessHandler implements Consumer<Event<ClusterCreationSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSuccessHandler.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Override
    public void accept(Event<ClusterCreationSuccess> event) {
        ClusterCreationSuccess clusterCreationSuccess = event.getData();
        Long clusterId = clusterCreationSuccess.getClusterId();
        LOGGER.info("Accepted {} event.", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, clusterId);
        Cluster cluster = clusterRepository.findById(clusterId);
        cluster.setStatus(Status.CREATE_COMPLETED);
        cluster.setCreationFinished(clusterCreationSuccess.getCreationFinished());
        clusterRepository.save(cluster);
        websocketService.sendToTopicUser(cluster.getUser().getEmail(), WebsocketEndPoint.CLUSTER,
                new StatusMessage(clusterId, cluster.getName(), Status.CREATE_COMPLETED.name()));
    }
}
