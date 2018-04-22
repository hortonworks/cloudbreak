package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;
import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.CustomContainerResponse;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.CloudbreakApiException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ExposedServices;
import com.sequenceiq.cloudbreak.domain.Gateway;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class ClusterToClusterResponseConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterResponseConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private ClusterComponentConfigProvider componentConfigProvider;

    @Inject
    private StackUtil stackUtil;

    @Override
    public ClusterResponse convert(Cluster source) {
        try {
            return convert(source, ClusterResponse.class);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    protected <R extends ClusterResponse> R convert(Cluster source, Class<R> clazz) throws IllegalAccessException, InstantiationException {
        R clusterResponse = clazz.newInstance();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        clusterResponse.setStatus(source.getStatus());
        clusterResponse.setStatusReason(source.getStatusReason());
        if (source.getBlueprint() != null) {
            clusterResponse.setBlueprintId(source.getBlueprint().getId());
        }

        long uptime = stackUtil.getUptimeForCluster(source, source.isAvailable());
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);

        Set<RDSConfig> rdsConfigs = source.getRdsConfigs();
        convertRdsIds(clusterResponse, rdsConfigs);
        if (source.getLdapConfig() != null) {
            clusterResponse.setLdapConfigId(source.getLdapConfig().getId());
        }
        if (source.getAttributes() != null) {
            clusterResponse.setAttributes(source.getAttributes().getMap());
        }
        String ambariIp = stackUtil.extractAmbariIp(source.getStack());
        clusterResponse.setAmbariServerIp(ambariIp);
        clusterResponse.setUserName(source.getUserName());
        clusterResponse.setExecutorType(source.getExecutorType());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        clusterResponse.setAmbariServerUrl(getAmbariServerUrl(source, ambariIp));
        clusterResponse.setServiceEndPoints(prepareServiceEndpointsMap(source, ambariIp));
        clusterResponse.setBlueprintInputs(convertBlueprintInputs(source.getBlueprintInputs()));
        clusterResponse.setConfigStrategy(source.getConfigStrategy());
        clusterResponse.setLdapConfig(getConversionService().convert(source.getLdapConfig(), LdapConfigResponse.class));
        convertRdsConfigs(source, clusterResponse);
        clusterResponse.setBlueprint(getConversionService().convert(source.getBlueprint(), BlueprintResponse.class));
        convertKnox(source, clusterResponse);
        convertCustomQueue(source, clusterResponse);
        if (source.getBlueprintCustomProperties() != null) {
            clusterResponse.setBlueprintCustomProperties(jsonHelper.createJsonFromString(source.getBlueprintCustomProperties()));
        }
        convertContainerConfig(source, clusterResponse);
        convertComponentConfig(clusterResponse, source);
        convertAmbariDatabaseComponentConfig(clusterResponse, source);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        KerberosConfig kerberosConfig = source.getKerberosConfig();
        if (source.isSecure() && kerberosConfig != null) {
            clusterResponse.setSecure(source.isSecure());
            clusterResponse.setKerberosResponse(getConversionService().convert(source.getKerberosConfig(), KerberosResponse.class));
        }
        return clusterResponse;
    }

    private void convertComponentConfig(ClusterResponse response, Cluster source) {
        try {
            AmbariRepo ambariRepo = componentConfigProvider.getAmbariRepo(source.getComponents());
            if (ambariRepo != null) {
                AmbariRepoDetailsJson ambariRepoDetailsJson = getConversionService().convert(ambariRepo, AmbariRepoDetailsJson.class);
                response.setAmbariRepoDetailsJson(ambariRepoDetailsJson);
            }
            StackRepoDetails stackRepoDetails = componentConfigProvider.getStackRepo(source.getComponents());
            if (stackRepoDetails != null) {
                AmbariStackDetailsResponse ambariRepoDetailsJson = getConversionService().convert(stackRepoDetails, AmbariStackDetailsResponse.class);
                response.setAmbariStackDetails(ambariRepoDetailsJson);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }

    }

    private void convertAmbariDatabaseComponentConfig(ClusterResponse response, Cluster source) {
        try {
            AmbariDatabase ambariDatabase = componentConfigProvider.getAmbariDatabase(source.getComponents());
            if (ambariDatabase != null) {
                AmbariDatabaseDetailsJson ambariRepoDetailsJson = getConversionService().convert(ambariDatabase, AmbariDatabaseDetailsJson.class);
                response.setAmbariDatabaseDetails(ambariRepoDetailsJson);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to convert dynamic component.", e);
        }

    }

    private void convertCustomQueue(Cluster source, ClusterResponse clusterResponse) {
        if (source.getAttributes().getValue() != null) {
            Map<String, Object> attributes = source.getAttributes().getMap();
            Object customQueue = attributes.get(CUSTOM_QUEUE.name());
            if (customQueue != null) {
                clusterResponse.setCustomQueue(customQueue.toString());
            } else {
                clusterResponse.setCustomQueue("default");
            }
        }
    }

    private void convertRdsIds(ClusterResponse clusterResponse, Collection<RDSConfig> rdsConfigs) {
        if (rdsConfigs != null && !rdsConfigs.isEmpty()) {
            for (RDSConfig rdsConfig : rdsConfigs) {
                clusterResponse.getRdsConfigIds().add(rdsConfig.getId());
            }
        }
    }

    private void convertRdsConfigs(Cluster source, ClusterResponse clusterResponse) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(source.getOwner(), source.getAccount(), source.getId());
        for (RDSConfig rdsConfig : rdsConfigs) {
            clusterResponse.getRdsConfigs().add(getConversionService().convert(rdsConfig, RDSConfigResponse.class));
        }
    }

    private void convertKnox(Cluster source, ClusterResponse clusterResponse) {
        Gateway gateway = source.getGateway();
        GatewayJson cloudGatewayJson = new GatewayJson();

        cloudGatewayJson.setEnableGateway(gateway.getEnableGateway());
        cloudGatewayJson.setTopologyName(gateway.getTopologyName());
        Json exposedJson = gateway.getExposedServices();
        if (exposedJson != null && StringUtils.isNoneEmpty(exposedJson.getValue())) {
            try {
                cloudGatewayJson.setExposedServices(exposedJson.get(ExposedServices.class).getServices());
            } catch (IOException e) {
                LOGGER.error("Failed to add exposedServices to response", e);
                throw new CloudbreakApiException("Failed to add exposedServices to response", e);
            }
        }
        cloudGatewayJson.setPath(gateway.getPath());
        cloudGatewayJson.setTokenCert(gateway.getTokenCert());
        cloudGatewayJson.setSsoProvider(gateway.getSsoProvider());
        cloudGatewayJson.setSsoType(gateway.getSsoType());
        cloudGatewayJson.setGatewayType(gateway.getGatewayType());
        clusterResponse.setGateway(cloudGatewayJson);
    }

    private void convertContainerConfig(Cluster source, ClusterResponse clusterResponse) {
        Json customContainerDefinition = source.getCustomContainerDefinition();
        if (customContainerDefinition != null && StringUtils.isNoneEmpty(customContainerDefinition.getValue())) {
            try {
                Map<String, String> map = customContainerDefinition.get(Map.class);
                Map<String, String> result = new HashMap<>();

                for (Entry<String, String> stringStringEntry : map.entrySet()) {
                    result.put(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
                clusterResponse.setCustomContainers(new CustomContainerResponse(result));

            } catch (IOException e) {
                LOGGER.error("Failed to add customContainerDefinition to response", e);
                throw new CloudbreakApiException("Failed to add customContainerDefinition to response", e);
            }
        }
    }

    private Set<BlueprintInputJson> convertBlueprintInputs(Json inputs) {
        Set<BlueprintInputJson> blueprintInputJsons = new HashSet<>();
        try {
            if (inputs != null && inputs.getValue() != null) {
                Map<String, String> is = inputs.get(Map.class);
                for (Entry<String, String> stringStringEntry : is.entrySet()) {
                    BlueprintInputJson blueprintInputJson = new BlueprintInputJson();
                    blueprintInputJson.setName(stringStringEntry.getKey());
                    blueprintInputJson.setPropertyValue(stringStringEntry.getValue());
                    blueprintInputJsons.add(blueprintInputJson);
                }
            }
        } catch (IOException ignored) {
            LOGGER.error("Could not convert blueprintinputs json to Set.");
        }
        return blueprintInputJsons;

    }

    private Set<HostGroupResponse> convertHostGroupsToJson(Iterable<HostGroup> hostGroups) {
        Set<HostGroupResponse> jsons = new HashSet<>();
        for (HostGroup hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupResponse.class));
        }
        return jsons;
    }

    private Map<String, String> prepareServiceEndpointsMap(Cluster cluster, String ambariIp) {
        Set<HostGroup> hostGroups = cluster.getHostGroups();
        Blueprint blueprint = cluster.getBlueprint();

        Map<String, String> result = new HashMap<>();
        List<Port> ports = NetworkUtils.getPorts(Optional.empty());
        try {
            JsonNode hostGroupsNode = blueprintValidator.getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = blueprintValidator.createHostGroupMap(hostGroups);
            for (JsonNode hostGroupNode : hostGroupsNode) {
                String hostGroupName = blueprintValidator.getHostGroupName(hostGroupNode);
                JsonNode componentsNode = blueprintValidator.getComponentsNode(hostGroupNode);
                HostGroup actualHostgroup = hostGroupMap.get(hostGroupName);
                String serviceAddress;
                if (actualHostgroup.getConstraint().getInstanceGroup() != null) {
                    InstanceMetaData next = actualHostgroup.getConstraint().getInstanceGroup().getNotTerminatedInstanceMetaDataSet().iterator().next();
                    serviceAddress = next.getPublicIpWrapper();
                } else {
                    serviceAddress = actualHostgroup.getHostMetadata().iterator().next().getHostName();
                }
                for (JsonNode componentNode : componentsNode) {
                    String componentName = componentNode.get("name").asText();
                    StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
                    collectServicePorts(result, ports, ambariIp, serviceAddress, componentDescriptor, cluster);
                }
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private void collectServicePorts(Map<String, String> result, Iterable<Port> ports, String ambariIp, String serviceAddress,
        StackServiceComponentDescriptor componentDescriptor, Cluster cluster) throws IOException {
        if (componentDescriptor != null && componentDescriptor.isMaster()) {
            List<String> exposedServices = new ArrayList<>();
            Gateway gateway = cluster.getGateway();
            if (gateway.getExposedServices() != null && gateway.getExposedServices().getValue() != null) {
                exposedServices = gateway.getExposedServices().get(ExposedServices.class).getServices();
            }

            for (Port port : ports) {
                collectServicePort(result, port, serviceAddress, ambariIp, componentDescriptor, exposedServices, gateway);
            }
        }
    }

    private void collectServicePort(Map<String, String> result, Port port, String serviceAddress, String ambariIp,
        StackServiceComponentDescriptor componentDescriptor, Collection<String> exposedServices, Gateway gateway) {
        if (port.getExposedService().getServiceName().equals(componentDescriptor.getName())) {
            if (gateway.getEnableGateway() && ambariIp != null) {
                String url;
                if (GatewayType.CENTRAL == gateway.getGatewayType()) {
                    url = String.format("/%s/%s%s", gateway.getPath(), gateway.getTopologyName(),
                        port.getExposedService().getKnoxUrl());
                } else {
                    url = String.format("https://%s:8443/%s/%s%s", ambariIp, gateway.getPath(), gateway.getTopologyName(),
                        port.getExposedService().getKnoxUrl());
                }
                // filter out what is not exposed
                // filter out what is not expected to be exposed e.g Zeppelin WS since it does not have Knox Url
                if (!Strings.isNullOrEmpty(port.getExposedService().getKnoxUrl())
                    && exposedServices.contains(port.getExposedService().getKnoxService())) {
                    result.put(port.getExposedService().getPortName(), url);
                }
            } else if (serviceAddress != null) {
                String url = String.format("http://%s:%s%s", serviceAddress, port.getPort(), port.getExposedService().getPostFix());
                result.put(port.getExposedService().getPortName(), url);
            }
        }
    }

    private String getAmbariServerUrl(Cluster cluster, String ambariIp) {
        String url;
        String orchestrator = cluster.getStack().getOrchestrator().getType();
        if (ambariIp != null) {
            Gateway gateway = cluster.getGateway();
            if (YARN.equals(orchestrator)) {
                url = String.format("http://%s:8080", ambariIp);
            } else {
                if (gateway.getEnableGateway() != null && gateway.getEnableGateway()) {
                    if (GatewayType.CENTRAL == gateway.getGatewayType()) {
                        url = String.format("/%s/%s/ambari/", gateway.getPath(), gateway.getTopologyName());
                    } else {
                        url = String.format("https://%s:8443/%s/%s/ambari/", ambariIp, gateway.getPath(), gateway.getTopologyName());
                    }
                } else {
                    url = String.format("https://%s/ambari/", ambariIp);
                }
            }
        } else {
            url = null;
        }
        return url;
    }
}