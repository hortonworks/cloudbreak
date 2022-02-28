package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
class GrainPropertiesService {

    private static final String ROLES = "roles";

    @Inject
    private ComponentLocatorService componentLocator;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    List<GrainProperties> createGrainProperties(Iterable<GatewayConfig> gatewayConfigs, Cluster cluster, Set<Node> nodes) {
        List<GrainProperties> grainPropertiesList = new ArrayList<>();
        Optional.ofNullable(addGatewayAddress(gatewayConfigs, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addNameNodeRoleForHosts(cluster, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addKnoxRoleForHosts(cluster, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addIdBrokerRoleForHosts(cluster, Set.of())).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addCloudIdentityRolesForHosts(cluster, nodes)).ifPresent(grainPropertiesList::add);
        return grainPropertiesList;
    }

    List<GrainProperties> createGrainPropertiesForTargetedUpscale(Iterable<GatewayConfig> gatewayConfigs, Cluster cluster, Set<Node> nodes) {
        List<GrainProperties> grainPropertiesList = new ArrayList<>();
        Optional.ofNullable(addGatewayAddress(gatewayConfigs, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addNameNodeRoleForHosts(cluster, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addKnoxRoleForHosts(cluster, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addIdBrokerRoleForHosts(cluster, nodes)).ifPresent(grainPropertiesList::add);
        Optional.ofNullable(addCloudIdentityRolesForHosts(cluster, nodes)).ifPresent(grainPropertiesList::add);
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

    private Map<String, List<String>> filteredLocations(Cluster cluster, String serviceName, Set<Node> nodes) {
        Map<String, List<String>> locations = getComponentLocationByHostname(cluster, serviceName);
        if (!nodes.isEmpty() && !MapUtils.isEmpty(locations) && locations.containsKey(serviceName)) {
            List<String> filteredFqdns = locations.get(serviceName).stream().filter(fqdn ->
                    nodes.stream().map(node -> node.getHostname()).collect(Collectors.toSet()).contains(fqdn)).collect(Collectors.toList());
            return Map.of(serviceName, filteredFqdns);
        }
        return locations;
    }

    private GrainProperties addNameNodeRoleForHosts(Cluster cluster, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        ExposedService nameNodeService = exposedServiceCollector.getNameNodeService();
        Map<String, List<String>> nameNodeServiceLocations = filteredLocations(cluster, nameNodeService.getServiceName(), nodes);
        nameNodeServiceLocations
                .getOrDefault(nameNodeService.getServiceName(), List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "namenode"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private GrainProperties addKnoxRoleForHosts(Cluster cluster, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        Map<String, List<String>> knoxServiceLocations = filteredLocations(cluster, KnoxRoles.KNOX_GATEWAY, nodes);
        knoxServiceLocations
                .getOrDefault(KnoxRoles.KNOX_GATEWAY, List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "knox"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private GrainProperties addIdBrokerRoleForHosts(Cluster cluster, Set<Node> nodes) {
        GrainProperties grainProperties = new GrainProperties();
        Map<String, List<String>> idBrokerServiceLocations = filteredLocations(cluster, KnoxRoles.IDBROKER, nodes);
        idBrokerServiceLocations
                .getOrDefault(KnoxRoles.IDBROKER, List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "idbroker"));
        return grainProperties.getProperties().isEmpty() ? null : grainProperties;
    }

    private Map<String, List<String>> getComponentLocationByHostname(Cluster cluster, String componentName) {
        return componentLocator.getComponentLocationByHostname(cluster, List.of(componentName));
    }

    private GrainProperties addCloudIdentityRolesForHosts(Cluster cluster, Set<Node> nodes) {
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.getAllInstanceMetadataByStackId(cluster.getStack().getId());
        GrainProperties propertiesForIdentityRoles = new GrainProperties();
        Set<String> hostNames = nodes.stream().map(Node::getHostname).collect(toSet());
        for (InstanceMetaData instanceMetaData : instanceMetaDataSet) {
            if (hostNames.contains(instanceMetaData.getDiscoveryFQDN())) {
                setCloudIdentityRoles(propertiesForIdentityRoles, instanceMetaData);
            }
        }
        return propertiesForIdentityRoles.getProperties().isEmpty() ? null : propertiesForIdentityRoles;
    }

    private void setCloudIdentityRoles(GrainProperties propertiesForIdentityRoles, InstanceMetaData instanceMetaData) {
        InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
        CloudIdentityType cloudIdentityType = instanceGroup.getCloudIdentityType().orElse(CloudIdentityType.LOG);
        Map<String, String> grainsForInstance = new HashMap<>();
        grainsForInstance.put(ROLES, cloudIdentityType.roleName());
        propertiesForIdentityRoles.put(instanceMetaData.getDiscoveryFQDN(), grainsForInstance);
    }

}
