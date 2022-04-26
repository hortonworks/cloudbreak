package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;
import static com.sequenceiq.cloudbreak.domain.ClusterAttributes.CUSTOM_QUEUE;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.StackDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintToBlueprintV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClusterDtoToClouderaManagerV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayToGatewayV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollectorV2;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.stack.ClusterDto;
import com.sequenceiq.cloudbreak.service.stack.StackProxy;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;

@Component
public class ClusterDtoToClusterV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDtoToClusterV4ResponseConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ServiceEndpointCollectorV2 serviceEndpointCollector;

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @Inject
    private BlueprintToBlueprintV4ResponseConverter blueprintToBlueprintV4ResponseConverter;

    @Inject
    private GatewayToGatewayV4ResponseConverter gatewayToGatewayV4ResponseConverter;

    @Inject
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Inject
    private RDSConfigToDatabaseV4ResponseConverter rdsConfigToDatabaseV4ResponseConverter;

    @Inject
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Inject
    private ClusterDtoToClouderaManagerV4ResponseConverter clusterDtoToClouderaManagerV4ResponseConverter;

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    public ClusterV4Response convert(StackProxy stackProxy) {
        ClusterDto source = stackProxy.getCluster();
        StackDto stack = stackProxy.getStack();
        ClusterV4Response clusterResponse = new ClusterV4Response();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        setUptime(source, stack, clusterResponse);
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        String managerAddress = stackUtil.extractClusterManagerAddress(source, stack.getId());
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies =
                    serviceEndpointCollector.prepareClusterExposedServices(stackProxy, managerAddress);
        clusterResponse.setExposedServices(clusterExposedServicesForTopologies);
        convertCustomQueue(source, clusterResponse);
        convertNullableProperties(stackProxy, clusterResponse);
        convertContainerConfig(source, clusterResponse);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        decorateResponseWithProxyConfig(source, clusterResponse);
        clusterResponse.setCloudStorage(getCloudStorage(stackProxy));
        clusterResponse.setCm(clusterDtoToClouderaManagerV4ResponseConverter.convert(stackProxy));
        clusterResponse.setDatabases(
                rdsConfigWithoutClusterService.findByClusterIdAndStatusIn(source.getId(), Set.of(ResourceStatus.USER_MANAGED))
                .stream()
                .map(rds -> rdsConfigToDatabaseV4ResponseConverter.convert(rds))
                .collect(Collectors.toList())
        );
        clusterResponse.setWorkspace(workspaceToWorkspaceResourceV4ResponseConverter.convert(stackProxy.getWorkspace()));
        clusterResponse.setBlueprint(getIfNotNull(stackProxy.getBlueprint(), blueprintToBlueprintV4ResponseConverter::convert));
        clusterResponse.setExtendedBlueprintText(getExtendedBlueprintText(source));
        convertDpSecrets(source, clusterResponse);
        clusterResponse.setServerIp(stackUtil.extractClusterManagerIp(source, stack.getId()));
        clusterResponse.setServerFqdn(source.getFqdn());
        clusterResponse.setServerUrl(serviceEndpointCollector.getManagerServerUrl(stackProxy, managerAddress));
        clusterResponse.setCustomConfigurationsName(getIfNotNull(source.getCustomConfigurations(), configurations -> configurations.getName()));
        clusterResponse.setCustomConfigurationsCrn(getIfNotNull(source.getCustomConfigurations(), configurations -> configurations.getCrn()));
        clusterResponse.setDatabaseServerCrn(source.getDatabaseServerCrn());
        clusterResponse.setRangerRazEnabled(source.isRangerRazEnabled());
        clusterResponse.setCertExpirationState(source.getCertExpirationState());
        return clusterResponse;
    }

    private void setUptime(ClusterDto source, StackDto stackDto, ClusterV4Response clusterResponse) {
        long uptime = stackUtil.getUptimeForCluster(source, stackDto.getStackStatus() == Status.AVAILABLE);
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);
    }

    private void convertCustomQueue(ClusterDto source, ClusterV4Response clusterResponse) {
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

    private String getExtendedBlueprintText(ClusterDto source) {
        if (StringUtils.isNotEmpty(source.getExtendedBlueprintText().getRaw()) && !disableShowBlueprint) {
            String fromVault = source.getExtendedBlueprintText().getRaw();
            return anonymize(fromVault);
        }
        return null;
    }

    private CloudStorageResponse getCloudStorage(StackProxy stackProxy) {
        if (stackProxy.getFileSystem() != null) {
            CloudStorageResponse cloudStorageResponse = cloudStorageConverter.fileSystemToResponse(stackProxy.getFileSystem());

            if (stackProxy.getAdditionalFileSystem() != null) {
                AwsEfsParameters efsParameters = cloudStorageConverter.fileSystemToEfsParameters(stackProxy.getAdditionalFileSystem());

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

    private void convertNullableProperties(StackProxy stackProxy, ClusterV4Response clusterResponse) {
        if (stackProxy.getGateway() != null) {
            GatewayV4Response gatewayV4Response = gatewayToGatewayV4ResponseConverter.convert(stackProxy.getGateway());
            clusterResponse.setGateway(gatewayV4Response);
        }
        if (stackProxy.getCluster().getAttributes() != null) {
            Json fromVault = new Json(stackProxy.getCluster().getAttributes());
            clusterResponse.setAttributes(fromVault.getMap());
        }
    }

    private void convertContainerConfig(ClusterDto source, ClusterV4Response clusterResponse) {
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

    private void decorateResponseWithProxyConfig(ClusterDto source, ClusterV4Response clusterResponse) {
        String proxyConfigCrn = source.getProxyConfigCrn();
        if (StringUtils.isNotEmpty(proxyConfigCrn)) {
            clusterResponse.setProxyConfigCrn(proxyConfigCrn);
            clusterResponse.setProxyConfigName(proxyConfigDtoService.getByCrn(proxyConfigCrn).getName());
        }
    }

    private void convertDpSecrets(ClusterDto source, ClusterV4Response response) {
        if (isNotEmpty(source.getDpAmbariUser().getSecret()) && isNotEmpty(source.getDpAmbariPassword().getSecret())) {
            response.setCmMgmtUser(stringToSecretResponseConverter.convert(source.getDpAmbariUser().getSecret()));
            response.setCmMgmtPassword(stringToSecretResponseConverter.convert(source.getDpAmbariPassword().getSecret()));
        }
    }
}
