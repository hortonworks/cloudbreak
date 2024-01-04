package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class FinalizeClusterInstallHandlerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FinalizeClusterInstallHandlerService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void finalizeClusterInstall(List<InstanceMetadataView> instances, ClusterView cluster) {
        LOGGER.info("Cluster created successfully. Cluster name: {}", cluster.getName());
        List<Long> metadataIds = instances.stream().map(InstanceMetadataView::getId).collect(Collectors.toList());
        instanceMetaDataService.updateAllInstancesToStatus(metadataIds, InstanceStatus.SERVICES_HEALTHY, "Cluster install finalized, services are healthy");
        clusterService.updateCreationFinishedAndUpSinceToNowByClusterId(cluster.getId());
    }
}
