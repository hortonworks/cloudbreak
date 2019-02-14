package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;

@Service
public class AmbariHaComponentFilter {

    public Set<String> getHaComponents(AmbariBlueprintTextProcessor ambariBlueprintTextProcessor) {
        Map<String, Set<String>> componentsByHostGroup = ambariBlueprintTextProcessor.getComponentsByHostGroup();
        Set<String> haComponents = ExposedService.filterSupportedKnoxServices().stream()
                .map(ExposedService::getServiceName)
                .filter(component -> isComponentPresentMoreThanOneHostGroup(componentsByHostGroup, component))
                .collect(Collectors.toSet());


        removeDisabledHaServices(haComponents, ambariBlueprintTextProcessor);
        return haComponents;
    }

    private boolean isComponentPresentMoreThanOneHostGroup(Map<String, Set<String>> componentsByHostGroup, String serviceName) {
        long occurrences = componentsByHostGroup.values()
                .stream()
                .filter(componentsInGroup -> componentsInGroup.contains(serviceName))
                .count();
        return occurrences > 1L;
    }

    private void removeDisabledHaServices(Set<String> haServices, AmbariBlueprintTextProcessor ambariBlueprintTextProcessor) {
        Map<String, Map<String, String>> configurations = ambariBlueprintTextProcessor.getConfigurationEntries();
        removeComponentIfHaDisabled(haServices, configurations, "hdfs-site", "dfs.ha.automatic-failover.enabled",
                ExposedService.NAMENODE.getServiceName());
        removeComponentIfHaDisabled(haServices, configurations, "yarn-site", "yarn.resourcemanager.ha.enabled",
                ExposedService.RESOURCEMANAGER_WEB.getServiceName());
    }

    private void removeComponentIfHaDisabled(Set<String> haServices, Map<String, Map<String, String>> configurations, String configName,
            String propertyName, String serviceName) {
        if (configurations.containsKey(configName)) {
            Map<String, String> configuration = configurations.get(configName);
            if (configuration.containsKey(propertyName)) {
                if (!Boolean.parseBoolean(configuration.get(propertyName))) {
                    haServices.remove(serviceName);
                }
            }
        }
    }
}
