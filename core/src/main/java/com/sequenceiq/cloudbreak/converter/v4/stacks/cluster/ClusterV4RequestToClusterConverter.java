package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.customcontainer.CustomContainerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.ClusterAttributes;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class ClusterV4RequestToClusterConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV4RequestToClusterConverter.class);

    @Inject
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CustomConfigurationsService customConfigurationsService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    @Inject
    private GatewayV4RequestToGatewayConverter gatewayV4RequestToGatewayConverter;

    public Cluster convert(ClusterV4Request source) {
        Workspace workspace = workspaceService.getForCurrentUser();
        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(REQUESTED);
        cluster.setDatabaseServerCrn(source.getDatabaseServerCrn());
        cluster.setBlueprint(getBlueprint(source.getBlueprintName(), workspace));
        cluster.setCustomConfigurations(getCustomConfigurations(source.getCustomConfigurationsName()));
        convertGateway(source, cluster);
        if (cloudStorageValidationUtil.isCloudStorageConfigured(source.getCloudStorage())) {
            FileSystem fileSystem = cloudStorageConverter.requestToFileSystem(source.getCloudStorage());
            cluster.setFileSystem(fileSystem);
        }
        convertAttributes(source, cluster);
        try {
            Json json = new Json(convertContainerConfigs(source.getCustomContainer()));
            cluster.setCustomContainerDefinition(json);
        } catch (IllegalArgumentException ignored) {
            cluster.setCustomContainerDefinition(null);
        }
        updateDatabases(source, cluster, workspace);
        extractClusterManagerAndCmRepoConfig(cluster, source);
        cluster.setProxyConfigCrn(source.getProxyConfigCrn());
        cluster.setRangerRazEnabled(source.isRangerRazEnabled());
        cluster.setRangerRmsEnabled(source.isRangerRmsEnabled());
        return cluster;
    }

    private void convertGateway(ClusterV4Request source, Cluster cluster) {
        GatewayV4Request gatewayRequest = source.getGateway();
        if (gatewayRequest != null) {
            if (StringUtils.isEmpty(gatewayRequest.getPath())) {
                gatewayRequest.setPath(source.getName());
            }
            Gateway gateway = gatewayV4RequestToGatewayConverter.convert(gatewayRequest);
            if (gateway != null) {
                cluster.setGateway(gateway);
                gateway.setCluster(cluster);
            }
        }
    }

    private void convertAttributes(ClusterV4Request source, Cluster cluster) {
        Map<String, Object> attributesMap = source.getCustomQueue() != null
                ? Collections.singletonMap(ClusterAttributes.CUSTOM_QUEUE.name(), source.getCustomQueue())
                : Collections.emptyMap();
        try {
            cluster.setAttributes((new Json(attributesMap)).getValue());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not initiate the attribute map on cluster object: ", e);
            throw new CloudbreakApiException("Failed to store exposedServices", e);
        }
    }

    private Map<String, String> convertContainerConfigs(CustomContainerV4Request customContainerRequest) {
        Map<String, String> configs = new HashMap<>();
        if (customContainerRequest != null) {
            configs.putAll(customContainerRequest.getDefinitions());
        }
        return configs;
    }

    private void extractClusterManagerAndCmRepoConfig(Cluster cluster, ClusterV4Request clusterRequest) {
        Set<ClusterComponent> components = new HashSet<>();

        ClouderaManagerV4Request clouderaManagerRequest = clusterRequest.getCm();
        if (Objects.nonNull(clouderaManagerRequest) && cluster.getBlueprint() != null && !StackType.CDH.name().equals(cluster.getBlueprint().getStackType())) {
            throw new BadRequestException("Cannot process the provided Ambari blueprint with Cloudera Manager");
        }

        Optional.ofNullable(clouderaManagerRequest)
                .map(ClouderaManagerV4Request::getRepository)
                .map(ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter::convert)
                .map(toJsonWrapException())
                .map(cmRepoJson -> new ClusterComponent(ComponentType.CM_REPO_DETAILS, cmRepoJson, cluster))
                .ifPresent(components::add);

        Optional.ofNullable(clouderaManagerRequest)
                .map(ClouderaManagerV4Request::getProducts)
                .orElseGet(List::of)
                .stream()
                .map(this::convertCMProductRequestToCMProduct)
                .map(product -> {
                    Json json = toJsonWrapException().apply(product);
                    return new ClusterComponent(cdhProductDetails(), product.getName(), json, cluster);
                })
                .forEach(components::add);
        cluster.setComponents(components);
    }

    private ClouderaManagerProduct convertCMProductRequestToCMProduct(ClouderaManagerProductV4Request request) {
        if (StringUtils.isEmpty(request.getName())) {
            throw new BadRequestException("Name of the ClouderaManagerProduct cannot be empty.");
        }
        return new ClouderaManagerProduct()
                .withName(request.getName())
                .withParcel(request.getParcel())
                .withVersion(request.getVersion())
                .withCsd(request.getCsd());
    }

    private <T> Function<T, Json> toJsonWrapException() {
        return repositoryObject -> {
            try {
                return new Json(repositoryObject);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Cannot serialize the stack template", e);
            }
        };
    }

    private Blueprint getBlueprint(String blueprintName, Workspace workspace) {
        Blueprint blueprint = null;
        if (!StringUtils.isEmpty(blueprintName)) {
            blueprint = blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace);
            if (blueprint == null) {
                throw new NotFoundException("Cluster definition does not exist by name: " + blueprintName);
            }
        }
        return blueprint;
    }

    private CustomConfigurations getCustomConfigurations(String customConfigurationsName) {
        CustomConfigurations customConfigurations = null;
        if (!StringUtils.isEmpty(customConfigurationsName)) {
            customConfigurations = customConfigurationsService.getByNameOrCrn(NameOrCrn.ofName(customConfigurationsName));
        }
        return customConfigurations;
    }

    private void updateDatabases(ClusterV4Request source, Cluster cluster, Workspace workspace) {
        Set<String> databases = source.getDatabases();
        if (!CollectionUtils.isEmpty(databases)) {
            Set<RDSConfig> rdsConfigs = rdsConfigService.findByNamesInWorkspace(databases, workspace.getId());
            if (rdsConfigs.isEmpty()) {
                throw new NotFoundException("RDS config names do not exist");
            }
            cluster.setRdsConfigs(rdsConfigs);
        }
    }
}
