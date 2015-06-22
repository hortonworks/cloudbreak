package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BAYWATCH_ENABLED;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.CONSUL;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.ELASTIC_SEARCH;
import static com.sequenceiq.cloudbreak.service.network.ExposedService.KIBANA;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.network.ExposedService;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;

@Component
public class ClusterToJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterResponse> {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    @Value("${cb.baywatch.enabled:" + CB_BAYWATCH_ENABLED + "}")
    private Boolean baywatchEnabled;

    @Inject
    private BlueprintValidator blueprintValidator;
    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Override
    public ClusterResponse convert(Cluster source) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(source.getId());
        clusterResponse.setStatus(source.getStatus().name());
        clusterResponse.setStatusReason(source.getStatusReason());
        if (source.getBlueprint() != null) {
            clusterResponse.setBlueprintId(source.getBlueprint().getId());
        }
        if (source.getUpSince() != null && source.isAvailable()) {
            long now = new Date().getTime();
            long uptime = now - source.getUpSince();
            int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
            int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
            clusterResponse.setHoursUp(hours);
            clusterResponse.setMinutesUp(minutes);
        } else {
            clusterResponse.setHoursUp(0);
            clusterResponse.setMinutesUp(0);
        }
        AmbariStackDetails ambariStackDetails = source.getAmbariStackDetails();
        if (ambariStackDetails != null) {
            clusterResponse.setAmbariStackDetails(getConversionService().convert(ambariStackDetails, AmbariStackDetailsJson.class));
        }
        clusterResponse.setAmbariServerIp(source.getAmbariIp());
        clusterResponse.setUserName(source.getUserName());
        clusterResponse.setPassword(source.getPassword());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        clusterResponse.setServiceEndPoints(prepareServiceEndpointsMap(source.getHostGroups(), source.getBlueprint(), source.getAmbariIp()));
        return clusterResponse;
    }

    private Set<HostGroupJson> convertHostGroupsToJson(Set<HostGroup> hostGroups) {
        Set<HostGroupJson> jsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupJson.class));
        }
        return jsons;
    }

    private Map<String, String> prepareServiceEndpointsMap(Set<HostGroup> hostGroups, Blueprint blueprint, String ambariIp) {
        Map<String, String> result = new HashMap<>();

        List<Port> ports = NetworkUtils.getPorts(Optional.<Stack>absent());
        try {
            JsonNode hostGroupsNode = blueprintValidator.getHostGroupNode(blueprint);
            for (JsonNode hostGroupNode : hostGroupsNode) {
                String hostGroupName = blueprintValidator.getHostGroupName(hostGroupNode);
                Map<String, HostGroup> hostGroupMap = blueprintValidator.createHostGroupMap(hostGroups);
                JsonNode componentsNode = blueprintValidator.getComponentsNode(hostGroupNode);
                HostGroup actualHostgroup = hostGroupMap.get(hostGroupName);
                InstanceMetaData next = actualHostgroup.getInstanceGroup().getInstanceMetaData().iterator().next();
                for (JsonNode componentNode : componentsNode) {
                    String componentName = componentNode.get("name").asText();
                    StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
                    collectServicePorts(result, ports, next, componentDescriptor);
                }
            }
            if (ambariIp != null) {
                collectLoggingPorts(ambariIp, result, ports);
                collectAdditionalPorts(ambariIp, result, ports);
            }
        } catch (Exception ex) {
            return result;
        }
        return result;
    }

    private void collectAdditionalPorts(String ambariIp, Map<String, String> result, List<Port> ports) {
        Optional<Port> consulOnGateWay = getPortForService(CONSUL, ports);
        if (consulOnGateWay.isPresent()) {
            result.put(consulOnGateWay.get().getExposedService().getPortName(), String.format("%s:%s", ambariIp, consulOnGateWay.get().getPort()));
        }
    }

    private void collectLoggingPorts(String ambariIp, Map<String, String> result, List<Port> ports) {
        if (baywatchEnabled) {
            Optional<Port> kibana = getPortForService(KIBANA, ports);
            if (kibana.isPresent()) {
                result.put(kibana.get().getExposedService().getPortName(), String.format("%s:%s", ambariIp, kibana.get().getPort()));
            }
            Optional<Port> elasticSearch = getPortForService(ELASTIC_SEARCH, ports);
            if (elasticSearch.isPresent()) {
                result.put(elasticSearch.get().getExposedService().getPortName(), String.format("%s:%s", ambariIp, elasticSearch.get().getPort()));
            }
        }
    }

    private void collectServicePorts(Map<String, String> result, List<Port> ports, InstanceMetaData next, StackServiceComponentDescriptor componentDescriptor) {
        if (componentDescriptor != null && componentDescriptor.isMaster()) {
            for (Port port : ports) {
                if (port.getExposedService().getServiceName().equals(componentDescriptor.getName())) {
                    result.put(port.getExposedService().getPortName(),
                            String.format("%s:%s%s", next.getPublicIp(), port.getPort(), port.getExposedService().getPostFix()));
                }
            }
            if ("GANGLIA_SERVER".equals(componentDescriptor.getName())) {
                result.put("Ganglia", String.format("%s/%s", next.getPublicIp(), "ganglia"));
            }
        }
    }

    private Optional<Port> getPortForService(ExposedService exposedService, List<Port> ports) {
        for (Port port : ports) {
            if (port.getExposedService().getServiceName().equals(exposedService.getServiceName())) {
                return Optional.fromNullable(port);
            }
        }
        return Optional.absent();
    }
}
