package com.sequenceiq.cloudbreak.service.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class ClusterCreationSuccessHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSuccessHandler.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostMetadataService hostMetadataService;

    public void handleClusterCreationSuccess(Stack stack) {
        Cluster cluster = clusterService.findOneByStackId(stack.getId())
                .orElseThrow(NotFoundException.notFound("cluster", stack.getId()));
        LOGGER.debug("Cluster created successfully. Cluster name: {}", cluster.getName());
        Long now = new Date().getTime();
        cluster.setCreationFinished(now);
        cluster.setUpSince(now);
        cluster = clusterService.updateCluster(cluster);
        Collection<InstanceMetaData> updatedInstances = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Set<InstanceMetaData> instances = instanceGroup.getAllInstanceMetaData();
            for (InstanceMetaData instanceMetaData : instances) {
                if (!instanceMetaData.isTerminated()) {
                    instanceMetaData.setInstanceStatus(InstanceStatus.REGISTERED);
                    updatedInstances.add(instanceMetaData);
                }
            }
        }
        instanceMetaDataService.saveAll(updatedInstances);
        Collection<HostMetadata> hostMetadataList = new ArrayList<>();
        for (HostMetadata host : hostMetadataService.findHostsInCluster(cluster.getId())) {
            host.setHostMetadataState(HostMetadataState.HEALTHY);
            hostMetadataList.add(host);
        }
        hostMetadataService.saveAll(hostMetadataList);
    }
}
