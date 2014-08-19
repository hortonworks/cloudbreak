package com.sequenceiq.cloudbreak.service.stack.handler.node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.stack.event.AddNodeSuccess;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class AddNodeAmbariUpdateFailedHandler implements Consumer<Event<AddNodeSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeAmbariUpdateFailedHandler.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Override
    public void accept(Event<AddNodeSuccess> event) {
        AddNodeSuccess data = event.getData();
        Cluster cluster = clusterRepository.findById(data.getClusterId());
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_NODE_AMBARI_UPDATE_NODE_FAILED_EVENT);
        websocketService.sendToTopicUser(cluster.getUser().getEmail(), WebsocketEndPoint.CLUSTER,
                new StatusMessage(data.getClusterId(), cluster.getName(), Status.CREATE_FAILED.name()));
    }

}
