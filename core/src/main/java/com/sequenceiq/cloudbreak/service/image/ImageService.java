package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CM_REPO_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.IMAGE;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V1;
import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V2;
import static com.sequenceiq.cloudbreak.service.image.ImageCatalogService.CDP_DEFAULT_CATALOG_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.DefaultClouderaManagerRepoService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.StackTypeResolver;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ComponentConverter componentConverter;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private DefaultClouderaManagerRepoService clouderaManagerRepoService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private StackTypeResolver stackTypeResolver;

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        return componentConfigProviderService.getImage(stackId);
    }

    public String getCurrentImageCatalogName(Long stackId) throws CloudbreakImageNotFoundException {
        return getImage(stackId).getImageCatalogName();
    }

    public StatedImage getCurrentImage(Long workspaceId, Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = getImage(stackId);
        return imageCatalogService.getImage(workspaceId, image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId());
    }

    @Measure(ImageService.class)
    public Set<Component> create(Stack stack, StatedImage imgFromCatalog)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {

        String region = stack.getRegion();
        String cloudPlatform = stack.getCloudPlatform();
        ImageCatalogPlatform platformString = platformStringTransformer.getPlatformStringForImageCatalog(cloudPlatform, stack.getPlatformVariant());
        LOGGER.debug("Determined image from catalog: {}", imgFromCatalog);
        String imageName = determineImageName(cloudPlatform, platformString, region, imgFromCatalog.getImage());
        LOGGER.debug("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                platformString, region, imageName, imgFromCatalog.getImageCatalogUrl());

        Set<Component> components = getComponents(stack, imgFromCatalog, EnumSet.of(IMAGE, cdhProductDetails(), CM_REPO_DETAILS));
        componentConfigProviderService.store(components);
        return components;
    }

    //CHECKSTYLE:OFF
    @Measure(ImageService.class)
    public StatedImage determineImageFromCatalog(Long workspaceId, ImageSettingsV4Request imageSettings, Architecture architecture, String platformString,
            String variant, Blueprint blueprint, boolean useBaseImage, boolean baseImageEnabled,
            User user, Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> imagePredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform platform = platformStringTransformer.getPlatformStringForImageCatalog(platformString, variant);
        String clusterVersion = getClusterVersion(blueprint);
        if (imageSettings != null && StringUtils.isNotEmpty(imageSettings.getId())) {
            LOGGER.debug("Image id {} is specified for the stack.", imageSettings.getId());

            if (imageSettings.getCatalog() == null) {
                imageSettings.setCatalog(CDP_DEFAULT_CATALOG_NAME);
            }
            StatedImage image = imageCatalogService.getImageByCatalogName(workspaceId, imageSettings.getId(), imageSettings.getCatalog());
            validateSpecifiedImage(workspaceId, user, imageSettings, image, clusterVersion, platform, baseImageEnabled, architecture);
            return image;
        } else if (useBaseImage && !baseImageEnabled) {
            throw new CloudbreakImageCatalogException("Inconsistent request, base images are disabled but custom repo information is submitted!");
        }

        boolean selectBaseImage = baseImageEnabled && useBaseImage;
        ImageFilter imageFilter = ImageFilter.builder()
                .withImageCatalog(getImageCatalogFromRequestOrDefault(workspaceId, imageSettings, user))
                .withPlatforms(Set.of(platform))
                .withBaseImageEnabled(baseImageEnabled)
                .withOperatingSystems(getSupportedOperatingSystems(workspaceId, imageSettings, clusterVersion, platform, architecture))
                .withClusterVersion(selectBaseImage ? null : clusterVersion)
                .withArchitecture(Objects.requireNonNullElse(architecture, Architecture.X86_64))
                .withAdditionalPredicate(imagePredicate)
                .build();
        LOGGER.info("Image id is not specified for the stack.");
        return imageCatalogService.getLatestImageDefaultPreferred(imageFilter, selectBaseImage);
    }

    private void validateSpecifiedImage(Long workspaceId, User user, ImageSettingsV4Request imageSettings, StatedImage image, String clusterVersion,
            ImageCatalogPlatform platform, boolean baseImageEnabled, Architecture architecture) throws CloudbreakImageCatalogException {
        validateIfBaseImagePermitted(image, baseImageEnabled);
        validateImageOs(workspaceId, imageSettings, image, clusterVersion, platform);
        validateArchitecture(user, image, architecture);
    }

    private void validateIfBaseImagePermitted(StatedImage image, boolean baseImageEnabled) throws CloudbreakImageCatalogException {
        if (!baseImageEnabled && !image.getImage().isPrewarmed()) {
            throw new CloudbreakImageCatalogException(String.format("Inconsistent request, base images are disabled but image with id %s is base image!",
                    image.getImage().getUuid()));
        }
    }

    private void validateImageOs(Long workspaceId, ImageSettingsV4Request imageSettings, StatedImage statedImage, String clusterVersion,
            ImageCatalogPlatform platform) throws CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image image = statedImage.getImage();
        if (imageSettings != null && StringUtils.isNotBlank(imageSettings.getId())
                && StringUtils.isNotBlank(imageSettings.getOs()) && !imageSettings.getOs().equalsIgnoreCase(image.getOs())) {
            throw new CloudbreakImageCatalogException("Image with requested id has different os than requested.");
        }
        if (!image.isPrewarmed()) {
            Set<String> operatingSystems = getSupportedOperatingSystems(workspaceId, statedImage, clusterVersion, platform);
            if (!operatingSystems.contains(image.getOs())) {
                String message = String.format("The %s OS of the selected base image (%s) is not compatible with the runtime version %s.",
                        image.getOs(), image.getUuid(), clusterVersion);
                throw new CloudbreakImageCatalogException(message);
            }
        }
    }

    private void validateArchitecture(User user, StatedImage image, Architecture requestedArchitecture) throws CloudbreakImageCatalogException {
        Architecture imageArchitecture = Architecture.fromStringWithFallback(image.getImage().getArchitecture());
        if (requestedArchitecture != null && imageArchitecture != requestedArchitecture) {
            throw new CloudbreakImageCatalogException(String.format("The selected image's architecture (%s) is not matching requested architecture (%s)",
                    imageArchitecture.getName(), requestedArchitecture.getName()));
        }
    }

    private String getClusterVersion(Blueprint blueprint) {
        String clusterVersion = ImageCatalogService.UNDEFINED;
        if (blueprint != null) {
            try {
                JsonNode root = JsonUtil.readTree(blueprint.getBlueprintJsonText());
                clusterVersion = blueprintUtils.getCDHStackVersion(root);
            } catch (IOException ex) {
                LOGGER.warn("Can not initiate default hdp info: ", ex);
            }
        }
        return clusterVersion;
    }

    private Set<String> getSupportedOperatingSystems(Long workspaceId, StatedImage image, String clusterVersion, ImageCatalogPlatform platform)
            throws CloudbreakImageCatalogException {
        String imageCatalogName = image.getImageCatalogName();
        String os = image.getImage().getOs();
        Architecture architecture = Architecture.fromStringWithFallback(image.getImage().getArchitecture());
        return getSupportedOperationSystems(workspaceId, clusterVersion, platform, os, architecture, imageCatalogName);
    }

    private Set<String> getSupportedOperatingSystems(Long workspaceId, ImageSettingsV4Request imageSettings, String clusterVersion,
            ImageCatalogPlatform platform,
            Architecture architecture) throws CloudbreakImageCatalogException {
        String imageCatalogName = imageSettings != null ? imageSettings.getCatalog() : null;
        String os = imageSettings != null ? imageSettings.getOs() : null;
        return getSupportedOperationSystems(workspaceId, clusterVersion, platform, os, architecture, imageCatalogName);
    }

    private Set<String> getSupportedOperationSystems(Long workspaceId, String clusterVersion, ImageCatalogPlatform platform, String os,
            Architecture architecture, String imageCatalogName) throws CloudbreakImageCatalogException {
        try {
            Set<String> operatingSystems =
                    stackMatrixService.getSupportedOperatingSystems(workspaceId, clusterVersion, platform, os, architecture, imageCatalogName);
            if (StringUtils.isNotEmpty(os)) {
                if (operatingSystems.isEmpty()) {
                    operatingSystems = Collections.singleton(os);
                } else {
                    operatingSystems = operatingSystems.stream().filter(o -> o.equalsIgnoreCase(os)).collect(Collectors.toSet());
                }
            }
            return operatingSystems;
        } catch (Exception ex) {
            throw new CloudbreakImageCatalogException(ex);
        }
    }
    //CHECKSTYLE:ON

    private ImageCatalog getImageCatalogFromRequestOrDefault(Long workspaceId, ImageSettingsV4Request imageSettings, User user)
            throws CloudbreakImageCatalogException {
        if (imageSettings == null || imageSettings.getCatalog() == null) {
            return imageCatalogService.getDefaultImageCatalog(user);
        } else {
            try {
                return imageCatalogService.getImageCatalogByName(workspaceId, imageSettings.getCatalog());
            } catch (NotFoundException e) {
                throw new CloudbreakImageCatalogException(e.getMessage());
            }
        }
    }

    public String determineImageName(String cloudPlatform, ImageCatalogPlatform platformString, String region,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog) throws CloudbreakImageNotFoundException {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(
                platformString.nameToLowerCase(),
                imgFromCatalog.getImageSetsByProvider());
        String translatedRegion = cloudPlatformConnectors.getDefault(platform(cloudPlatform.toUpperCase(Locale.ROOT))).regionToDisplayName(region);
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            return selectImageByRegionPreferDefault(translatedRegion, imagesByRegion, platformString.nameToLowerCase());
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new CloudbreakImageNotFoundException(msg);
    }

    public String determineImageNameByRegion(String cloudPlatform, ImageCatalogPlatform platformString, String region,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog) throws CloudbreakImageNotFoundException {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(
                platformString.nameToLowerCase(),
                imgFromCatalog.getImageSetsByProvider());
        String translatedRegion = cloudPlatformConnectors.getDefault(platform(cloudPlatform.toUpperCase(Locale.ROOT))).regionToDisplayName(region);
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            return selectImageByRegion(translatedRegion, imagesByRegion, platformString.nameToLowerCase());
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new CloudbreakImageNotFoundException(msg);
    }

    public void updateImageNameForImageComponent(Long stackId, String newImageName) {
        try {
            com.sequenceiq.cloudbreak.domain.stack.Component component = componentConfigProviderService.getImageComponent(stackId);
            Image currentImage = component.getAttributes().get(Image.class);
            currentImage.setImageName(newImageName);
            component.setAttributes(new Json(currentImage));
            componentConfigProviderService.store(component);
        } catch (CloudbreakImageNotFoundException | IOException e) {
            LOGGER.warn("This exception ({}) should not happen at all, please investigate", e.getMessage());
        }
    }

    private String selectImageByRegionPreferDefault(String translatedRegion, Map<String, String> imagesByRegion, String platform)
            throws CloudbreakImageNotFoundException {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(platform) && entitlementService.azureMarketplaceImagesEnabled(accountId)) {
            return selectAzureMarketplaceImage(translatedRegion, imagesByRegion, platform, accountId);
        } else {
            LOGGER.debug("Preferred region is translated region: {}. Platform: {}", translatedRegion, platform);
            return findStringKeyWithEqualsIgnoreCase(translatedRegion, imagesByRegion)
                    .or(() -> {
                        LOGGER.debug("Not found image with translated region: {}. Attempt to find with 'default' region. Platform: {}",
                                translatedRegion, platform);
                        return findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
                    })
                    .orElseThrow(() -> new CloudbreakImageNotFoundException(
                            String.format("The virtual machine image couldn't be found for %s in the %s region. Available images are as follows: %s.",
                                    platform, translatedRegion, imagesByRegion)));
        }
    }

    private String selectAzureMarketplaceImage(String translatedRegion, Map<String, String> imagesByRegion, String platform, String accountId)
            throws CloudbreakImageNotFoundException {
        LOGGER.debug("Preferred region is 'default'. Platform: {}, Azure Marketplace images enabled.", platform);
        return findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion)
                .or(() -> supplyAlternativeImageWhenEntitlementAllows(translatedRegion, imagesByRegion, accountId))
                .orElseThrow(() -> new CloudbreakImageNotFoundException(
                        String.format("The preferred Azure Marketplace image is not present and virtual machine image could not be found in the %s region. " +
                                        "Available images are as follows: %s.",
                                translatedRegion, imagesByRegion)));
    }

    private Optional<String> supplyAlternativeImageWhenEntitlementAllows(String translatedRegion, Map<String, String> imagesByRegion, String accountId) {
        if (entitlementService.azureOnlyMarketplaceImagesEnabled(accountId)) {
            LOGGER.debug("No Azure Marketplace images found. Only Azure Marketplace images are allowed, skipping search for alternative images.");
            return Optional.empty();
        } else {
            LOGGER.debug("Searching for alternative Azure images in translated region: {}", translatedRegion);
            return findStringKeyWithEqualsIgnoreCase(translatedRegion, imagesByRegion);
        }
    }

    private String selectImageByRegion(String translatedRegion, Map<String, String> imagesByRegion, String platform) throws CloudbreakImageNotFoundException {
        return findStringKeyWithEqualsIgnoreCase(translatedRegion, imagesByRegion)
                .orElseThrow(() -> new CloudbreakImageNotFoundException(
                        String.format("The virtual machine image couldn't be found for %s in the %s region. Available images are as follows: %s.",
                                platform, translatedRegion, imagesByRegion)));
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Entry::getValue)
                .findFirst();
    }

    public Set<Component> getComponentsWithoutUserData(Stack stack, StatedImage statedImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getComponents(stack, statedImage, EnumSet.of(cdhProductDetails(), CM_REPO_DETAILS));
    }

    public Set<Component> getComponents(Stack stack, StatedImage statedImage, EnumSet<ComponentType> requestedComponents)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Set<Component> components = new HashSet<>();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage = statedImage.getImage();
        String cloudPlatform = stack.getCloudPlatform();
        if (requestedComponents.contains(IMAGE)) {
            String imageName = determineImageName(
                    cloudPlatform,
                    platformStringTransformer.getPlatformStringForImageCatalog(stack.cloudPlatform(), stack.getPlatformVariant()),
                    stack.getRegion(),
                    statedImage.getImage()
            );
            addImage(stack, statedImage, imageName, catalogBasedImage, components);
        }
        if (centralCDHVersionCoordinator.requestedComponentTypesContainsCdhComponentType(requestedComponents)) {
            addStackRepo(stack, components, catalogBasedImage);
            addPrewarmParcels(stack, statedImage, components);
        }
        if (requestedComponents.contains(CM_REPO_DETAILS)) {
            addCmRepo(stack, components, catalogBasedImage);
        }
        return components;
    }

    public Optional<String> getSupportedImdsVersion(String cloudPlatform, StatedImage image) {
        if (CloudPlatform.AWS.equals(CloudPlatform.valueOf(cloudPlatform))) {
            Map<String, String> packageVersions = image.getImage().getPackageVersions();
            if (packageVersions.containsKey(ImagePackageVersion.IMDS_VERSION.getKey()) &&
                    StringUtils.equals(packageVersions.get(ImagePackageVersion.IMDS_VERSION.getKey()), AWS_IMDS_VERSION_V2)) {
                return Optional.of(AWS_IMDS_VERSION_V2);
            }
            return Optional.of(AWS_IMDS_VERSION_V1);
        }
        return Optional.empty();
    }

    private void addPrewarmParcels(Stack stack, StatedImage statedImage, Set<Component> components) {
        Set<ClouderaManagerProduct> prewarmParcels = clouderaManagerProductTransformer.transform(statedImage.getImage(), false, true);
        components.addAll(componentConverter.fromClouderaManagerProductList(prewarmParcels, stack));
    }

    private void addStackRepo(Stack stack, Set<Component> components, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage)
            throws CloudbreakImageCatalogException {
        if (catalogBasedImage.getStackDetails() != null) {
            ImageStackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = stackTypeResolver.determineStackType(stackDetails);
            Component stackRepoComponent = getStackComponent(stack, stackDetails, stackType, catalogBasedImage.getOsType());
            components.add(stackRepoComponent);
        }
    }

    private void addCmRepo(Stack stack, Set<Component> components, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage)
            throws CloudbreakImageCatalogException {
        if (catalogBasedImage.getStackDetails() != null) {
            ImageStackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = stackTypeResolver.determineStackType(stackDetails);
            ClouderaManagerRepo clouderaManagerRepo = clouderaManagerRepoService.getClouderaManagerRepo(catalogBasedImage, stackType);
            components.add(new Component(CM_REPO_DETAILS, CM_REPO_DETAILS.name(), new Json(clouderaManagerRepo), stack));
        } else {
            LOGGER.debug("There are no stackDetails for stack {}, cannot determine CM repo version.", stack.getName());
        }
    }

    private void addImage(Stack stack, StatedImage statedImage, String imageName, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage,
            Set<Component> components) {
        Image image = new Image(imageName, new HashMap<>(), catalogBasedImage.getOs(), catalogBasedImage.getOsType(), catalogBasedImage.getArchitecture(),
                statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), catalogBasedImage.getUuid(),
                catalogBasedImage.getPackageVersions(), catalogBasedImage.getDate(), catalogBasedImage.getCreated(), catalogBasedImage.getTags());
        components.add(new Component(IMAGE, IMAGE.name(), new Json(image), stack));
    }

    private Component getStackComponent(Stack stack, ImageStackDetails stackDetails, StackType stackType, String osType) {
        ComponentType componentType = stackType.getComponentType();
        if (centralCDHVersionCoordinator.isCdhProductDetails(componentType)) {
            ClouderaManagerProduct product = createProductRepo(stackDetails, osType);
            return new Component(componentType, componentType.name(), new Json(product), stack);
        } else {
            StackRepoDetails repo = createStackRepo(stackDetails);
            return new Component(componentType, componentType.name(), new Json(repo), stack);
        }
    }

    private StackRepoDetails createStackRepo(ImageStackDetails stack) {
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(stack.getVersion());
        repo.setStack(stack.getRepo().getStack());
        repo.setUtil(stack.getRepo().getUtil());
        return repo;
    }

    private ClouderaManagerProduct createProductRepo(ImageStackDetails stack, String osType) {
        ClouderaManagerProduct cmProduct = new ClouderaManagerProduct();
        Map<String, String> stackInfo = stack.getRepo().getStack();
        cmProduct.
                withVersion(stackInfo.get(StackRepoDetails.REPOSITORY_VERSION)).
                withName(stackInfo.get(StackRepoDetails.REPO_ID_TAG).split("-")[0]).
                withParcel(stackInfo.get(osType));
        return cmProduct;
    }

}