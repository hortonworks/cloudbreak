package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import freemarker.template.Configuration;

@Service
public class ClusterCreationSuccessHandler implements Consumer<Event<ClusterCreationSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSuccessHandler.class);

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Value("${cb.smtp.sender.from}")
    private String msgFrom;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Autowired
    private AmbariClusterInstallerMailSenderService ambariClusterInstallerMailSenderService;

    @Override
    public void accept(Event<ClusterCreationSuccess> event) {
        ClusterCreationSuccess clusterCreationSuccess = event.getData();
        Long clusterId = clusterCreationSuccess.getClusterId();
        LOGGER.info("Accepted {} event.", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, clusterId);
        Cluster cluster = clusterRepository.findById(clusterId);
        cluster.setStatus(Status.AVAILABLE);
        cluster.setCreationFinished(clusterCreationSuccess.getCreationFinished());
        clusterRepository.save(cluster);
        Stack stack = stackRepository.findStackForCluster(clusterId);
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE);
        if (cluster.getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendSuccessEmail(cluster.getUser(), event.getData().getAmbariIp());
        }
        websocketService.sendToTopicUser(cluster.getUser().getEmail(), WebsocketEndPoint.CLUSTER,
                new StatusMessage(clusterId, cluster.getName(), Status.AVAILABLE.name()));
    }

}
