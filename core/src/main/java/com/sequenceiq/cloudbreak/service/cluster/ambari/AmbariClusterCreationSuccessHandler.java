package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Service
public class AmbariClusterCreationSuccessHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterCreationSuccessHandler.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public void handleClusterCreationSuccess(Set<InstanceMetaData> instances, Cluster cluster) {
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.getName());
        for (InstanceMetaData instance : instances) {
            instance.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
            instanceMetadataRepository.save(instance);
        }
        Long now = new Date().getTime();
        cluster.setCreationFinished(now);
        cluster.setUpSince(now);
        clusterService.updateCluster(cluster);
    }
}
