package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ClouderaManagerStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.ImageBasedDefaultCDHInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelFilterService;

@Service
public class ClouderaManagerClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterCreationSetupService.class);

    private static final String CDH = "CDH";

    @Inject
    private DefaultClouderaManagerRepoService defaultClouderaManagerRepoService;

    @Inject
    private ImageBasedDefaultCDHEntries imageBasedDefaultCDHEntries;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ParcelFilterService parcelFilterService;

    public List<ClusterComponent> prepareClouderaManagerCluster(ClusterV4Request request, Cluster cluster,
            Optional<Component> stackClouderaManagerRepoConfig,
            List<Component> stackCdhRepoConfig,
            Optional<Component> stackImageComponent) throws IOException, CloudbreakImageCatalogException {
        List<ClusterComponent> components = new ArrayList<>();
        long start = System.currentTimeMillis();
        String blueprintCdhVersion = blueprintUtils.getCDHStackVersion(JsonUtil.readTree(cluster.getBlueprint().getBlueprintText()));
        LOGGER.debug("blueprintUtils.getCDHStackVersion took {} ms", System.currentTimeMillis() - start);

        Optional<ClouderaManagerRepositoryV4Request> cmRepoRequest = measure(() ->
                Optional.ofNullable(request.getCm()).map(ClouderaManagerV4Request::getRepository),
                LOGGER,
                "ClouderaManagerV4Request::getRepository {} ms");

        start = System.currentTimeMillis();
        String osType = getOsType(stackImageComponent);
        String imageCatalogName = getImageCatalogName(stackImageComponent);
        LOGGER.debug("getImageCatalogName took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        ClusterComponent cmRepoConfig = getCmRepoConfiguration(cluster, stackClouderaManagerRepoConfig, components, blueprintCdhVersion, cmRepoRequest, osType);
        LOGGER.debug("getCmRepoConfiguration took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        checkCmStackRepositories(cmRepoConfig, stackImageComponent.get());
        LOGGER.debug("checkCmStackRepositories took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        addProductComponentsToCluster(request, cluster, stackCdhRepoConfig, components, blueprintCdhVersion, osType, imageCatalogName);
        LOGGER.debug("addProductComponentsToCluster took {} ms", System.currentTimeMillis() - start);

        return components;
    }

    private String getOsType(Optional<Component> stackImageComponent) throws IOException {
        if (Objects.nonNull(stackImageComponent) && stackImageComponent.isPresent()) {
            Image image = stackImageComponent.get().getAttributes().get(Image.class);
            return image.getOsType();
        } else {
            return "";
        }
    }

    private String getImageCatalogName(Optional<Component> stackImageComponent) throws IOException {
        if (Objects.nonNull(stackImageComponent) && stackImageComponent.isPresent()) {
            Image image = stackImageComponent.get().getAttributes().get(Image.class);
            return image.getImageCatalogName();
        } else {
            return null;
        }
    }

    private ClusterComponent getCmRepoConfiguration(Cluster cluster, Optional<Component> stackClouderaManagerRepoConfig,
            List<ClusterComponent> components, String blueprintCdhVersion, Optional<ClouderaManagerRepositoryV4Request> cmRepoRequest, String osType)
    throws CloudbreakImageCatalogException {
        ClusterComponent cmRepoConfig;
        if (cmRepoRequest.isEmpty()) {
            cmRepoConfig = determineCmRepoConfig(stackClouderaManagerRepoConfig, osType, cluster, blueprintCdhVersion);
            components.add(cmRepoConfig);
        } else {
            cmRepoConfig = cluster.getComponents().stream().
                    filter(component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType())).findFirst().orElse(null);
        }
        return cmRepoConfig;
    }

    private void addProductComponentsToCluster(ClusterV4Request request, Cluster cluster, List<Component> stackCdhRepoConfig,
            List<ClusterComponent> components, String blueprintCdhVersion, String osType, String imageCatalogName) throws CloudbreakImageCatalogException {
        List<ClouderaManagerProductV4Request> products = Optional.ofNullable(request.getCm()).map(ClouderaManagerV4Request::getProducts)
                .orElse(Collections.emptyList());
        if (products.isEmpty()) {
            Set<ClouderaManagerProduct> filteredProducts = determineCdhRepoConfig(cluster, stackCdhRepoConfig, osType,
                    blueprintCdhVersion, imageCatalogName);
            components.addAll(convertParcelsToClusterComponents(cluster, filteredProducts));
        }
        if (isCdhParcelMissing(products, components)) {
            throw new BadRequestException("CDH parcel is missing from the cluster. "
                    + "If parcels are provided in the request, CDH parcel must be specified too.");
        }
        components.addAll(cluster.getComponents());
    }

    private List<ClusterComponent> convertParcelsToClusterComponents(Cluster cluster, Set<ClouderaManagerProduct> filteredProducts) {
        return filteredProducts.stream()
                .map(product -> new ClusterComponent(CDH_PRODUCT_DETAILS, product.getName(), new Json(product), cluster))
                .collect(Collectors.toList());
    }

    private void checkCmStackRepositories(ClusterComponent cmRepoConfig, Component imageComponent) throws IOException, CloudbreakImageCatalogException {
        if (Objects.nonNull(cmRepoConfig)) {
            ClouderaManagerRepo clouderaManagerRepo = cmRepoConfig.getAttributes().get(ClouderaManagerRepo.class);
            Image image = imageComponent.getAttributes().get(Image.class);
            StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix(
                    cmRepoConfig.getCluster().getWorkspace().getId(),
                    cmRepoConfig.getCluster().getStack().getCloudPlatform(),
                    image.getImageCatalogName());
            Map<String, ClouderaManagerStackDescriptorV4Response> stackDescriptorMap = stackMatrixV4Response.getCdh();
            ClouderaManagerStackDescriptorV4Response clouderaManagerStackDescriptor = stackDescriptorMap.get(clouderaManagerRepo.getVersion());
            if (clouderaManagerStackDescriptor != null) {
                boolean hasDefaultStackRepoUrlForOsType = clouderaManagerStackDescriptor.getRepository().getStack().containsKey(image.getOsType());
                boolean hasDefaultCmRepoUrlForOsType = clouderaManagerStackDescriptor.getClouderaManager().getRepository().containsKey(image.getOsType());
                if (!hasDefaultCmRepoUrlForOsType || !hasDefaultStackRepoUrlForOsType) {
                    String message = String.format("The given repository information seems to be incompatible."
                            + " CM version: %s, Image Id: %s, Os type: %s.", clouderaManagerRepo.getVersion(), image.getImageId(), image.getOsType());
                    LOGGER.warn(message);
                }
            }
        }
    }

    private ClusterComponent determineCmRepoConfig(Optional<Component> stackClouderaManagerRepoConfig,
            String osType, Cluster cluster, String cdhStackVersion) throws CloudbreakImageCatalogException {
        Json json;
        if (Objects.isNull(stackClouderaManagerRepoConfig) || stackClouderaManagerRepoConfig.isEmpty()) {
            ClouderaManagerRepo clouderaManagerRepo = defaultClouderaManagerRepoService.getDefault(
                    osType, StackType.CDH.name(), cdhStackVersion, cluster.getStack().getCloudPlatform());
            if (clouderaManagerRepo == null) {
                throw new BadRequestException(String.format("Couldn't determine Cloudera Manager repo for the stack: %s", cluster.getStack().getName()));
            }
            json = new Json(clouderaManagerRepo);
        } else {
            json = stackClouderaManagerRepoConfig.get().getAttributes();
        }
        return new ClusterComponent(ComponentType.CM_REPO_DETAILS, json, cluster);
    }

    private Set<ClouderaManagerProduct> determineCdhRepoConfig(Cluster cluster, List<Component> stackCdhRepoConfig,
            String osType, String blueprintCdhVersion, String imageCatalogName) throws CloudbreakImageCatalogException {
        if (Objects.isNull(stackCdhRepoConfig) || stackCdhRepoConfig.isEmpty()) {
            DefaultCDHInfo defaultCDHInfo = getDefaultCDHInfo(cluster.getWorkspace().getId(), blueprintCdhVersion, osType,
                    cluster.getStack().getCloudPlatform(), imageCatalogName);
            Map<String, String> stack = defaultCDHInfo.getRepo().getStack();
            ClouderaManagerProduct cmProduct = new ClouderaManagerProduct()
                    .withVersion(defaultCDHInfo.getVersion())
                    .withName(stack.get("repoid").split("-")[0])
                    .withParcel(stack.get(osType));
            Set<ClouderaManagerProduct> products = Sets.newHashSet(cmProduct);
            LOGGER.info("Product list before filter out products by blueprint: {}", products);
            Set<ClouderaManagerProduct> filteredProducts = parcelFilterService.filterParcelsByBlueprint(products, cluster.getBlueprint());
            LOGGER.info("Product list after filter out products by blueprint: {}", filteredProducts);
            return filteredProducts;
        } else {
            Set<ClouderaManagerProduct> products = stackCdhRepoConfig.stream()
                    .map(Component::getAttributes)
                    .map(json -> json.getSilent(ClouderaManagerProduct.class))
                    .collect(Collectors.toSet());
            LOGGER.info("Product list before filter out products by blueprint: {}", products);
            Set<ClouderaManagerProduct> filteredProducts = parcelFilterService.filterParcelsByBlueprint(products, cluster.getBlueprint());
            LOGGER.info("Product list after filter out products by blueprint: {}", filteredProducts);
            return filteredProducts;
        }
    }

    private DefaultCDHInfo getDefaultCDHInfo(Long workspaceId, String blueprintCdhVersion, String osType, String cloudPlatform,
            String imageCatalogName) throws CloudbreakImageCatalogException {
        DefaultCDHInfo defaultCDHInfo = null;
        Map<String, ImageBasedDefaultCDHInfo> entries = imageBasedDefaultCDHEntries.getEntries(workspaceId, cloudPlatform, imageCatalogName);

        if (blueprintCdhVersion != null && entries.containsKey(blueprintCdhVersion)) {
            defaultCDHInfo = entries.get(blueprintCdhVersion).getDefaultCDHInfo();
        }
        if (defaultCDHInfo == null) {
            defaultCDHInfo = entries.entrySet().stream()
                    .filter(e -> Objects.nonNull(e.getValue().getDefaultCDHInfo().getRepo().getStack().get(osType)))
                    .max(ImageBasedDefaultCDHEntries.IMAGE_BASED_CDH_ENTRY_COMPARATOR)
                    .orElseThrow(notFound("Default Product Info with OS type:", osType))
                    .getValue().getDefaultCDHInfo();
        }
        return defaultCDHInfo;
    }

    private boolean isCdhParcelMissing(List<ClouderaManagerProductV4Request> products, List<ClusterComponent> components) {
        boolean missingFromProducts = products.stream()
                .noneMatch(product -> CDH.equals(product.getName()));

        boolean missingFromComponents = components.stream()
                .filter(component -> component.getComponentType() == CDH_PRODUCT_DETAILS)
                .map(component -> component.getAttributes().getSilent(ClouderaManagerProduct.class))
                .noneMatch(product -> CDH.equals(product.getName()));

        return missingFromProducts && missingFromComponents;
    }
}
