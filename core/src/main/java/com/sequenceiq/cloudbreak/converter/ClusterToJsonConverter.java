package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.service.network.ExposedService.SHIPYARD;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Optional;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.network.Port;

@Component
public class ClusterToJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToJsonConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private BlueprintValidator blueprintValidator;
    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;
    @Inject
    private TlsSecurityService tlsSecurityService;
    @Inject
    private AmbariClientProvider ambariClientProvider;
    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Override
    public ClusterResponse convert(Cluster source) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
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
        clusterResponse.setLdapRequired(source.isLdapRequired());
        if (source.getSssdConfig() != null) {
            clusterResponse.setSssdConfigId(source.getSssdConfig().getId());
        }
        RDSConfig rdsConfig = source.getRdsConfig();
        if (rdsConfig != null) {
            clusterResponse.setRdsConfigId(rdsConfig.getId());
        }
        if (source.getLdapConfig() != null) {
            clusterResponse.setLdapConfigId(source.getLdapConfig().getId());
        }
        source = provideViewDefinitions(source);
        if (source.getAttributes() != null) {
            clusterResponse.setAttributes(source.getAttributes().getMap());
        }
        clusterResponse.setAmbariServerIp(source.getAmbariIp());
        clusterResponse.setUserName(source.getUserName());
        clusterResponse.setPassword(source.getPassword());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        clusterResponse.setServiceEndPoints(prepareServiceEndpointsMap(source.getHostGroups(), source.getBlueprint(), source.getAmbariIp(),
                source.getEnableShipyard()));
        clusterResponse.setBlueprintInputs(convertBlueprintInputs(source.getBlueprintInputs()));
        clusterResponse.setEnableShipyard(source.getEnableShipyard());
        clusterResponse.setConfigStrategy(source.getConfigStrategy());
        return clusterResponse;
    }

    private Cluster provideViewDefinitions(Cluster source) {
        if ((source.getAttributes().getValue() == null || ambariViewProvider.isViewDefinitionNotProvided(source))
                && !Strings.isNullOrEmpty(source.getAmbariIp())) {
            try {
                HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(source.getStack().getId(), source.getAmbariIp());
                AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, source.getStack().getGatewayPort(),
                        source.getUserName(), source.getPassword());
                return ambariViewProvider.provideViewInformation(ambariClient, source);
            } catch (CloudbreakSecuritySetupException e) {
                LOGGER.error("Unable to setup ambari client tls configs: ", e);
            }
        }
        return source;
    }

    private Set<BlueprintInputJson> convertBlueprintInputs(Json inputs) {
        Set<BlueprintInputJson> blueprintInputJsons = new HashSet<>();
        try {
            if (inputs != null && inputs.getValue() != null) {
                Map<String, String> is = inputs.get(Map.class);
                for (Map.Entry<String, String> stringStringEntry : is.entrySet()) {
                    BlueprintInputJson blueprintInputJson = new BlueprintInputJson();
                    blueprintInputJson.setName(stringStringEntry.getKey());
                    blueprintInputJson.setPropertyValue(stringStringEntry.getValue());
                    blueprintInputJsons.add(blueprintInputJson);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not convert blueprintinputs json to Set.");
        }
        return blueprintInputJsons;

    }

    private Set<HostGroupJson> convertHostGroupsToJson(Set<HostGroup> hostGroups) {
        Set<HostGroupJson> jsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupJson.class));
        }
        return jsons;
    }

    private Map<String, String> prepareServiceEndpointsMap(Set<HostGroup> hostGroups, Blueprint blueprint, String ambariIp, Boolean shipyardEnabled) {
        Map<String, String> result = new HashMap<>();

        List<Port> ports = NetworkUtils.getPorts(Optional.absent());
        collectPortsOfAdditionalServices(result, ambariIp, shipyardEnabled);
        try {
            JsonNode hostGroupsNode = blueprintValidator.getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = blueprintValidator.createHostGroupMap(hostGroups);
            for (JsonNode hostGroupNode : hostGroupsNode) {
                String hostGroupName = blueprintValidator.getHostGroupName(hostGroupNode);
                JsonNode componentsNode = blueprintValidator.getComponentsNode(hostGroupNode);
                HostGroup actualHostgroup = hostGroupMap.get(hostGroupName);
                String serviceAddress;
                if (actualHostgroup.getConstraint().getInstanceGroup() != null) {
                    InstanceMetaData next = actualHostgroup.getConstraint().getInstanceGroup().getInstanceMetaData().iterator().next();
                    serviceAddress = next.getPublicIpWrapper();
                } else {
                    serviceAddress = actualHostgroup.getHostMetadata().iterator().next().getHostName();
                }
                for (JsonNode componentNode : componentsNode) {
                    String componentName = componentNode.get("name").asText();
                    StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
                    collectServicePorts(result, ports, serviceAddress, componentDescriptor);
                }
            }
        } catch (Exception ex) {
            return result;
        }
        return result;
    }

    private void collectPortsOfAdditionalServices(Map<String, String> result, String ambariIp, Boolean shipyardEnabled) {
        if (BooleanUtils.isTrue(shipyardEnabled)) {
            Port shipyardPort = NetworkUtils.getPortByServiceName(SHIPYARD);
            result.put(shipyardPort.getName(), String.format("%s:%s%s", ambariIp, shipyardPort.getPort(), shipyardPort.getExposedService().getPostFix()));
        }
    }

    private void collectServicePorts(Map<String, String> result, List<Port> ports, String address, StackServiceComponentDescriptor componentDescriptor) {
        if (componentDescriptor != null && componentDescriptor.isMaster()) {
            for (Port port : ports) {
                if (port.getExposedService().getServiceName().equals(componentDescriptor.getName())) {
                    result.put(port.getExposedService().getPortName(),
                            String.format("%s:%s%s", address, port.getPort(), port.getExposedService().getPostFix()));
                }
            }
        }
    }

}
