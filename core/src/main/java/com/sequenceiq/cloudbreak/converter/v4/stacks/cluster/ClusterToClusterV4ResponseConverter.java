package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClusterToClouderaManagerV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;

@Component
public class ClusterToClusterV4ResponseConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterV4ResponseConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    @Override
    public ClusterV4Response convert(Cluster source) {
        ClusterV4Response clusterResponse = new ClusterV4Response();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        clusterResponse.setStatus(source.getStatus());
        clusterResponse.setStatusReason(source.getStatusReason());
        setUptime(source, clusterResponse);
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        String managerAddress = stackUtil.extractClusterManagerAddress(source.getStack());
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies =
                    serviceEndpointCollector.prepareClusterExposedServices(source, managerAddress);
        clusterResponse.setExposedServices(clusterExposedServicesForTopologies);
        convertCustomQueue(source, clusterResponse);
        convertNullableProperties(source, clusterResponse);
        convertContainerConfig(source, clusterResponse);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        decorateResponseWithProxyConfig(source, clusterResponse);
        clusterResponse.setCloudStorage(getCloudStorage(source));
        clusterResponse.setCm(ClusterToClouderaManagerV4ResponseConverter.convert(source));
        clusterResponse.setDatabases(converterUtil.convertAll(source.getRdsConfigs().stream().filter(
                rds -> ResourceStatus.USER_MANAGED.equals(rds.getStatus())).collect(Collectors.toList()), DatabaseV4Response.class));
        clusterResponse.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        clusterResponse.setBlueprint(getConversionService().convert(source.getBlueprint(), BlueprintV4Response.class));
        clusterResponse.setExtendedBlueprintText(getExtendedBlueprintText(source));
        clusterResponse.setCustomConfigsName(getIfNotNull(source.getCustomConfigs(), configs -> configs.getName()));
        convertDpSecrets(source, clusterResponse);
        clusterResponse.setServerIp(stackUtil.extractClusterManagerIp(source.getStack()));
        clusterResponse.setServerFqdn(source.getFqdn());
        clusterResponse.setServerUrl(serviceEndpointCollector.getManagerServerUrl(source, managerAddress));
        clusterResponse.setDatabaseServerCrn(source.getDatabaseServerCrn());
        clusterResponse.setRangerRazEnabled(source.isRangerRazEnabled());
        clusterResponse.setCertExpirationState(source.getCertExpirationState());
        return clusterResponse;
    }

    private void setUptime(Cluster source, ClusterV4Response clusterResponse) {
        long uptime = stackUtil.getUptimeForCluster(source, source.isAvailable());
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);
    }

    private void convertCustomQueue(Cluster source, ClusterV4Response clusterResponse) {
        if (source.getAttributes() != null) {
            Json fromVault = new Json(source.getAttributes());
            Map<String, Object> attributes = fromVault.getMap();
            Object customQueue = attributes.get(CUSTOM_QUEUE.name());
            if (customQueue != null) {
                clusterResponse.setCustomQueue(customQueue.toString());
            } else {
                clusterResponse.setCustomQueue("default");
            }
        }
    }

    private String getExtendedBlueprintText(Cluster source) {
        if (StringUtils.isNotEmpty(source.getExtendedBlueprintText()) && !disableShowBlueprint) {
            String fromVault = source.getExtendedBlueprintText();
            return anonymize(fromVault);
        }
        return null;
    }

    private CloudStorageResponse getCloudStorage(Cluster source) {
        if (source.getFileSystem() != null) {
            CloudStorageResponse cloudStorageResponse = cloudStorageConverter.fileSystemToResponse(source.getFileSystem());

            if (source.getAdditionalFileSystem() != null) {
                AwsEfsParameters efsParameters = cloudStorageConverter.fileSystemToEfsParameters(source.getAdditionalFileSystem());

                if (efsParameters != null) {
                    if (cloudStorageResponse.getAws() == null) {
                        cloudStorageResponse.setAws(new AwsStorageParameters());
                    }

                    cloudStorageResponse.getAws().setEfsParameters(efsParameters);
                }
            }

            return cloudStorageResponse;
        }
        return null;
    }

    private void convertNullableProperties(Cluster source, ClusterV4Response clusterResponse) {
        if (source.getGateway() != null) {
            GatewayV4Response gatewayV4Response = getConversionService().convert(source.getGateway(), GatewayV4Response.class);
            clusterResponse.setGateway(gatewayV4Response);
        }
        if (source.getAttributes() != null) {
            Json fromVault = new Json(source.getAttributes());
            clusterResponse.setAttributes(fromVault.getMap());
        }
    }

    private void convertContainerConfig(Cluster source, ClusterV4Response clusterResponse) {
        Json customContainerDefinition = source.getCustomContainerDefinition();
        if (customContainerDefinition != null && StringUtils.isNotEmpty(customContainerDefinition.getValue())) {
            try {
                Map<String, String> map = customContainerDefinition.get(Map.class);
                Map<String, String> result = new HashMap<>();

                for (Entry<String, String> stringStringEntry : map.entrySet()) {
                    result.put(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
                CustomContainerV4Response customContainers = new CustomContainerV4Response();
                customContainers.setDefinitions(result);
                clusterResponse.setCustomContainers(customContainers);

            } catch (IOException e) {
                LOGGER.info("Failed to add customContainerDefinition to response", e);
                throw new CloudbreakApiException("Failed to add customContainerDefinition to response", e);
            }
        }
    }

    private void decorateResponseWithProxyConfig(Cluster source, ClusterV4Response clusterResponse) {
        String proxyConfigCrn = source.getProxyConfigCrn();
        if (StringUtils.isNotEmpty(proxyConfigCrn)) {
            clusterResponse.setProxyConfigCrn(proxyConfigCrn);
            clusterResponse.setProxyConfigName(proxyConfigDtoService.getByCrn(proxyConfigCrn).getName());
        }
    }

    private void convertDpSecrets(Cluster source, ClusterV4Response response) {
        if (isNotEmpty(source.getDpAmbariUserSecret()) && isNotEmpty(source.getDpAmbariPasswordSecret())) {
            response.setCmMgmtUser(getConversionService().convert(source.getDpAmbariUserSecret(), SecretResponse.class));
            response.setCmMgmtPassword(getConversionService().convert(source.getDpAmbariPasswordSecret(), SecretResponse.class));
        }
    }
}
