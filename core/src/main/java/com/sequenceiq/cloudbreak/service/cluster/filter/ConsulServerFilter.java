package com.sequenceiq.cloudbreak.service.cluster.filter;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConsulServerFilter implements HostFilter {

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Override
    public List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts) {
        List<HostMetadata> copy = new ArrayList<>(hosts);
        Cluster cluster = clusterRepository.findById(clusterId);
        for (HostMetadata host : hosts) {
            InstanceMetaData instanceMetaData = instanceMetadataRepository.findHostInStack(cluster.getStack().getId(), host.getHostName());
            if (instanceMetaData != null && instanceMetaData.getConsulServer()) {
                copy.remove(host);
            }
        }
        return copy;
    }

}
