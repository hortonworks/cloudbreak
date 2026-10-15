package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.common.model.OsType;

@Service
public class ClusterUpgradePropertiesFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePropertiesFactory.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private BaseImagePackageVersionsEnricher baseImagePackageVersionsEnricher;

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private OsChangeService osChangeService;

    public ClusterUpgradeProperties create(ImageChangeDto imageChangeDto, boolean lockComponents, boolean rollingUpgradeEnabled, boolean replaceVms) {
        return create(imageChangeDto, null, new ClusterUpgradeProperties.UpgradeRequestOptions(replaceVms, lockComponents, rollingUpgradeEnabled));
    }

    public ClusterUpgradeProperties create(ImageChangeDto imageChangeDto, Image originalImage, ClusterUpgradeProperties.UpgradeRequestOptions options) {
        try {
            if (imageChangeDto.getComponents() != null) {
                imageChangeDto.getComponents().validate();
            }
            Image currentImage = componentConfigProviderService.getImage(imageChangeDto.getStackId());
            Stack stack = stackService.get(imageChangeDto.getStackId());
            String catalogUrl = imageChangeDto.getImageCatalogUrl() != null ? imageChangeDto.getImageCatalogUrl() : currentImage.getImageCatalogUrl();
            String catalogName = imageChangeDto.getImageCatalogName() != null ? imageChangeDto.getImageCatalogName() : currentImage.getImageCatalogName();
            StatedImage targetStatedImage = imageCatalogService.getImage(stack.getWorkspace().getId(), catalogUrl, catalogName, imageChangeDto.getImageId());
            return buildProperties(stack, originalImage != null ? originalImage : currentImage, targetStatedImage, options, imageChangeDto.getComponents());
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for cluster upgrade", e);
        } catch (CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image catalog is not reachable", e);
        }
    }

    public ClusterUpgradeProperties create(Long stackId, Image originalImage, StatedImage targetImage, ClusterUpgradeProperties.UpgradeRequestOptions options) {
        return buildProperties(stackService.get(stackId), originalImage, targetImage, options, null);
    }

    private ClusterUpgradeProperties buildProperties(Stack stack, Image currentImage, StatedImage targetStatedImage,
            ClusterUpgradeProperties.UpgradeRequestOptions options, UpgradeComponents components) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = targetStatedImage.getImage();
        Image targetCloudImage = stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage);
        OsType targetOsType = OsType.getByOsTypeString(targetCatalogImage.getOsType());

        Set<ClouderaManagerProduct> allProducts = components == null
                ? clouderaManagerProductTransformer.transform(targetCatalogImage, true, !stack.isDatalake())
                : getRequestedProducts(components, targetOsType, targetCatalogImage.getArchitecture());
        ClouderaManagerProduct cdhParcel = allProducts.stream()
                .filter(product -> CDH.name().equals(product.getName()))
                .findFirst()
                .orElse(null);
        Set<ClouderaManagerProduct> preWarmParcels = allProducts.stream()
                .filter(product -> cdhParcel == null || !cdhParcel.equals(product))
                .collect(Collectors.toCollection(HashSet::new));

        ClouderaManagerRepo cmRepo = components == null ? imageToClouderaManagerRepoConverter.convert(targetCatalogImage)
                : getRequestedCmRepo(components, targetOsType, targetCatalogImage.getArchitecture());
        Map<String, String> packageVersions = new HashMap<>(targetCatalogImage.getPackageVersions());
        if (components != null) {
            packageVersions.putAll(baseImagePackageVersionsEnricher.getPackageVersionsFromComponents(cmRepo, allProducts));
        }
        String runtimeVersion = packageVersions.getOrDefault(ImagePackageVersion.STACK.getKey(), targetCatalogImage.getVersion());

        ClusterUpgradeProperties.CurrentImageUpgradeContext currentImageContext = createCurrentImageContext(currentImage, stack);
        ClusterUpgradeProperties.TargetImageUpgradeContext targetImageContext = new ClusterUpgradeProperties.TargetImageUpgradeContext(
                targetCatalogImage.getUuid(),
                targetStatedImage.getImageCatalogName(),
                targetStatedImage.getImageCatalogUrl(),
                runtimeVersion,
                components == null ? targetCatalogImage.getVersion() : runtimeVersion,
                packageVersions.get(ImagePackageVersion.CDH_BUILD_NUMBER.getKey()),
                packageVersions,
                targetCatalogImage.getTags() != null ? new HashMap<>(targetCatalogImage.getTags()) : new HashMap<>(),
                targetOsType,
                targetCatalogImage.getOs(),
                targetCatalogImage.getArchitecture(),
                targetCatalogImage.getDate(),
                targetCatalogImage.getCreated(),
                targetCloudImage.getImageName(),
                targetCatalogImage.getStackDetails(),
                targetCatalogImage.getRepo() != null ? new HashMap<>(targetCatalogImage.getRepo()) : new HashMap<>(),
                targetCatalogImage.getPreWarmParcels(),
                targetCatalogImage.getPreWarmCsd(),
                cdhParcel,
                preWarmParcels,
                cmRepo);

        LOGGER.debug("Resolved cluster upgrade properties for target image {} with runtime version {}", targetCatalogImage.getUuid(), runtimeVersion);
        return new ClusterUpgradeProperties(options, currentImageContext, targetImageContext);
    }

    private ClusterUpgradeProperties.CurrentImageUpgradeContext createCurrentImageContext(Image currentImage, Stack stack) {
        Image enrichedImage = StringUtils.isBlank(currentImage.getPackageVersion(ImagePackageVersion.STACK))
                ? baseImagePackageVersionsEnricher.enrich(currentImage, stack) : currentImage;
        Long clusterId = stack.getCluster() != null ? stack.getCluster().getId() : null;
        Set<ClouderaManagerProduct> currentProducts = captureCurrentClusterProducts(clusterId);
        ClouderaManagerRepo currentCmRepo = clusterId != null ? clusterComponentConfigProvider.getClouderaManagerRepoDetails(clusterId) : null;
        return new ClusterUpgradeProperties.CurrentImageUpgradeContext(
                enrichedImage.getImageId(),
                enrichedImage.getImageCatalogName(),
                enrichedImage.getImageCatalogUrl(),
                enrichedImage.getPackageVersion(ImagePackageVersion.STACK),
                enrichedImage.getPackageVersions() != null ? new HashMap<>(enrichedImage.getPackageVersions()) : new HashMap<>(),
                enrichedImage.getTags() != null ? new HashMap<>(enrichedImage.getTags()) : new HashMap<>(),
                OsType.getByOsTypeString(enrichedImage.getOsType()),
                enrichedImage.getOs(),
                enrichedImage.getArchitecture(),
                enrichedImage.getDate(),
                enrichedImage.getCreated(),
                enrichedImage.getImageName(),
                currentProducts,
                currentCmRepo);
    }

    private Set<ClouderaManagerProduct> captureCurrentClusterProducts(Long clusterId) {
        return clusterId != null ? new HashSet<>(clusterComponentConfigProvider.getClouderaManagerProductDetails(clusterId)) : new HashSet<>();
    }

    private Set<ClouderaManagerProduct> getRequestedProducts(UpgradeComponents components, OsType osType, String architecture) {
        return components.products().stream().map(product -> {
            String parcelUrl = osChangeService.updateRepoUrlForOs(StringUtils.stripEnd(product.getParcel(), "/") + "/", osType, architecture);
            return new ClouderaManagerProduct().withName(product.getName()).withVersion(product.getVersion())
                    .withParcel(parcelUrl).withCsd(product.getCsd() == null ? null : product.getCsd().stream()
                            .map(url -> osChangeService.updateRepoUrlForOs(url, osType, architecture)).toList())
                    .withParcelFileUrl(ClouderaManagerProductTransformer.getParcelFileUrl(
                            parcelUrl, product.getName(), product.getVersion(), osType, architecture));
        }).collect(Collectors.toSet());
    }

    private ClouderaManagerRepo getRequestedCmRepo(UpgradeComponents components, OsType osType, String architecture) {
        ClouderaManagerRepo repo = components.getNormalizedRepository();
        repo.setBaseUrl(osChangeService.updateRepoUrlForOs(StringUtils.appendIfMissing(repo.getBaseUrl(), "/"), osType, architecture));
        repo.setGpgKeyUrl(osChangeService.updateRepoUrlForOs(repo.getGpgKeyUrl(), osType, architecture));
        return repo;
    }

}
