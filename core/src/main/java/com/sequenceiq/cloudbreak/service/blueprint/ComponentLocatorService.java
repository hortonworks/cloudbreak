package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;

@Service
public class ComponentLocatorService {

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        return getComponentAttribute(cluster, componentNames, InstanceMetaData::getDiscoveryFQDN);
    }

    public Map<String, List<String>> getComponentPrivateIp(Long clusterId, AmbariBlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getComponentAttribute(clusterId, blueprintTextProcessor, componentNames, InstanceMetaData::getPrivateIp);
    }

    private Map<String, List<String>> getComponentAttribute(Cluster cluster, Collection<String> componentNames,
            Function<InstanceMetaData, String> getterFunction) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            Set<String> hgComponents = ambariBlueprintProcessorFactory.get(blueprintText).getComponentsInHostGroup(hg.getName());
            hgComponents.retainAll(componentNames);
            fillList(getterFunction, result, hg, hgComponents);
        }
        return result;
    }

    private Map<String, List<String>> getComponentAttribute(Long clusterId, AmbariBlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames, Function<InstanceMetaData, String> getterFunction) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(clusterId)) {
            Set<String> hgComponents = blueprintTextProcessor.getComponentsInHostGroup(hg.getName());
            hgComponents.retainAll(componentNames);
            fillList(getterFunction, result, hg, hgComponents);
        }
        return result;
    }

    private void fillList(Function<InstanceMetaData, String> getterFunction, Map<String, List<String>> result, HostGroup hg, Set<String> hgComponents) {
        List<String> attributeList = hg.getConstraint().getInstanceGroup().getNotDeletedInstanceMetaDataSet().stream()
                .map(getterFunction).collect(Collectors.toList());
        for (String service : hgComponents) {
            List<String> storedAttributes = result.get(service);
            if (storedAttributes == null) {
                result.put(service, attributeList);
            } else {
                storedAttributes.addAll(attributeList);
            }
        }
    }
}
