package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class FinalizeClusterInstallHandlerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizeClusterInstallHandlerService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void finalizeClusterInstall(Set<InstanceMetaData> instances, Cluster cluster) {
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.getName());
        for (InstanceMetaData instance : instances) {
            instance.setInstanceStatus(InstanceStatus.SERVICES_HEALTHY);
        }
        instanceMetaDataService.saveAll(instances);
        Long now = new Date().getTime();
        cluster.setCreationFinished(now);
        cluster.setUpSince(now);
        clusterService.updateCluster(cluster);
    }
}
