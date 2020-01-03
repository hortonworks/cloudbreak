package com.sequenceiq.cloudbreak.service.blueprint;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComponentLocatorService {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        return getComponentAttribute(cluster, componentNames, InstanceMetaData::getDiscoveryFQDN);
    }

    public Map<String, List<String>> getImpalaCoordinatorLocations(Cluster cluster) {
        Map<String, List<String>> result = new HashMap<>();
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);

        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = new HashSet<>(processor.getImpalaCoordinatorsInHostGroup(hg.getName()));
            fillList(InstanceMetaData::getDiscoveryFQDN, result, hg, hgComponents);
        }
        return result;
    }

    public Map<String, List<String>> getComponentLocationByHostname(Cluster cluster, Collection<String> componentNames) {
        return getComponentAttribute(cluster, componentNames, InstanceMetaData::getDiscoveryFQDN);
    }

    public Map<String, List<String>> getComponentLocation(Long clusterId, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getComponentAttribute(clusterId, blueprintTextProcessor, componentNames, InstanceMetaData::getDiscoveryFQDN);
    }

    private Map<String, List<String>> getComponentAttribute(Cluster cluster, Collection<String> componentNames, Function<InstanceMetaData, String> fqdn) {
        Map<String, List<String>> result = new HashMap<>();
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = new HashSet<>(processor.getComponentsInHostGroup(hg.getName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdn, result, hg, hgComponents);
        }
        return result;
    }

    private Map<String, List<String>> getComponentAttribute(Long clusterId, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames, Function<InstanceMetaData, String> fqdn) {
        Map<String, List<String>> result = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(clusterId)) {
            Set<String> hgComponents = new HashSet<>(blueprintTextProcessor.getComponentsInHostGroup(hg.getName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdn, result, hg, hgComponents);
        }
        return result;
    }

    private void fillList(Function<InstanceMetaData, String> fqdn, Map<String, List<String>> result, HostGroup hg, Set<String> hgComponents) {
        List<String> attributeList = hg.getInstanceGroup().getNotDeletedInstanceMetaDataSet().stream()
                .map(fqdn).collect(Collectors.toList());
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
