package com.sequenceiq.cloudbreak.service.cluster.handler;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsSuccess;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class UpdateAmbariHostsSuccessHandler implements Consumer<Event<UpdateAmbariHostsSuccess>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAmbariHostsSuccessHandler.class);

    @Autowired
    private WebsocketService websocketService;

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
        MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.getOwner());
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cluster.getId().toString());
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CLUSTER_ID.toString());
        LOGGER.info("Accepted {} event.", ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT);
        Stack stack = stackRepository.findStackWithListsForCluster(data.getClusterId());
        for (String hostName : hostNames) {
            InstanceMetaData metadataEntry = metadataRepository.findHostInStack(stack.getId(), hostName);
            if (data.isDecommision()) {
                metadataEntry.setRemovable(true);
            } else {
                metadataEntry.setRemovable(false);
            }
            metadataRepository.save(metadataEntry);
        }
        stackUpdater.updateStackStatus(stack.getId(), Status.AVAILABLE, "");
        websocketService.sendToTopicUser(cluster.getOwner(), WebsocketEndPoint.CLUSTER,
                new StatusMessage(data.getClusterId(), cluster.getName(), Status.AVAILABLE.name()));
    }

}
