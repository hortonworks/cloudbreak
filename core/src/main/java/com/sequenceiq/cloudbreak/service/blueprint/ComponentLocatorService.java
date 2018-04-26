package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;

@Service
public class ComponentLocatorService {

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = blueprintProcessorFactory.get(cluster.getBlueprint().getBlueprintText()).getComponentsInHostGroup(hg.getName());
            hgComponents.retainAll(componentNames);

            List<String> fqdn = instanceMetadataRepository.findInstancesInInstanceGroupWithoutStatuses(hg.getConstraint().getInstanceGroup().getId(),
                    Arrays.asList(InstanceStatus.TERMINATED, InstanceStatus.DELETED_ON_PROVIDER_SIDE))
                    .stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toList());

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
