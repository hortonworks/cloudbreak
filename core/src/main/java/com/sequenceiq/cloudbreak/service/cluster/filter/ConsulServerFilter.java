package com.sequenceiq.cloudbreak.service.cluster.filter;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class ConsulServerFilter implements HostFilter {

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Override
    public List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts) {
        List<HostMetadata> copy = new ArrayList<>(hosts);
        Cluster cluster = getCluster(clusterId);
        for (HostMetadata host : hosts) {
            InstanceMetaData instanceMetaData = instanceMetadataRepository.findHostInStack(cluster.getStack().getId(), host.getHostName());
            if (instanceMetaData != null && instanceMetaData.getConsulServer()) {
                copy.remove(host);
            }
        }
        return copy;
    }

    private Cluster getCluster(long clusterId) {
        return clusterService.findById(clusterId).orElseThrow(notFound("Cluster", clusterId));
    }
}
