package com.sequenceiq.cloudbreak.service.blueprint;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Service
public class ComponentLocatorService {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private HostGroupService hostGroupService;

    public Map<String, List<String>> getComponentLocation(Cluster cluster, Collection<String> componentNames) {
        return getFqdnsByComponents(cluster, componentNames);
    }

    public Map<String, List<String>> getImpalaCoordinatorLocations(Cluster cluster) {
        Map<String, List<String>> result = new HashMap<>();
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);

        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = new HashSet<>(processor.getImpalaCoordinatorsInHostGroup(hg.getName()));
            fillList(result, hg, hgComponents);
        }
        return result;
    }

    public Map<String, List<String>> getComponentLocationByHostname(Cluster cluster, Collection<String> componentNames) {
        return getFqdnsByComponents(cluster, componentNames);
    }

    public Map<String, List<String>> getComponentLocation(Long clusterId, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getFqdnsByComponents(clusterId, blueprintTextProcessor, componentNames);
    }

    private Map<String, List<String>> getFqdnsByComponents(Cluster cluster, Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByService = new HashMap<>();
        String blueprintText = cluster.getBlueprint().getBlueprintText();
        BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        for (HostGroup hg : hostGroupService.getByCluster(cluster.getId())) {
            Set<String> hgComponents = new HashSet<>(processor.getComponentsInHostGroup(hg.getName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByService, hg, hgComponents);
        }
        return fqdnsByService;
    }

    private Map<String, List<String>> getFqdnsByComponents(Long clusterId, BlueprintTextProcessor blueprintTextProcessor, Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByComponent = new HashMap<>();
        for (HostGroup hg : hostGroupService.getByCluster(clusterId)) {
            Set<String> hgComponents = new HashSet<>(blueprintTextProcessor.getComponentsInHostGroup(hg.getName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByComponent, hg, hgComponents);
        }
        return fqdnsByComponent;
    }

    private void fillList(Map<String, List<String>> fqdnsByComponent, HostGroup hg, Set<String> hgComponents) {
        List<String> fqdnList = hg.getInstanceGroup()
                .getReachableInstanceMetaDataSet()
                .stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(toList());
        for (String component : hgComponents) {
            List<String> storedFqdnsForComponent = fqdnsByComponent.get(component);
            if (storedFqdnsForComponent == null) {
                fqdnsByComponent.put(component, new ArrayList<>(fqdnList));
            } else {
                storedFqdnsForComponent.addAll(fqdnList);
            }
        }
    }
}
