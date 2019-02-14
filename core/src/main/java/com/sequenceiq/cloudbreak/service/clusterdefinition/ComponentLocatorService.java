package com.sequenceiq.cloudbreak.service.clusterdefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Service
public class ComponentLocatorService {

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            String clusterDefinitionText = cluster.getClusterDefinition().getClusterDefinitionText();
            Set<String> hgComponents = ambariBlueprintProcessorFactory.get(clusterDefinitionText).getComponentsInHostGroup(hg.getName());
            hgComponents.retainAll(componentNames);

            List<String> fqdn = hg.getConstraint().getInstanceGroup().getNotDeletedInstanceMetaDataSet().stream()
                    .map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());
            for (String service : hgComponents) {
                List<String> storedAddresses = result.get(service);
                if (storedAddresses == null) {
                    result.put(service, fqdn);
                } else {
                    storedAddresses.addAll(fqdn);
                }
            }
        }
        return result;
    }
}
