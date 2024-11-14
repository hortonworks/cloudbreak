package com.sequenceiq.cloudbreak.service.stack;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class DependentRolesHealthCheckService {

    public static final String UNDEFINED_DEPENDENCY = "UNDEFINED_DEPENDENCY";

    public static final String GOOD_HEALTH = "GOOD";

    private static final Map<String, List<String>> ROLE_DEPENDENCIES = Map.ofEntries(
            Map.entry("NODEMANAGER", List.of("RESOURCEMANAGER")));

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    /**
     * Returns Set of Dependent Components on which the Source Components of the hostgroup depends.
     * @param hostGroup We need to find the dependentComponents for the components present in this hostGroup.
     * @param processor it is used to find all the components which are running in the given hostgroup.
     * @return Set of components on which the Source Components of the given hostGroup Depends.
     */

    public Set<String> getDependentComponentsForHostGroup(CmTemplateProcessor processor, String hostGroup) {
        return processor.getNonGatewayComponentsByHostGroup().get(hostGroup).stream()
                .map(srcComponent -> ROLE_DEPENDENCIES.getOrDefault(srcComponent, Collections.singletonList(UNDEFINED_DEPENDENCY)))
                .flatMap(Collection::stream)
                .collect(toSet());
    }

    public Set<String> getDependentHostGroupsForHostGroup(CmTemplateProcessor processor, String hostGroup) {
        Set<String> dependentComponents = getDependentComponentsForHostGroup(processor, hostGroup);
        return dependentComponents.contains(UNDEFINED_DEPENDENCY)
                ? Collections.singleton(UNDEFINED_DEPENDENCY)
                : processor.getNonGatewayComponentsByHostGroup()
                .entrySet().stream()
                .filter(e -> isDependentComponentPresentInHostGroup(e.getValue(), dependentComponents))
                .map(Map.Entry::getKey)
                .collect(toSet());
    }

    private boolean isDependentComponentPresentInHostGroup(Collection<String> components, Collection<String> dependentComponents) {
        return dependentComponents.stream().anyMatch(components::contains);
    }

    /**
     * Returns List of Dependent Hostgroups which has unhealthy services.
     * @param dependentComponents these are the Set of components for which we need to first find there hostGroup and check the health of that hostgroup.
     * @param stackDto It is used to get the health of all the avaiable instances and from that we will filter the ones which we require.
     * @param templateProcessor  it is used to find the hostGroup in which our dependComponent is running.
     * @return List of Hostgroups which have our dependentComponents and are not healthy.
     */

    public List<String> getUnhealthyDependentHostGroups(StackDto stackDto, CmTemplateProcessor templateProcessor, Set<String> dependentComponents) {
        Set<String> dependentHostGroupNames = templateProcessor.getNonGatewayComponentsByHostGroup().entrySet().stream()
                .filter(hgComponents ->
                        isDependentComponentPresentInHostGroup(hgComponents.getValue(), dependentComponents))
                .map(Map.Entry::getKey)
                .collect(toSet());
        Set<String> allUnHealthyHostGroupNames = stackDto.getAllAvailableInstances().stream()
                .filter(instanceMetaData -> !InstanceStatus.SERVICES_HEALTHY.equals(instanceMetaData.getInstanceStatus()))
                .map(InstanceMetadataView::getInstanceGroupName)
                .collect(toSet());
        return dependentHostGroupNames.stream()
                .filter(allUnHealthyHostGroupNames::contains)
                .collect(toList());
    }

    public Set<String> getUnhealthyDependentComponents(StackDto stack, CmTemplateProcessor processor, String hostGroup) {
        Set<String> dependentComponents = getDependentComponentsForHostGroup(processor, hostGroup);
        if (dependentComponents.contains(UNDEFINED_DEPENDENCY)) {
            return Set.of(UNDEFINED_DEPENDENCY);
        }
        List<String> unhealthyDependentHostGroups = getUnhealthyDependentHostGroups(stack, processor, dependentComponents);
        if (!unhealthyDependentHostGroups.isEmpty()) {
            ClusterHealthService clusterHealthService = clusterApiConnectors.getConnector(stack).clusterHealthService();
            Map<String, String> dependentComponentsHealthCheck = clusterHealthService.readServicesHealth(stack.getResourceName());
            Set<String> unhealthyComponents = new HashSet<>();
            for (String component : dependentComponents) {
                String healthCheck = String.valueOf(dependentComponentsHealthCheck
                        .entrySet()
                        .stream()
                        .filter(i -> i.getKey().contains(component))
                        .map(Map.Entry::getValue)
                        .findFirst().get());
                if (!Objects.equals(healthCheck, GOOD_HEALTH)) {
                    unhealthyComponents.add(component);
                }
            }
            return unhealthyComponents;
        }
        return Collections.emptySet();
    }
}