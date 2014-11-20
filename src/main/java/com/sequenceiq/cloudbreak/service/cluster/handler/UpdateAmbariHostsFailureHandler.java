package com.sequenceiq.cloudbreak.service.cluster.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsFailure;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class UpdateAmbariHostsFailureHandler implements Consumer<Event<UpdateAmbariHostsFailure>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsFailureHandler.class);

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public void accept(Event<UpdateAmbariHostsFailure> event) {
        UpdateAmbariHostsFailure failure = event.getData();
        Cluster cluster = clusterRepository.findById(failure.getClusterId());
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT);
        cluster.setStatus(Status.UPDATE_FAILED);
        cluster.setStatusReason(failure.getDetailedMessage());
        clusterRepository.save(cluster);
        Stack stack = stackRepository.findStackForCluster(cluster.getId());
        String statusMessage = failure.isAddingNodes() ? "new node(s) could not be added." : "node(s) could not be removed.";
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "Failed to update cluster because " + statusMessage);
    }

}
