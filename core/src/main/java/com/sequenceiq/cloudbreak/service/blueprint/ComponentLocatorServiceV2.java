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
import com.sequenceiq.cloudbreak.service.stack.InstanceMetadataDto;
import com.sequenceiq.cloudbreak.service.stack.StackProxy;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Service
public class ComponentLocatorServiceV2 {

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public Map<String, List<String>> getComponentLocation(StackProxy stack, Collection<String> componentNames) {
        return getFqdnsByComponents(stack, componentNames);
    }

    public Map<String, List<String>> getImpalaCoordinatorLocations(StackProxy stack) {
        Map<String, List<String>> result = new HashMap<>();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprint().getBlueprintText());

        stack.getInstanceGroups().forEach((ig, ims) -> {
            Set<String> hgComponents = new HashSet<>(processor.getImpalaCoordinatorsInHostGroup(ig.getGroupName()));
            fillList(result, ims, hgComponents);
        });
        return result;
    }

    public Map<String, List<String>> getComponentLocationByHostname(StackProxy stackProxy, Collection<String> componentNames) {
        return getFqdnsByComponents(stackProxy, componentNames);
    }

    public Map<String, List<String>> getComponentLocation(StackProxy stackProxy, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getFqdnsByComponents(stackProxy, blueprintTextProcessor, componentNames);
    }

    private Map<String, List<String>> getFqdnsByComponents(StackProxy stackProxy, Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByService = new HashMap<>();
        String blueprintText = stackProxy.getBlueprint().getBlueprintText();
        BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        stackProxy.getInstanceGroups().forEach((ig, ims) -> {
            Set<String> hgComponents = new HashSet<>(processor.getComponentsInHostGroup(ig.getGroupName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByService, ims, hgComponents);
        });
        return fqdnsByService;
    }

    private Map<String, List<String>> getFqdnsByComponents(StackProxy stackProxy, BlueprintTextProcessor blueprintTextProcessor, Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByComponent = new HashMap<>();
        stackProxy.getInstanceGroups().forEach((ig, ims) -> {
            Set<String> hgComponents = new HashSet<>(blueprintTextProcessor.getComponentsInHostGroup(ig.getGroupName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByComponent, ims, hgComponents);
        });
        return fqdnsByComponent;
    }

    private void fillList(Map<String, List<String>> fqdnsByComponent, List<InstanceMetadataDto> instanceMetaData, Set<String> hgComponents) {
        List<String> fqdnList = instanceMetaData
                .stream()
                .filter(im -> im.getDiscoveryFQDN() != null)
                .map(InstanceMetadataDto::getDiscoveryFQDN)
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
