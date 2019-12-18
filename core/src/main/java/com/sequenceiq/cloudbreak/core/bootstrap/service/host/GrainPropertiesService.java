package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
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

    List<GrainProperties> createGrainProperties(Iterable<GatewayConfig> gatewayConfigs, Cluster cluster) {
        GrainProperties grainProperties = new GrainProperties();
        for (GatewayConfig gatewayConfig : gatewayConfigs) {
            Map<String, String> hostGrain = new HashMap<>();
            hostGrain.put("gateway-address", gatewayConfig.getPublicAddress());
            grainProperties.put(gatewayConfig.getHostname(), hostGrain);
        }
        addNameNodeRoleForHosts(grainProperties, cluster);
        addKnoxRoleForHosts(grainProperties, cluster);
        return addCloudIdentityRolesForHosts(grainProperties, cluster);
    }

    private void addNameNodeRoleForHosts(GrainProperties grainProperties, Cluster cluster) {
        Map<String, List<String>> nameNodeServiceLocations = getComponentLocationByHostname(cluster, ExposedService.NAMENODE.getServiceName());
        nameNodeServiceLocations.getOrDefault(ExposedService.NAMENODE.getServiceName(), List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "namenode"));
    }

    private void addKnoxRoleForHosts(GrainProperties grainProperties, Cluster cluster) {
        Map<String, List<String>> knoxServiceLocations = getComponentLocationByHostname(cluster, "KNOX_GATEWAY");
        knoxServiceLocations.getOrDefault("KNOX_GATEWAY", List.of())
                .forEach(nmn -> grainProperties.computeIfAbsent(nmn, s -> new HashMap<>()).put(ROLES, "knox"));
    }

    private Map<String, List<String>> getComponentLocationByHostname(Cluster cluster, String componentName) {
        return componentLocator.getComponentLocationByHostname(cluster, List.of(componentName));
    }

    private List<GrainProperties> addCloudIdentityRolesForHosts(GrainProperties grainProperties, Cluster cluster) {
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.getAllInstanceMetadataByStackId(cluster.getStack().getId());
        List<GrainProperties> results = new ArrayList<>();
        results.add(grainProperties);
        GrainProperties propertiesForIdentityRoles = new GrainProperties();
        for (InstanceMetaData instanceMetaData : instanceMetaDataSet) {
            setCloudIdentityRoles(propertiesForIdentityRoles, instanceMetaData);
        }
        results.add(propertiesForIdentityRoles);
        return results;
    }

    private void setCloudIdentityRoles(GrainProperties propertiesForIdentityRoles, InstanceMetaData instanceMetaData) {
        InstanceGroup instanceGroup = instanceMetaData.getInstanceGroup();
        CloudIdentityType cloudIdentityType = instanceGroup.getCloudIdentityType().orElse(CloudIdentityType.LOG);
        Map<String, String> grainsForInstance = new HashMap<>();
        grainsForInstance.put(ROLES, cloudIdentityType.roleName());
        propertiesForIdentityRoles.put(instanceMetaData.getDiscoveryFQDN(), grainsForInstance);
    }

}
