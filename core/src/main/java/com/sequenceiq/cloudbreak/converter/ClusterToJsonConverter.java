package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.ExposedService.SHIPYARD;
import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.MARATHON;
import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Optional;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.CustomContainerResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ExposedServices;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.AmbariClientProvider;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.util.StackUtil;

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
    private ConversionService conversionService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private AmbariViewProvider ambariViewProvider;

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    @Inject
    private StackUtil stackUtil;

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
        Set<RDSConfig> rdsConfigs = source.getRdsConfigs();
        convertRdsIds(clusterResponse, rdsConfigs);
        if (source.getLdapConfig() != null) {
            clusterResponse.setLdapConfigId(source.getLdapConfig().getId());
        }
        source = provideViewDefinitions(source);
        if (source.getAttributes() != null) {
            clusterResponse.setAttributes(source.getAttributes().getMap());
        }

        String ambariIp = stackUtil.extractAmbariIp(source.getStack());
        clusterResponse.setAmbariServerIp(ambariIp);
        clusterResponse.setUserName(source.getUserName());
        clusterResponse.setPassword(source.getPassword());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        clusterResponse.setAmbariServerUrl(getAmbariServerUrl(source));
        clusterResponse.setServiceEndPoints(prepareServiceEndpointsMap(source, ambariIp));
        clusterResponse.setBlueprintInputs(convertBlueprintInputs(source.getBlueprintInputs()));
        clusterResponse.setEnableShipyard(source.getEnableShipyard());
        clusterResponse.setConfigStrategy(source.getConfigStrategy());
        clusterResponse.setLdapConfig(getConversionService().convert(source.getLdapConfig(), LdapConfigResponse.class));
        convertRdsConfigs(source, clusterResponse);
        clusterResponse.setBlueprint(getConversionService().convert(source.getBlueprint(), BlueprintResponse.class));
        clusterResponse.setSssdConfig(getConversionService().convert(source.getSssdConfig(), SssdConfigResponse.class));
        convertKnox(source, clusterResponse);
        if (source.getBlueprintCustomProperties() != null) {
            clusterResponse.setBlueprintCustomProperties(jsonHelper.createJsonFromString(source.getBlueprintCustomProperties()));
        }
        convertContainerConfig(source, clusterResponse);
        convertComponentConfig(clusterResponse, source.getId());
        return clusterResponse;
    }

    private ClusterResponse convertComponentConfig(ClusterResponse response, Long clusterId) {
        try {
            AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(clusterId);
            if (ambariRepo != null) {
                AmbariRepoDetailsJson ambariRepoDetailsJson = conversionService.convert(ambariRepo, AmbariRepoDetailsJson.class);
                response.setAmbariRepoDetailsJson(ambariRepoDetailsJson);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }

        return response;
    }

    private void convertRdsIds(ClusterResponse clusterResponse, Set<RDSConfig> rdsConfigs) {
        if (rdsConfigs != null && !rdsConfigs.isEmpty()) {
            for (RDSConfig rdsConfig : rdsConfigs) {
                clusterResponse.getRdsConfigIds().add(rdsConfig.getId());
            }
        }
    }

    private void convertRdsConfigs(Cluster source, ClusterResponse clusterResponse) {
        for (RDSConfig rdsConfig : source.getRdsConfigs()) {
            clusterResponse.getRdsConfigs().add(getConversionService().convert(rdsConfig, RDSConfigResponse.class));
        }
    }

    private void convertKnox(Cluster source, ClusterResponse clusterResponse) {
        clusterResponse.setEnableKnoxGateway(source.getEnableKnoxGateway());
        clusterResponse.setKnoxTopologyName(source.getKnoxTopologyName());
        Json exposedJson = source.getExposedKnoxServices();
        if (exposedJson != null && StringUtils.isNoneEmpty(exposedJson.getValue())) {
            try {
                clusterResponse.setExposedKnoxServices(exposedJson.get(ExposedServices.class).getServices());
            } catch (IOException e) {
                LOGGER.error("Failed to add exposedServices to response", e);
                throw new CloudbreakApiException("Failed to add exposedServices to response", e);
            }
        }
    }

    private void convertContainerConfig(Cluster source, ClusterResponse clusterResponse) {
        Json customContainerDefinition = source.getCustomContainerDefinition();
        if (customContainerDefinition != null && StringUtils.isNoneEmpty(customContainerDefinition.getValue())) {
            try {
                Map<String, String> map = customContainerDefinition.get(Map.class);
                Map<String, String> result = new HashMap<>();

                for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
                    result.put(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
                clusterResponse.setCustomContainers(new CustomContainerResponse(result));

            } catch (IOException e) {
                LOGGER.error("Failed to add customContainerDefinition to response", e);
                throw new CloudbreakApiException("Failed to add customContainerDefinition to response", e);
            }
        }
    }

    private Cluster provideViewDefinitions(Cluster source) {
        if (!Strings.isNullOrEmpty(source.getAmbariIp())
                && (source.getAttributes().getValue() == null || ambariViewProvider.isViewDefinitionNotProvided(source))) {
            try {
                HttpClientConfig clientConfig = tlsSecurityService.buildTLSClientConfig(source.getStack().getId(), source.getAmbariIp());
                AmbariClient ambariClient = ambariClientProvider.getAmbariClient(clientConfig, source.getStack().getGatewayPort(), source);
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

    private Set<HostGroupResponse> convertHostGroupsToJson(Set<HostGroup> hostGroups) {
        Set<HostGroupResponse> jsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupResponse.class));
        }
        return jsons;
    }

    private Map<String, String> prepareServiceEndpointsMap(Cluster cluster, String ambariIp) {
        Set<HostGroup> hostGroups = cluster.getHostGroups();
        Blueprint blueprint = cluster.getBlueprint();
        Boolean shipyardEnabled = cluster.getEnableShipyard();

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
                    collectServicePorts(result, ports, serviceAddress, componentDescriptor, cluster);
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

    private void collectServicePorts(Map<String, String> result, List<Port> ports, String serviceAddress,
            StackServiceComponentDescriptor componentDescriptor, Cluster cluster) throws IOException {
        if (componentDescriptor != null && componentDescriptor.isMaster()) {
            List<String> exposedServices = new ArrayList<>();
            if (cluster.getExposedKnoxServices() != null && cluster.getExposedKnoxServices().getValue() != null) {
                exposedServices = cluster.getExposedKnoxServices().get(ExposedServices.class).getServices();
            }

            for (Port port : ports) {
                if (port.getExposedService().getServiceName().equals(componentDescriptor.getName())) {
                    String url;
                    if (cluster.getEnableKnoxGateway()) {
                        url = String.format("https://%s:8443/gateway/%s%s", cluster.getAmbariIp(), cluster.getKnoxTopologyName(),
                                port.getExposedService().getKnoxUrl());
                        if (exposedServices.contains(port.getExposedService().getKnoxService())) {
                            result.put(port.getExposedService().getPortName(), url);
                        }
                    } else {
                        url = String.format("http://%s:%s%s", serviceAddress, port.getPort(), port.getExposedService().getPostFix());
                        result.put(port.getExposedService().getPortName(), url);
                    }
                }
            }
        }
    }

    private String getAmbariServerUrl(Cluster cluster) {
        String url;
        String orchestrator = cluster.getStack().getOrchestrator().getType();
        if (cluster.getAmbariIp() != null) {
            if (YARN.equals(orchestrator) || MARATHON.equals(orchestrator)) {
                url = String.format("http://%s:8080", cluster.getAmbariIp());
            } else {
                if (cluster.getEnableKnoxGateway() != null && cluster.getEnableKnoxGateway()) {
                    url = String.format("https://%s:8443/gateway/%s/ambari/", cluster.getAmbariIp(), cluster.getKnoxTopologyName());
                } else {
                    url = String.format("https://%s/ambari/", cluster.getAmbariIp());
                }
            }
        } else {
            url = null;
        }
        return url;
    }

}
