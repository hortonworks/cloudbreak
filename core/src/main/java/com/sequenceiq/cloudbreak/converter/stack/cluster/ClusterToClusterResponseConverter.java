package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;
import static com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil.anonymize;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsResponse;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.CustomContainerResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemResponse;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.api.model.v2.AttachedClusterInfoResponse;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
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

    @Inject
    private ProxyConfigMapper proxyConfigMapper;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackService stackService;

    @Inject
    private SecretService secretService;

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Override
    public ClusterResponse convert(Cluster source) {
        return doConvert(source);
    }

    private ClusterResponse doConvert(Cluster source) {
        ClusterResponse clusterResponse = new ClusterResponse();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        clusterResponse.setStatus(source.getStatus());
        clusterResponse.setStatusReason(source.getStatusReason());
        if (source.getBlueprint() != null) {
            clusterResponse.setBlueprintId(source.getBlueprint().getId());
        }
        setUptime(source, clusterResponse);
        Set<RDSConfig> rdsConfigs = source.getRdsConfigs();
        convertRdsIds(clusterResponse, rdsConfigs);
        String ambariIp = stackUtil.extractAmbariIp(source.getStack());
        clusterResponse.setAmbariServerIp(ambariIp);
        clusterResponse.setExecutorType(source.getExecutorType());
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        clusterResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        String ambariServerUrl = serviceEndpointCollector.getAmbariServerUrl(source, ambariIp);
        clusterResponse.setAmbariServerUrl(ambariServerUrl);
        Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesForTopologies =
                serviceEndpointCollector.prepareClusterExposedServices(source, ambariIp);
        clusterResponse.setClusterExposedServicesForTopologies(clusterExposedServicesForTopologies);
        clusterResponse.setConfigStrategy(source.getConfigStrategy());
        setExtendedBlueprintText(source, clusterResponse);
        convertRdsConfigs(source, clusterResponse);
        clusterResponse.setLdapConfig(getConversionService().convert(source.getLdapConfig(), LdapConfigResponse.class));
        clusterResponse.setBlueprint(getConversionService().convert(source.getBlueprint(), BlueprintResponse.class));
        convertCustomQueue(source, clusterResponse);
        convertNullableProperties(source, clusterResponse);
        convertContainerConfig(source, clusterResponse);
        convertComponentConfig(clusterResponse, source);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        convertKerberosConfig(source, clusterResponse);
        decorateResponseWithProxyConfig(source, clusterResponse);
        addFilesystem(source, clusterResponse);
        addSharedServiceResponse(source, clusterResponse);
        clusterResponse.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        return clusterResponse;
    }

    private void setUptime(Cluster source, ClusterResponse clusterResponse) {
        long uptime = stackUtil.getUptimeForCluster(source, source.isAvailable());
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);
    }

    private <R extends ClusterResponse> void setExtendedBlueprintText(Cluster source, R clusterResponse) {
        if (StringUtils.isNoneEmpty(source.getExtendedBlueprintText()) && !disableShowBlueprint) {
            String fromVault = secretService.get(source.getExtendedBlueprintText());
            clusterResponse.setExtendedBlueprintText(anonymize(fromVault));
        }
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

    private void convertCustomQueue(Cluster source, ClusterResponse clusterResponse) {
        if (source.getAttributes() != null) {
            Json fromVault = new Json(secretService.get(source.getAttributes()));
            Map<String, Object> attributes = fromVault.getMap();
            Object customQueue = attributes.get(CUSTOM_QUEUE.name());
            if (customQueue != null) {
                clusterResponse.setCustomQueue(customQueue.toString());
            } else {
                clusterResponse.setCustomQueue("default");
            }
        }
    }

    private void addFilesystem(Cluster source, ClusterResponse clusterResponse) {
        if (source.getFileSystem() != null) {
            clusterResponse.setFileSystemResponse(getConversionService().convert(source.getFileSystem(), FileSystemResponse.class));
        }
    }

    private void addSharedServiceResponse(Cluster cluster, ClusterResponse clusterResponse) {
        SharedServiceResponse sharedServiceResponse = new SharedServiceResponse();
        if (cluster.getStack().getDatalakeId() != null) {
            sharedServiceResponse.setSharedClusterId(cluster.getStack().getDatalakeId());
            sharedServiceResponse.setSharedClusterName(stackService.getByIdWithTransaction(cluster.getStack().getDatalakeId()).getName());
        } else {
            for (Stack stack : stackService.findClustersConnectedToDatalake(cluster.getStack().getId())) {
                AttachedClusterInfoResponse attachedClusterInfoResponse = new AttachedClusterInfoResponse();
                attachedClusterInfoResponse.setId(stack.getId());
                attachedClusterInfoResponse.setName(stack.getName());
                sharedServiceResponse.getAttachedClusters().add(attachedClusterInfoResponse);
            }
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    private void convertRdsIds(ClusterResponse clusterResponse, Collection<RDSConfig> rdsConfigs) {
        if (rdsConfigs != null && !rdsConfigs.isEmpty()) {
            for (RDSConfig rdsConfig : rdsConfigs) {
                clusterResponse.getRdsConfigIds().add(rdsConfig.getId());
            }
        }
    }

    private void convertRdsConfigs(Cluster source, ClusterResponse clusterResponse) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findUserManagedByClusterId(source.getId());
        for (RDSConfig rdsConfig : rdsConfigs) {
            clusterResponse.getRdsConfigs().add(getConversionService().convert(rdsConfig, RDSConfigResponse.class));
        }
    }

    private void convertNullableProperties(Cluster source, ClusterResponse clusterResponse) {
        if (source.getGateway() != null) {
            GatewayJson gatewayJson = getConversionService().convert(source.getGateway(), GatewayJson.class);
            clusterResponse.setGateway(gatewayJson);
        }
        if (source.getLdapConfig() != null) {
            clusterResponse.setLdapConfigId(source.getLdapConfig().getId());
        }
        if (source.getAttributes() != null) {
            Json fromVault = new Json(secretService.get(source.getAttributes()));
            clusterResponse.setAttributes(fromVault.getMap());
        }
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

    private void convertKerberosConfig(Cluster source, ClusterResponse clusterResponse) {
        KerberosConfig kerberosConfig = source.getKerberosConfig();
        if (source.isSecure() && kerberosConfig != null) {
            clusterResponse.setSecure(source.isSecure());
            clusterResponse.setKerberosResponse(getConversionService().convert(source.getKerberosConfig(), KerberosResponse.class));
        }
    }

    private void decorateResponseWithProxyConfig(Cluster source, ClusterResponse clusterResponse) {
        if (source.getProxyConfig() != null) {
            clusterResponse.setProxyName(source.getProxyConfig().getName());
        }
    }
}