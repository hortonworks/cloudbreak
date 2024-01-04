package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
class GrainPropertiesService {

    private static final String ROLES = "roles";

    @Inject
    private ComponentLocatorService componentLocator;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    List<GrainProperties> createGrainProperties(Iterable<GatewayConfig> gatewayConfigs, StackDto stackDto, Set<Node> nodes) {
        List<GrainProperties> grainPropertiesList = new ArrayList<>();
        Optional.ofNullable(addGatewayAddress(gatewayConfigs, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addNameNodeRoleForHosts(stackDto, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addKnoxRoleForHosts(stackDto, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addIdBrokerRoleForHosts(stackDto, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addCloudIdentityRolesForHosts(stackDto, nodes)).ifPresent(grainPropertiesList::add);
        return grainPropertiesList;
    }

    List<GrainProperties> createGrainPropertiesForTargetedUpscale(Iterable<GatewayConfig> gatewayConfigs, StackDto stackDto, Set<Node> nodes) {
        List<GrainProperties> grainPropertiesList = new ArrayList<>();
        Optional.ofNullable(addGatewayAddress(gatewayConfigs, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addNameNodeRoleForHosts(stackDto, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addKnoxRoleForHosts(stackDto, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addIdBrokerRoleForHosts(stackDto, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addCloudIdentityRolesForHosts(stackDto, nodes)).ifPresent(grainPropertiesList::add);
        return grainPropertiesList;
    }

    private GrainProperties addGatewayAddress(Iterable<GatewayConfig> gatewayConfigs, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        for (GatewayConfig gatewayConfig : gatewayConfigs) {
            boolean nodesContainsGateway = nodes.stream().map(node -> node.getPrivateIp())
                    .collect(Collectors.toList()).contains(gatewayConfig.getPrivateAddress());
            if (nodes.isEmpty() || nodesContainsGateway) {
                grainProperties.put(gatewayConfig.getHostname(), Map.of("gateway-address", gatewayConfig.getPublicAddress()));
            }
        }
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private Map<String, List<String>> filteredLocations(StackDto stackDto, String serviceName, Set<Node> nodes) {
        Map<String, List<String>> locations = getComponentLocationByHostname(stackDto, serviceName);
        if (!nodes.isEmpty() && !MapUtils.isEmpty(locations) && locations.containsKey(serviceName)) {
            List<String> filteredFqdns = locations.get(serviceName).stream().filter(fqdn ->
                    nodes.stream().map(node -> node.getHostname()).collect(Collectors.toSet()).contains(fqdn)).collect(Collectors.toList());
            return Map.of(serviceName, filteredFqdns);
        }
        return locations;
    }

    private GrainProperties addNameNodeRoleForHosts(StackDto stackDto, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        ExposedService nameNodeService = exposedServiceCollector.getNameNodeService();
        Map<String, List<String>> nameNodeServiceLocations = filteredLocations(stackDto, nameNodeService.getServiceName(), nodes);
        nameNodeServiceLocations
                .getOrDefault(nameNodeService.getServiceName(), List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "namenode"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private GrainProperties addKnoxRoleForHosts(StackDto stackDto, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        Map<String, List<String>> knoxServiceLocations = filteredLocations(stackDto, KnoxRoles.KNOX_GATEWAY, nodes);
        knoxServiceLocations
                .getOrDefault(KnoxRoles.KNOX_GATEWAY, List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "knox"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private GrainProperties addIdBrokerRoleForHosts(StackDto stackDto, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        Map<String, List<String>> idBrokerServiceLocations = filteredLocations(stackDto, KnoxRoles.IDBROKER, nodes);
        idBrokerServiceLocations
                .getOrDefault(KnoxRoles.IDBROKER, List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "idbroker"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private Map<String, List<String>> getComponentLocationByHostname(StackDto stackDto, String componentName) {
        return componentLocator.getComponentLocationByHostname(stackDto, List.of(componentName));
    }

    private GrainProperties addCloudIdentityRolesForHosts(StackDto stackDto, Set<Node> nodes) {
        GrainProperties propertiesForIdentityRoles = new GrainProperties();
        Set<String> hostNames = nodes.stream().map(Node::getHostname).collect(toSet());
        for (InstanceGroupDto instanceGroupDto : stackDto.getInstanceGroupDtos()) {
            instanceGroupDto.getInstanceMetadataViews().forEach(im -> {
                if (hostNames.contains(im.getDiscoveryFQDN())) {
                    setCloudIdentityRoles(propertiesForIdentityRoles, im, instanceGroupDto.getInstanceGroup());
                }
            });
        }
        return propertiesForIdentityRoles.getProperties().isEmpty() ? null : propertiesForIdentityRoles;
    }

    private void setCloudIdentityRoles(GrainProperties propertiesForIdentityRoles, InstanceMetadataView instanceMetaData, InstanceGroupView instanceGroup) {
        CloudIdentityType cloudIdentityType = instanceGroup.getCloudIdentityType().orElse(CloudIdentityType.LOG);
        Map<String, String> grainsForInstance = new HashMap<>();
        grainsForInstance.put(ROLES, cloudIdentityType.roleName());
        propertiesForIdentityRoles.put(instanceMetaData.getDiscoveryFQDN(), grainsForInstance);
    }

}
