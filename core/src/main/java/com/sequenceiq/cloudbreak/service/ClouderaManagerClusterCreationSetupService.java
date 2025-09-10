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

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelFilterService;
import com.sequenceiq.common.model.ImageCatalogPlatform;

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

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    public List<ClusterComponent> prepareClouderaManagerCluster(ClusterV4Request request, Cluster cluster,
            Component stackClouderaManagerRepoConfig,
            List<Component> stackCdhRepoConfig,
            Component stackImageComponent) throws IOException, CloudbreakImageCatalogException {
        List<ClusterComponent> components = new ArrayList<>();
        long start = System.currentTimeMillis();
        String blueprintCdhVersion = blueprintUtils.getCDHStackVersion(JsonUtil.readTree(cluster.getBlueprint().getBlueprintJsonText()));
        LOGGER.debug("blueprintUtils.getCDHStackVersion took {} ms", System.currentTimeMillis() - start);

        ClouderaManagerRepositoryV4Request cmRepoRequest = measure(() ->
                Optional.ofNullable(request.getCm()).map(ClouderaManagerV4Request::getRepository).orElse(null),
                LOGGER,
                "ClouderaManagerV4Request::getRepository {} ms");

        start = System.currentTimeMillis();
        Image image = getImage(stackImageComponent);
        LOGGER.debug("getImageCatalogName took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        ClusterComponent cmRepoConfig = getCmRepoConfiguration(cluster, stackClouderaManagerRepoConfig, components,
                blueprintCdhVersion, cmRepoRequest, image);
        LOGGER.debug("getCmRepoConfiguration took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        checkCmStackRepositories(cmRepoConfig, image);
        LOGGER.debug("checkCmStackRepositories took {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        List<ClusterComponent> productComponents =
                getProductComponents(request, cluster, stackCdhRepoConfig, blueprintCdhVersion, image);
        components.addAll(productComponents);
        LOGGER.debug("addProductComponentsToCluster took {} ms", System.currentTimeMillis() - start);

        return components;
    }

    private Image getImage(Component stackImageComponent) throws IOException {
        return stackImageComponent.getAttributes().get(Image.class);
    }

    private ClusterComponent getCmRepoConfiguration(Cluster cluster, Component stackClouderaManagerRepoConfig,
            List<ClusterComponent> components, String blueprintCdhVersion, ClouderaManagerRepositoryV4Request cmRepoRequest, Image image)
            throws CloudbreakImageCatalogException {
        ClusterComponent cmRepoConfig;
        if (Objects.isNull(cmRepoRequest)) {
            cmRepoConfig = determineCmRepoConfig(stackClouderaManagerRepoConfig, cluster, blueprintCdhVersion, image);
            components.add(cmRepoConfig);
        } else {
            cmRepoConfig = cluster.getComponents().stream().
                    filter(component -> ComponentType.CM_REPO_DETAILS.equals(component.getComponentType())).findFirst().orElse(null);
        }
        return cmRepoConfig;
    }

    private List<ClusterComponent> getProductComponents(ClusterV4Request request, Cluster cluster, List<Component> stackCdhRepoConfig,
            String blueprintCdhVersion, Image image)
            throws CloudbreakImageCatalogException {
        List<ClusterComponent> components = new ArrayList<>();
        List<ClouderaManagerProductV4Request> products = Optional.ofNullable(request.getCm()).map(ClouderaManagerV4Request::getProducts)
                .orElse(Collections.emptyList());
        if (products.isEmpty()) {
            Set<ClouderaManagerProduct> filteredProducts = determineCdhRepoConfig(cluster, stackCdhRepoConfig, blueprintCdhVersion, image);
            components.addAll(convertParcelsToClusterComponents(cluster, filteredProducts));
        }
        if (isCdhParcelMissing(products, components)) {
            throw new BadRequestException("CDH parcel is missing from the cluster. "
                    + "If parcels are provided in the request, CDH parcel must be specified too.");
        }
        components.addAll(cluster.getComponents());
        return components;
    }

    private List<ClusterComponent> convertParcelsToClusterComponents(Cluster cluster, Set<ClouderaManagerProduct> filteredProducts) {
        return filteredProducts.stream()
                .map(product -> new ClusterComponent(CDH_PRODUCT_DETAILS, product.getName(), new Json(product), cluster))
                .collect(Collectors.toList());
    }

    private void checkCmStackRepositories(ClusterComponent cmRepoConfig, Image image)
            throws IOException, CloudbreakImageCatalogException {
        if (Objects.nonNull(cmRepoConfig)) {
            ClouderaManagerRepo clouderaManagerRepo = cmRepoConfig.getAttributes().get(ClouderaManagerRepo.class);
            StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix(
                    cmRepoConfig.getCluster().getWorkspace().getId(),
                    platformStringTransformer.getPlatformStringForImageCatalog(
                            cmRepoConfig.getCluster().getStack().getCloudPlatform(),
                            cmRepoConfig.getCluster().getStack().getPlatformVariant()),
                    image.getOs(),
                    image.getArchitectureEnum(),
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

    private ClusterComponent determineCmRepoConfig(Component stackClouderaManagerRepoConfig, Cluster cluster, String cdhStackVersion, Image image)
            throws CloudbreakImageCatalogException {
        Json json;
        if (Objects.isNull(stackClouderaManagerRepoConfig)) {
            ImageCatalogPlatform platform = platformStringTransformer
                    .getPlatformStringForImageCatalog(cluster.getStack().getCloudPlatform(), cluster.getStack().getPlatformVariant());

            ClouderaManagerRepo clouderaManagerRepo = defaultClouderaManagerRepoService.getDefault(
                    cluster.getWorkspace().getId(), cdhStackVersion, platform, image);
            if (clouderaManagerRepo == null) {
                throw new BadRequestException(String.format("Couldn't determine Cloudera Manager repo for the stack: %s", cluster.getStack().getName()));
            }
            json = new Json(clouderaManagerRepo);
        } else {
            json = stackClouderaManagerRepoConfig.getAttributes();
        }
        return new ClusterComponent(ComponentType.CM_REPO_DETAILS, json, cluster);
    }

    private Set<ClouderaManagerProduct> determineCdhRepoConfig(Cluster cluster, List<Component> stackCdhRepoConfig, String blueprintCdhVersion, Image image)
            throws CloudbreakImageCatalogException {
        if (Objects.isNull(stackCdhRepoConfig) || stackCdhRepoConfig.isEmpty()) {
            DefaultCDHInfo defaultCDHInfo = getDefaultCDHInfo(cluster, blueprintCdhVersion, image);
            Map<String, String> stack = defaultCDHInfo.getRepo().getStack();
            Set<ClouderaManagerProduct> cdhProduct = Set.of(new ClouderaManagerProduct()
                    .withVersion(defaultCDHInfo.getVersion())
                    .withName(stack.get("repoid").split("-")[0])
                    .withParcel(stack.get(image.getOsType())));
            LOGGER.debug("Determined CDH product: {}", cdhProduct);
            return cdhProduct;
        } else {
            Set<ClouderaManagerProduct> products = stackCdhRepoConfig.stream()
                    .map(Component::getAttributes)
                    .map(json -> json.getUnchecked(ClouderaManagerProduct.class))
                    .collect(Collectors.toSet());
            return filterParcelsIfNecessary(cluster, products);
        }
    }

    private Set<ClouderaManagerProduct> filterParcelsIfNecessary(Cluster cluster, Set<ClouderaManagerProduct> products) {
        Stack stack = cluster.getStack();
        if (stack.isDatalake()) {
            return Set.of(clouderaManagerProductsProvider.getCdhProduct(products).orElseThrow(() -> new NotFoundException("Runtime component not found!")));
        } else {
            LOGGER.info("Product list before filter out products by blueprint: {}", products);
            Set<ClouderaManagerProduct> filteredProducts = parcelFilterService
                    .filterParcelsByBlueprint(stack.getWorkspace().getId(), stack.getId(), products, cluster.getBlueprint());
            LOGGER.info("Product list after filter out products by blueprint: {}", filteredProducts);
            return filteredProducts;
        }
    }

    private DefaultCDHInfo getDefaultCDHInfo(Cluster cluster, String blueprintCdhVersion, Image image)
            throws CloudbreakImageCatalogException {
        DefaultCDHInfo defaultCDHInfo = null;
        Stack stack = cluster.getStack();
        ImageCatalogPlatform platformString = platformStringTransformer.getPlatformStringForImageCatalog(stack.getCloudPlatform(), stack.getPlatformVariant());
        Map<String, ImageBasedDefaultCDHInfo> entries = imageBasedDefaultCDHEntries.getEntries(cluster.getWorkspace().getId(), platformString,
                image.getOs(), image.getArchitectureEnum(), image.getImageCatalogName());
        if (blueprintCdhVersion != null && entries.containsKey(blueprintCdhVersion)) {
            defaultCDHInfo = entries.get(blueprintCdhVersion).getDefaultCDHInfo();
        }
        if (defaultCDHInfo == null) {
            defaultCDHInfo = entries.entrySet().stream()
                    .filter(e -> Objects.nonNull(e.getValue().getDefaultCDHInfo().getRepo().getStack().get(image.getOsType())))
                    .max(ImageBasedDefaultCDHEntries.IMAGE_BASED_CDH_ENTRY_COMPARATOR)
                    .orElseThrow(notFound("Default Product Info with OS type:", image.getOsType()))
                    .getValue().getDefaultCDHInfo();
        }
        return defaultCDHInfo;
    }

    private boolean isCdhParcelMissing(List<ClouderaManagerProductV4Request> products, List<ClusterComponent> components) {
        boolean missingFromProducts = products.stream()
                .noneMatch(product -> CDH.equals(product.getName()));

        boolean missingFromComponents = components.stream()
                .filter(component -> component.getComponentType() == CDH_PRODUCT_DETAILS)
                .map(component -> component.getAttributes().getUnchecked(ClouderaManagerProduct.class))
                .noneMatch(product -> CDH.equals(product.getName()));

        return missingFromProducts && missingFromComponents;
    }
}
