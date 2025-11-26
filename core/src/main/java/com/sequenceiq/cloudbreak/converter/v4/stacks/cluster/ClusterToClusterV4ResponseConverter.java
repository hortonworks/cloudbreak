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

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.customcontainer.CustomContainerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintToBlueprintV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClusterToClouderaManagerV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayToGatewayV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;

@Component
public class ClusterToClusterV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterToClusterV4ResponseConverter.class);

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_SECOND = 1000;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

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
    private ClusterToClouderaManagerV4ResponseConverter clusterToClouderaManagerV4ResponseConverter;

    @Value("${cb.disable.show.blueprint:false}")
    private boolean disableShowBlueprint;

    public ClusterV4Response convert(StackDtoDelegate stackDto) {
        ClusterView source = stackDto.getCluster();
        ClusterV4Response clusterResponse = new ClusterV4Response();
        clusterResponse.setId(source.getId());
        clusterResponse.setName(source.getName());
        StackView stack = stackDto.getStack();
        clusterResponse.setStatus(stack.getStatus());
        clusterResponse.setStatusReason(stack.getStatusReason());
        setUptime(source, stack, clusterResponse);
        clusterResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        String managerAddress = stackUtil.extractClusterManagerAddress(stackDto);
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies =
                serviceEndpointCollector.prepareClusterExposedServices(stackDto, managerAddress);
        clusterResponse.setExposedServices(clusterExposedServicesForTopologies);
        convertCustomQueue(source, clusterResponse);
        convertNullableProperties(stackDto, clusterResponse);
        convertContainerConfig(source, clusterResponse);
        clusterResponse.setCreationFinished(source.getCreationFinished());
        decorateResponseWithProxyConfig(source, clusterResponse);
        clusterResponse.setCloudStorage(getCloudStorage(source));
        clusterResponse.setCm(clusterToClouderaManagerV4ResponseConverter.convert(stackDto));
        WorkspaceResourceV4Response workspaceResponse = workspaceToWorkspaceResourceV4ResponseConverter.convert(stackDto.getWorkspace());
        clusterResponse.setDatabases(
                rdsConfigWithoutClusterService.findByClusterIdAndStatusIn(source.getId(), Set.of(ResourceStatus.USER_MANAGED))
                        .stream()
                        .map(rds -> rdsConfigToDatabaseV4ResponseConverter.convert(rds, workspaceResponse))
                        .collect(Collectors.toList())
        );
        clusterResponse.setWorkspace(workspaceResponse);
        clusterResponse.setBlueprint(getIfNotNull(stackDto.getBlueprint(), blueprintToBlueprintV4ResponseConverter::convert));
        clusterResponse.setExtendedBlueprintText(getExtendedBlueprintText(source));
        convertDpSecrets(source, clusterResponse);
        clusterResponse.setServerIp(stackUtil.extractClusterManagerIp(stackDto));
        clusterResponse.setServerFqdn(source.getFqdn());
        clusterResponse.setServerUrl(serviceEndpointCollector.getManagerServerUrl(stackDto, managerAddress));
        clusterResponse.setCustomConfigurationsName(getIfNotNull(source.getCustomConfigurations(), configurations -> configurations.getName()));
        clusterResponse.setCustomConfigurationsCrn(getIfNotNull(source.getCustomConfigurations(), configurations -> configurations.getCrn()));
        clusterResponse.setDatabaseServerCrn(source.getDatabaseServerCrn());
        clusterResponse.setRangerRazEnabled(source.isRangerRazEnabled());
        clusterResponse.setRangerRmsEnabled(source.isRangerRmsEnabled());
        clusterResponse.setCertExpirationState(source.getCertExpirationState());
        clusterResponse.setDbSSLEnabled(source.getDbSslEnabled() != null && source.getDbSslEnabled());
        clusterResponse.setDbSslRootCertBundle(source.getDbSslRootCertBundle());
        clusterResponse.setCertExpirationDetails(source.getCertExpirationDetails());
        clusterResponse.setEncryptionProfileCrn(source.getEncryptionProfileCrn());
        return clusterResponse;
    }

    private void setUptime(ClusterView source, StackView stackView, ClusterV4Response clusterResponse) {
        long uptime = stackUtil.getUptimeForCluster(source, stackView.getStatus() == Status.AVAILABLE);
        int minutes = (int) ((uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        int hours = (int) (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        clusterResponse.setUptime(uptime);
        clusterResponse.setHoursUp(hours);
        clusterResponse.setMinutesUp(minutes);
    }

    private void convertCustomQueue(ClusterView source, ClusterV4Response clusterResponse) {
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

    private String getExtendedBlueprintText(ClusterView source) {
        if (StringUtils.isNotEmpty(source.getExtendedBlueprintText()) && !disableShowBlueprint) {
            String fromVault = source.getExtendedBlueprintText();
            return anonymize(fromVault);
        }
        return null;
    }

    private CloudStorageResponse getCloudStorage(ClusterView clusterView) {
        if (clusterView.getFileSystem() != null) {
            CloudStorageResponse cloudStorageResponse = cloudStorageConverter.fileSystemToResponse(clusterView.getFileSystem());

            if (clusterView.getAdditionalFileSystem() != null) {
                AwsEfsParameters efsParameters = cloudStorageConverter.fileSystemToEfsParameters(clusterView.getAdditionalFileSystem());

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

    private void convertNullableProperties(StackDtoDelegate stackDto, ClusterV4Response clusterResponse) {
        if (stackDto.getGateway() != null) {
            GatewayV4Response gatewayV4Response = gatewayToGatewayV4ResponseConverter.convert(stackDto.getGateway());
            clusterResponse.setGateway(gatewayV4Response);
        }
        if (stackDto.getCluster().getAttributes() != null) {
            Json fromVault = new Json(stackDto.getCluster().getAttributes());
            clusterResponse.setAttributes(fromVault.getMap());
        }
    }

    private void convertContainerConfig(ClusterView source, ClusterV4Response clusterResponse) {
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
                throw new CloudbreakServiceException("Failed to add customContainerDefinition to response", e);
            }
        }
    }

    private void decorateResponseWithProxyConfig(ClusterView source, ClusterV4Response clusterResponse) {
        String proxyConfigCrn = source.getProxyConfigCrn();
        if (StringUtils.isNotEmpty(proxyConfigCrn)) {
            clusterResponse.setProxyConfigCrn(proxyConfigCrn);
            clusterResponse.setProxyConfigName(proxyConfigDtoService.getByCrn(proxyConfigCrn).getName());
        }
    }

    private void convertDpSecrets(ClusterView source, ClusterV4Response response) {
        if (isNotEmpty(source.getDpClusterManagerUserSecretPath()) && isNotEmpty(source.getDpClusterManagerPasswordSecretPath())) {
            response.setCmMgmtUser(stringToSecretResponseConverter.convert(source.getDpClusterManagerUserSecretPath()));
            response.setCmMgmtPassword(stringToSecretResponseConverter.convert(source.getDpClusterManagerPasswordSecretPath()));
        }
    }
}
