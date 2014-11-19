package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationFailure;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterCreationFailureHandler implements Consumer<Event<ClusterCreationFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFailureHandler.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Autowired
    private CloudbreakEventService eventService;

    @Override
    public void accept(Event<ClusterCreationFailure> event) {
        ClusterCreationFailure clusterCreationFailure = event.getData();

        Long clusterId = clusterCreationFailure.getClusterId();
        Cluster cluster = clusterRepository.findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Accepted {} event.", ReactorConfig.CLUSTER_CREATE_FAILED_EVENT, clusterId);
        String detailedMessage = clusterCreationFailure.getDetailedMessage();
        cluster.setStatus(Status.CREATE_FAILED);
        cluster.setStatusReason(detailedMessage);
        clusterRepository.save(cluster);
        Long stackId = clusterCreationFailure.getStackId();
        stackUpdater.updateStackStatus(stackId, Status.AVAILABLE, "Stack update finished - Cluster create failed.");
        eventService.fireCloudbreakEvent(stackId, "CLUSTER_CREATION_FAILED", detailedMessage);
        if (cluster.getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendFailEmail(cluster.getOwner());
        }
        websocketService.sendToTopicUser(cluster.getOwner(), WebsocketEndPoint.CLUSTER,
                new StatusMessage(clusterId, cluster.getName(), Status.CREATE_FAILED.name(), detailedMessage));
    }

}
