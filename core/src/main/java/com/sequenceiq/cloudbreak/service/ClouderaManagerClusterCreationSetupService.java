package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class ClouderaManagerClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterCreationSetupService.class);

    @Inject
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Inject
    private DefaultCDHEntries defaultCDHEntries;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private BlueprintUtils blueprintUtils;

    public List<ClusterComponent> prepareClouderaManagerCluster(ClusterV4Request request, Cluster cluster,
            Optional<Component> stackClouderaManagerRepoConfig,
            List<Component> stackCdhRepoConfig,
            Optional<Component> stackImageComponent) throws IOException {
        List<ClusterComponent> components = new ArrayList<>();
        ClouderaManagerRepositoryV4Request repoDetailsJson = Optional.ofNullable(
                request.getCm()).map(ClouderaManagerV4Request::getRepository).orElse(null);
        ClusterComponent cmRepoConfig;
        if (repoDetailsJson == null) {
            cmRepoConfig = determineCmRepoConfig(stackClouderaManagerRepoConfig, stackImageComponent, cluster);
            components.add(cmRepoConfig);
        } else {
            cmRepoConfig = cluster.getComponents().stream().
                    filter(component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType())).findFirst().orElse(null);
        }
        List<ClouderaManagerProductV4Request> products = Optional.ofNullable(request.getCm()).map(ClouderaManagerV4Request::getProducts).orElse(null);
        if (products == null || products.isEmpty()) {
            List<ClusterComponent> cdhProductRepoConfig = determineCdhRepoConfig(cluster, stackCdhRepoConfig, stackImageComponent);
            components.addAll(cdhProductRepoConfig);
        }
        components.addAll(cluster.getComponents());
        checkCmStackRepositories(cmRepoConfig, stackImageComponent.get());
        return components;
    }

    private void checkCmStackRepositories(ClusterComponent cmRepoConfig, Component imageComponent)
            throws IOException {
        if (Objects.nonNull(cmRepoConfig)) {
            ClouderaManagerRepo clouderaManagerRepo = cmRepoConfig.getAttributes().get(ClouderaManagerRepo.class);
            Image image = imageComponent.getAttributes().get(Image.class);
            StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();
            Map<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorMap = stackMatrixV4Response.getCdh();
            ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptor = stackDescriptorMap.get(clouderaManagerRepo.getVersion());
            if (clouderaManagerStackDescriptor != null) {
                boolean hasDefaultStackRepoUrlForOsType = clouderaManagerStackDescriptor.getRepository().getStack().containsKey(image.getOsType());
                boolean hasDefaultCmRepoUrlForOsType = clouderaManagerStackDescriptor.getClouderaManager().getRepository().containsKey(image.getOsType());
                boolean compatibleClusterManager = new VersionComparator().compare(() -> clouderaManagerStackDescriptor.getVersion().
                                substring(0, clouderaManagerStackDescriptor.getMinCM().length()),
                        clouderaManagerStackDescriptor::getMinCM) >= 0;
                if (!hasDefaultCmRepoUrlForOsType || !hasDefaultStackRepoUrlForOsType || !compatibleClusterManager) {
                    String message = String.format("The given repository information seems to be incompatible."
                            + " CM version: %s, Image Id: %s, Os type: %s.", clouderaManagerRepo.getVersion(), image.getImageId(), image.getOsType());
                    LOGGER.warn(message);
                }
            }
        }
    }

    private ClusterComponent determineCmRepoConfig(Optional<Component> stackClouderaManagerRepoConfig,
            Optional<Component> stackImageComponent, Cluster cluster) throws IOException {
        Json json;
        if (Objects.isNull(stackClouderaManagerRepoConfig) || !stackClouderaManagerRepoConfig.isPresent()) {
            JsonNode root = JsonUtil.readTree(cluster.getBlueprint().getBlueprintText());
            String cdhStackVersion = blueprintUtils.getCDHStackVersion(root);
            String cdh = StackType.CDH.name();
            ClouderaManagerRepo clouderaManagerRepo = defaultClouderaManagerRepoService.getDefault(
                    getOsType(stackImageComponent), cdh, cdhStackVersion);
            if (clouderaManagerRepo == null) {
                throw new BadRequestException(String.format("Couldn't determine Cloudera Manager repo for the stack: %s", cluster.getStack().getName()));
            }
            json = new Json(clouderaManagerRepo);
        } else {
            json = stackClouderaManagerRepoConfig.get().getAttributes();
        }
        return new ClusterComponent(ComponentType.CM_REPO_DETAILS, json, cluster);
    }

    private List<ClusterComponent> determineCdhRepoConfig(Cluster cluster, List<Component> stackCdhRepoConfig,
            Optional<Component> stackImageComponent) throws IOException {
        if (Objects.isNull(stackCdhRepoConfig) || stackCdhRepoConfig.isEmpty()) {
            String osType = getOsType(stackImageComponent);

            Map.Entry<String, DefaultCDHInfo> defaultCDHInfoEntry = defaultCDHEntries.
                    getEntries().
                    entrySet().
                    stream().
                    filter(entry -> Objects.nonNull(entry.getValue().getRepo().getStack().get(osType))).
                    max(DefaultCDHEntries.CDH_ENTRY_COMPARATOR).
                    orElseThrow(notFound("Default Product Info with OS type:", osType));
            ClouderaManagerProduct cmProduct = new ClouderaManagerProduct();
            DefaultCDHInfo defaultCDHInfo = defaultCDHInfoEntry.getValue();
            Map<String, String> stack = defaultCDHInfo.getRepo().getStack();
            cmProduct.
                    withVersion(defaultCDHInfo.getVersion()).
                    withName(stack.get("repoid").split("-")[0]).
                    withParcel(stack.get(osType));
            ClusterComponent product = new ClusterComponent(ComponentType.CDH_PRODUCT_DETAILS, new Json(cmProduct), cluster);
            List<ClusterComponent> productList = new ArrayList<>();
            productList.add(product);
            return productList;
        } else {
            return stackCdhRepoConfig.stream().
                    map(Component::getAttributes).
                    map(json -> new ClusterComponent(ComponentType.CDH_PRODUCT_DETAILS, json, cluster)).
                    collect(Collectors.toList());
        }
    }

    private String getOsType(Optional<Component> stackImageComponent) throws IOException {
        if (Objects.nonNull(stackImageComponent) &&  stackImageComponent.isPresent()) {
            Image image = stackImageComponent.get().getAttributes().get(Image.class);
            return image.getOsType();
        } else {
            return "";
        }
    }
}