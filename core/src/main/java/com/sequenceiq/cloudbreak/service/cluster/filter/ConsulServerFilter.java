package com.sequenceiq.cloudbreak.service.cluster.filter;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class ConsulServerFilter implements HostFilter {

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts) {
        List<HostMetadata> copy = new ArrayList<>(hosts);
        Cluster cluster = getCluster(clusterId);
        for (HostMetadata host : hosts) {
            Optional<InstanceMetaData> instanceMetaData = instanceMetaDataService.findHostInStack(cluster.getStack().getId(), host.getHostName());
            if (instanceMetaData.isPresent() && instanceMetaData.get().getConsulServer()) {
                copy.remove(host);
            }
        }
        return copy;
    }

    private Cluster getCluster(long clusterId) {
        return clusterService.findById(clusterId).orElseThrow(notFound("Cluster", clusterId));
    }
}
