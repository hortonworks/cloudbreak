package com.sequenceiq.cloudbreak.service.blueprint;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessorFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComponentLocatorService {

    @Inject
    private TemplateProcessorFactory blueprintProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = blueprintProcessorFactory.get(cluster.getBlueprint().getBlueprintText()).getComponentsInHostGroup(hg.getName());
            hgComponents.retainAll(componentNames);

            List<String> fqdn = hg.getConstraint().getInstanceGroup().getInstanceMetaData().stream()
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
