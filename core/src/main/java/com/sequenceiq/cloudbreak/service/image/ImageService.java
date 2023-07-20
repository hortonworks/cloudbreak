package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CM_REPO_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.IMAGE;
import static com.sequenceiq.cloudbreak.service.image.ImageCatalogService.CDP_DEFAULT_CATALOG_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.workspace.model.User;
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
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ComponentConverter componentConverter;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UserDataService userDataService;

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

        Set<Component> components = getComponents(stack, imgFromCatalog, EnumSet.of(IMAGE, CDH_PRODUCT_DETAILS, CM_REPO_DETAILS));
        componentConfigProviderService.store(components);
        return components;
    }

    //CHECKSTYLE:OFF
    @Measure(ImageService.class)
    public StatedImage determineImageFromCatalog(Long workspaceId, ImageSettingsV4Request imageSettings, String platformString,
            String variant, Blueprint blueprint, boolean useBaseImage, boolean baseImageEnabled,
            User user, Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> imagePredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform platform = platformStringTransformer.getPlatformStringForImageCatalog(platformString, variant);

        if (imageSettings != null && StringUtils.isNotEmpty(imageSettings.getId())) {
            LOGGER.debug("Image id {} is specified for the stack.", imageSettings.getId());

            if (imageSettings.getCatalog() == null) {
                imageSettings.setCatalog(CDP_DEFAULT_CATALOG_NAME);
            }
            StatedImage image = imageCatalogService.getImageByCatalogName(workspaceId, imageSettings.getId(), imageSettings.getCatalog());
            return checkIfBasePermitted(image, baseImageEnabled);
        } else if (useBaseImage && !baseImageEnabled) {
            throw new CloudbreakImageCatalogException("Inconsistent request, base images are disabled but custom repo information is submitted!");
        }

        String clusterVersion = getClusterVersion(blueprint);

        Set<String> operatingSystems;
        try {
            operatingSystems = getSupportedOperatingSystems(workspaceId, imageSettings, clusterVersion, platform);
        } catch (Exception ex) {
            throw new CloudbreakImageCatalogException(ex);
        }

        ImageCatalog imageCatalog = getImageCatalogFromRequestOrDefault(workspaceId, imageSettings, user);
        boolean selectBaseImage = baseImageEnabled && useBaseImage;
        ImageFilter imageFilter = new ImageFilter(
                imageCatalog,
                Set.of(platform),
                null,
                baseImageEnabled,
                operatingSystems,
                selectBaseImage ? null : clusterVersion);
        LOGGER.info("Image id is not specified for the stack.");
        if (selectBaseImage) {
            LOGGER.info("Trying to select a base image.");
            return imageCatalogService.getLatestBaseImageDefaultPreferred(imageFilter, imagePredicate);
        } else {
            LOGGER.info("Trying to select a prewarmed image.");
            return imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilter, imagePredicate);
        }
    }

    private String getClusterVersion(Blueprint blueprint) {
        String clusterVersion = ImageCatalogService.UNDEFINED;
        if (blueprint != null) {
            try {
                JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
                clusterVersion = blueprintUtils.getCDHStackVersion(root);
            } catch (IOException ex) {
                LOGGER.warn("Can not initiate default hdp info: ", ex);
            }
        }
        return clusterVersion;
    }

    private StatedImage checkIfBasePermitted(StatedImage image, boolean baseImageEnabled) throws CloudbreakImageCatalogException {
        if (!baseImageEnabled && !image.getImage().isPrewarmed()) {
            throw new CloudbreakImageCatalogException(String.format("Inconsistent request, base images are disabled but image with id %s is base image!",
                    image.getImage().getUuid()));
        }
        return image;
    }

    private Set<String> getSupportedOperatingSystems(Long workspaceId, ImageSettingsV4Request imageSettings, String clusterVersion, ImageCatalogPlatform platform)
            throws Exception {
        String imageCatalogName = imageSettings != null ? imageSettings.getCatalog() : null;
        String os = imageSettings != null ? imageSettings.getOs() : null;
        Set<String> operatingSystems = stackMatrixService.getSupportedOperatingSystems(workspaceId, clusterVersion, platform, os, imageCatalogName);
        if (StringUtils.isNotEmpty(os)) {
            if (operatingSystems.isEmpty()) {
                operatingSystems = Collections.singleton(os);
            } else {
                operatingSystems = operatingSystems.stream().filter(o -> o.equalsIgnoreCase(os)).collect(Collectors.toSet());
            }
        }
        return operatingSystems;
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
        String translatedRegion = cloudPlatformConnectors.getDefault(platform(cloudPlatform.toUpperCase())).regionToDisplayName(region);
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
        String translatedRegion = cloudPlatformConnectors.getDefault(platform(cloudPlatform.toUpperCase())).regionToDisplayName(region);
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            return selectImageByRegion(translatedRegion, imagesByRegion, platformString.nameToLowerCase());
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new CloudbreakImageNotFoundException(msg);
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
                            String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                    imagesByRegion, platform, translatedRegion)));
        }
    }

    private String selectAzureMarketplaceImage(String translatedRegion, Map<String, String> imagesByRegion, String platform, String accountId)
            throws CloudbreakImageNotFoundException {
        LOGGER.debug("Preferred region is 'default'. Platform: {}, Azure Marketplace images enabled.", platform);
        return findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion)
                .or(() -> supplyAlternativeImageWhenEntitlementAllows(translatedRegion, imagesByRegion, accountId))
                .orElseThrow(() -> new CloudbreakImageNotFoundException(
                        String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                imagesByRegion, platform, translatedRegion)));
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
                            String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                                    imagesByRegion, platform, translatedRegion)));
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Entry::getValue)
                .findFirst();
    }

    public Set<Component> getComponentsWithoutUserData(Stack stack, StatedImage statedImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getComponents(stack, statedImage, EnumSet.of(CDH_PRODUCT_DETAILS, CM_REPO_DETAILS));
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
        if (requestedComponents.contains(CDH_PRODUCT_DETAILS)) {
            addStackRepo(stack, components, catalogBasedImage);
            addPrewarmParcels(stack, statedImage, components);
        }
        if (requestedComponents.contains(CM_REPO_DETAILS)) {
            addCmRepo(stack, components, catalogBasedImage);
        }
        return components;
    }

    private void addPrewarmParcels(Stack stack, StatedImage statedImage, Set<Component> components) {
        Set<ClouderaManagerProduct> prewarmParcels = clouderaManagerProductTransformer.transform(statedImage.getImage(), false, true);
        components.addAll(componentConverter.fromClouderaManagerProductList(prewarmParcels, stack));
    }

    private void addStackRepo(Stack stack, Set<Component> components, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage)
            throws CloudbreakImageCatalogException {
        if (catalogBasedImage.getStackDetails() != null) {
            ImageStackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = determineStackType(stackDetails);
            Component stackRepoComponent = getStackComponent(stack, stackDetails, stackType, catalogBasedImage.getOsType());
            components.add(stackRepoComponent);
        }
    }

    private void addCmRepo(Stack stack, Set<Component> components, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage)
            throws CloudbreakImageCatalogException {
        if (catalogBasedImage.getStackDetails() != null) {
            ImageStackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = determineStackType(stackDetails);
            ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo(catalogBasedImage, stackType);
            components.add(new Component(CM_REPO_DETAILS, CM_REPO_DETAILS.name(), new Json(clouderaManagerRepo), stack));
        } else {
            LOGGER.debug("There are no stackDetails for stack {}, cannot determine CM repo version.", stack.getName());
        }
    }

    private void addImage(Stack stack, StatedImage statedImage, String imageName, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage,
            Set<Component> components) {
        Image image = new Image(imageName, new HashMap<>(), catalogBasedImage.getOs(), catalogBasedImage.getOsType(),
                statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), catalogBasedImage.getUuid(),
                catalogBasedImage.getPackageVersions(), catalogBasedImage.getDate(), catalogBasedImage.getCreated());
        components.add(new Component(IMAGE, IMAGE.name(), new Json(image), stack));
    }

    private Component getStackComponent(Stack stack, ImageStackDetails stackDetails, StackType stackType, String osType) {
        ComponentType componentType = stackType.getComponentType();
        if (CDH_PRODUCT_DETAILS.equals(componentType)) {
            ClouderaManagerProduct product = createProductRepo(stackDetails, osType);
            return new Component(componentType, componentType.name(), new Json(product), stack);
        } else {
            StackRepoDetails repo = createStackRepo(stackDetails);
            return new Component(componentType, componentType.name(), new Json(repo), stack);
        }
    }

    public Optional<ClouderaManagerRepo> getClouderaManagerRepo(com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog)
            throws CloudbreakImageCatalogException {
        return imgFromCatalog.getStackDetails() != null
                ? Optional.of(getClouderaManagerRepo(imgFromCatalog, determineStackType(imgFromCatalog.getStackDetails())))
                : Optional.empty();
    }

    private ClouderaManagerRepo getClouderaManagerRepo(com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog, StackType stackType)
            throws CloudbreakImageCatalogException {
        if (imgFromCatalog.getRepo() != null) {
            if (StackType.CDH.equals(stackType)) {
                ClouderaManagerRepo clouderaManagerRepo = imageToClouderaManagerRepoConverter.convert(imgFromCatalog);
                if (Objects.isNull(clouderaManagerRepo) || clouderaManagerRepo.getBaseUrl() == null) {
                    throw new CloudbreakImageCatalogException(
                            String.format("Cloudera Manager repo was not found in image for os: '%s'.", imgFromCatalog.getOsType()));
                }
                return clouderaManagerRepo;
            } else {
                throw new CloudbreakImageCatalogException(String.format("Invalid Ambari repo present in image catalog: '%s'.", imgFromCatalog.getRepo()));
            }
        } else {
            throw new CloudbreakImageCatalogException(String.format("Invalid Ambari repo present in image catalog: '%s'.", imgFromCatalog.getRepo()));
        }
    }

    public StackType determineStackType(ImageStackDetails stackDetails) throws CloudbreakImageCatalogException {
        String repoId = stackDetails.getRepo().getStack().get(StackRepoDetails.REPO_ID_TAG);
        Optional<StackType> stackType = EnumSet.allOf(StackType.class).stream().filter(st -> repoId.contains(st.name())).findFirst();
        if (stackType.isPresent()) {
            return stackType.get();
        } else {
            throw new CloudbreakImageCatalogException(String.format("Unsupported stack type: '%s'.", repoId));
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
