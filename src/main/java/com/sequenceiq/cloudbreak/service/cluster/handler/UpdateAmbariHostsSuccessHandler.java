package com.sequenceiq.cloudbreak.service.cluster.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsSuccess;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class UpdateAmbariHostsSuccessHandler implements Consumer<Event<UpdateAmbariHostsSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsSuccessHandler.class);

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private InstanceMetaDataRepository metadataRepository;

    @Override
    public void accept(Event<UpdateAmbariHostsSuccess> event) {
        UpdateAmbariHostsSuccess data = event.getData();
        Cluster cluster = clusterRepository.findById(data.getClusterId());
        Set<String> hostNames = data.getHostNames();
        MDCBuilder.buildMdcContext(cluster);
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT);
        Stack stack = stackRepository.findStackWithListsForCluster(data.getClusterId());
        updateResourcesStatus(data.isDecommission(), hostNames, stack);
    }

    private void updateResourcesStatus(boolean decommission, Set<String> hostNames, Stack stack) {
        for (String hostName : hostNames) {
            InstanceMetaData metadataEntry = metadataRepository.findHostInStack(stack.getId(), hostName);
            if (decommission) {
                metadataEntry.setInstanceStatus(InstanceStatus.DECOMMISSIONED);
            } else {
                metadataEntry.setInstanceStatus(InstanceStatus.REGISTERED);
            }
            metadataRepository.save(metadataEntry);
        }
        String cause = decommission ? "Down" : "Up";
        String statusReason =  String.format("%sscale of cluster finished successfully. AMBARI_IP:%s", cause, stack.getAmbariIp());
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, statusReason);
    }

}
