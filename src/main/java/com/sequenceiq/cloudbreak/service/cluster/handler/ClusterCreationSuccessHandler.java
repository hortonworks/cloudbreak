package com.sequenceiq.cloudbreak.service.cluster.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterCreationSuccess;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterInstallerMailSenderService;

import freemarker.template.Configuration;
import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class ClusterCreationSuccessHandler implements Consumer<Event<ClusterCreationSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSuccessHandler.class);

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
        Cluster cluster = clusterRepository.findById(clusterId);
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Accepted {} event.", ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT, clusterId);
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason("");
        cluster.setCreationFinished(clusterCreationSuccess.getCreationFinished());
        cluster.setUpSince(clusterCreationSuccess.getCreationFinished());
        clusterRepository.save(cluster);
        Stack stack = stackRepository.findStackWithListsForCluster(clusterId);
        Set<InstanceMetaData> instances = stack.getInstanceMetaData();
        for (InstanceMetaData instanceMetaData : instances) {
            instanceMetaData.setRemovable(false);
        }
        stackUpdater.updateStackMetaData(stack.getId(), instances);
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Cluster installation successfully finished. AMBARI_IP:" + stack.getAmbariIp());

        if (cluster.getEmailNeeded()) {
            ambariClusterInstallerMailSenderService.sendSuccessEmail(cluster.getOwner(), event.getData().getAmbariIp());
        }
    }

}
