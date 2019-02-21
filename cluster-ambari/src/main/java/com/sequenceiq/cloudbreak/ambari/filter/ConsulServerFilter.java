package com.sequenceiq.cloudbreak.ambari.filter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class ConsulServerFilter implements HostFilter {

    @Override
    public List<HostMetadata> filter(long clusterId, Map<String, String> config, List<HostMetadata> hosts, Set<InstanceMetaData> instanceMetaDatasInStack) {
        Map<String, InstanceMetaData> instanceMetaDataMap =
                instanceMetaDatasInStack.stream().collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, im -> im));
        return hosts.stream().filter(host -> !instanceMetaDataMap.containsKey(host.getHostName())
                || !instanceMetaDataMap.get(host.getHostName()).getConsulServer()).collect(Collectors.toList());
    }
}
