package com.sequenceiq.cloudbreak.service.blueprint;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class ComponentLocatorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentLocatorService.class);

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public Map<String, List<String>> getComponentLocation(StackDtoDelegate stackDto, Collection<String> componentNames) {
        return getFqdnsByComponents(stackDto, componentNames);
    }

    public Map<String, List<String>> getImpalaCoordinatorLocations(StackDtoDelegate stack) {
        Map<String, List<String>> result = new HashMap<>();
        Blueprint blueprint = stack.getBlueprint();
        if (blueprint != null) {
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText());

            stack.getInstanceGroupDtos().forEach(ig -> {
                Set<String> hgComponents = new HashSet<>(processor.getImpalaCoordinatorsInHostGroup(ig.getInstanceGroup().getGroupName()));
                fillList(result, ig.getReachableInstanceMetaData(), hgComponents);
            });
        } else {
            LOGGER.info("No blueprint available on stack with CRN: '{}'", stack.getResourceCrn());
        }
        return result;
    }

    public Map<String, List<String>> getComponentLocationByHostname(StackDtoDelegate stackDto, Collection<String> componentNames) {
        return getFqdnsByComponents(stackDto, componentNames);
    }

    public Map<String, List<String>> getComponentLocation(StackDtoDelegate stackDto, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getFqdnsByComponents(stackDto, blueprintTextProcessor, componentNames);
    }

    public Map<String, List<String>> getComponentLocationEvenIfStopped(StackDtoDelegate stackDto, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        return getFqdnsByComponentsEvenIfStopped(stackDto, blueprintTextProcessor, componentNames);
    }

    private Map<String, List<String>> getFqdnsByComponents(StackDtoDelegate stackDto, Collection<String> componentNames) {
        String blueprintText = stackDto.getBlueprintJsonText();
        BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        return getFqdnsByComponents(stackDto, processor, componentNames);
    }

    private Map<String, List<String>> getFqdnsByComponents(StackDtoDelegate stackDto, BlueprintTextProcessor blueprintTextProcessor,
            Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByComponent = new HashMap<>();
        stackDto.getInstanceGroupDtos().forEach(ig -> {
            Set<String> hgComponents = new HashSet<>(blueprintTextProcessor.getComponentsInHostGroup(ig.getInstanceGroup().getGroupName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByComponent, ig.getReachableInstanceMetaData(), hgComponents);
        });
        return fqdnsByComponent;
    }

    private Map<String, List<String>> getFqdnsByComponentsEvenIfStopped(StackDtoDelegate stackDto, BlueprintTextProcessor blueprintTextProcessor,
        Collection<String> componentNames) {
        Map<String, List<String>> fqdnsByComponent = new HashMap<>();
        stackDto.getInstanceGroupDtos().forEach(ig -> {
            Set<String> hgComponents = new HashSet<>(blueprintTextProcessor.getComponentsInHostGroup(ig.getInstanceGroup().getGroupName()));
            hgComponents.retainAll(componentNames);
            fillList(fqdnsByComponent, ig.getReachableOrStoppedInstanceMetaData(), hgComponents);
        });
        return fqdnsByComponent;
    }

    private void fillList(Map<String, List<String>> fqdnsByComponent, List<InstanceMetadataView> instanceMetaData, Set<String> hgComponents) {
        List<String> fqdnList = instanceMetaData
                .stream()
                .filter(im -> im.getDiscoveryFQDN() != null)
                .map(InstanceMetadataView::getDiscoveryFQDN)
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
