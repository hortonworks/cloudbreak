package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.CM_REPO_DETAILS;
import static com.sequenceiq.cloudbreak.common.type.ComponentType.IMAGE;
import static com.sequenceiq.cloudbreak.service.image.ImageCatalogService.CDP_DEFAULT_CATALOG_NAME;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

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

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        return componentConfigProviderService.getImage(stackId);
    }

    public String getCurrentImageCatalogName(Long stackId) throws CloudbreakImageNotFoundException {
        return getImage(stackId).getImageCatalogName();
    }

    public StatedImage getCurrentImage(Long stackId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = getImage(stackId);
        return imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId());
    }

    @Measure(ImageService.class)
    public Set<Component> create(Stack stack, String platformString, StatedImage imgFromCatalog)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {

        String region = stack.getRegion();
        LOGGER.debug("Determined image from catalog: {}", imgFromCatalog);
        String imageName = determineImageName(platformString, region, imgFromCatalog.getImage());
        LOGGER.debug("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                platformString, region, imageName, imgFromCatalog.getImageCatalogUrl());

        Set<Component> components = getComponents(stack, Map.of(), imgFromCatalog, EnumSet.of(IMAGE, CDH_PRODUCT_DETAILS, CM_REPO_DETAILS));
        componentConfigProviderService.store(components);
        return components;
    }

    //CHECKSTYLE:OFF
    @Measure(ImageService.class)
    public StatedImage determineImageFromCatalog(Long workspaceId, ImageSettingsV4Request imageSettings, String platformString,
            Blueprint blueprint, boolean useBaseImage, boolean baseImageEnabled,
            User user, Predicate<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> imagePredicate)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {

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
            operatingSystems = getSupportedOperatingSystems(workspaceId, imageSettings, clusterVersion, platformString);
        } catch (Exception ex) {
            throw new CloudbreakImageCatalogException(ex);
        }

        ImageCatalog imageCatalog = getImageCatalogFromRequestOrDefault(workspaceId, imageSettings, user);
        ImageFilter imageFilter = new ImageFilter(imageCatalog, ImmutableSet.of(platformString), null, baseImageEnabled, operatingSystems, clusterVersion);
        LOGGER.info("Image id is not specified for the stack.");
        if (baseImageEnabled && useBaseImage) {
            LOGGER.info("Trying to select a base image.");
            return imageCatalogService.getLatestBaseImageDefaultPreferred(imageFilter, imagePredicate);
        }

        LOGGER.info("Trying to select a prewarmed image.");
        return imageCatalogService.getImagePrewarmedDefaultPreferred(imageFilter, imagePredicate);
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

    private Set<String> getSupportedOperatingSystems(Long workspaceId, ImageSettingsV4Request imageSettings, String clusterVersion, String platform)
            throws Exception {
        String imageCatalogName = imageSettings != null ? imageSettings.getCatalog() : null;
        Set<String> operatingSystems = stackMatrixService.getSupportedOperatingSystems(workspaceId, clusterVersion, platform, imageCatalogName);
        if (imageSettings != null && StringUtils.isNotEmpty(imageSettings.getOs())) {
            if (operatingSystems.isEmpty()) {
                operatingSystems = Collections.singleton(imageSettings.getOs());
            } else {
                operatingSystems = operatingSystems.stream().filter(os -> os.equalsIgnoreCase(imageSettings.getOs())).collect(Collectors.toSet());
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

    public String determineImageName(String platformString, String region, com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog)
            throws CloudbreakImageNotFoundException {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        String translatedRegion = cloudPlatformConnectors.getDefault(platform(platformString.toUpperCase())).regionToDisplayName(region);
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            Optional<String> imageNameOpt = findStringKeyWithEqualsIgnoreCase(translatedRegion, imagesByRegion);
            if (!imageNameOpt.isPresent()) {
                imageNameOpt = findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
            }
            if (imageNameOpt.isPresent()) {
                return imageNameOpt.get();
            }
            String msg = String.format("Virtual machine image couldn't be found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                    imgFromCatalog, platformString, translatedRegion);
            throw new CloudbreakImageNotFoundException(msg);
        }
        String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                imgFromCatalog, platformString);
        throw new CloudbreakImageNotFoundException(msg);
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Entry::getValue)
                .findFirst();
    }

    public Set<Component> getComponents(Stack stack, Map<InstanceGroupType, String> userData, StatedImage statedImage,
            EnumSet<ComponentType> requestedComponents)
            throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        Set<Component> components = new HashSet<>();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage = statedImage.getImage();
        if (requestedComponents.contains(IMAGE)) {
            String imageName = determineImageName(stack.cloudPlatform(), stack.getRegion(), statedImage.getImage());
            addImage(stack, userData, statedImage, imageName, catalogBasedImage, components);
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
            StackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = determineStackType(stackDetails);
            Component stackRepoComponent = getStackComponent(stack, stackDetails, stackType, catalogBasedImage.getOsType());
            components.add(stackRepoComponent);
        }
    }

    private void addCmRepo(Stack stack, Set<Component> components, com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage)
            throws CloudbreakImageCatalogException {
        if (catalogBasedImage.getStackDetails() != null) {
            StackDetails stackDetails = catalogBasedImage.getStackDetails();
            StackType stackType = determineStackType(stackDetails);
            ClouderaManagerRepo clouderaManagerRepo = getClouderaManagerRepo(catalogBasedImage, stackType);
            components.add(new Component(CM_REPO_DETAILS, CM_REPO_DETAILS.name(), new Json(clouderaManagerRepo), stack));
        } else {
            LOGGER.debug("There are no stackDetails for stack {}, cannot determine CM repo version.", stack.getName());
        }
    }

    private void addImage(Stack stack, Map<InstanceGroupType, String> userData, StatedImage statedImage, String imageName,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogBasedImage, Set<Component> components) {
        Image image = new Image(imageName, userData, catalogBasedImage.getOs(), catalogBasedImage.getOsType(),
                statedImage.getImageCatalogUrl(), statedImage.getImageCatalogName(), catalogBasedImage.getUuid(),
                catalogBasedImage.getPackageVersions());
        components.add(new Component(IMAGE, IMAGE.name(), new Json(image), stack));
    }

    private Component getStackComponent(Stack stack, StackDetails stackDetails, StackType stackType, String osType) {
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
                ClouderaManagerRepo clouderaManagerRepo = conversionService.convert(imgFromCatalog, ClouderaManagerRepo.class);
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

    public StackType determineStackType(StackDetails stackDetails) throws CloudbreakImageCatalogException {
        String repoId = stackDetails.getRepo().getStack().get(StackRepoDetails.REPO_ID_TAG);
        Optional<StackType> stackType = EnumSet.allOf(StackType.class).stream().filter(st -> repoId.contains(st.name())).findFirst();
        if (stackType.isPresent()) {
            return stackType.get();
        } else {
            throw new CloudbreakImageCatalogException(String.format("Unsupported stack type: '%s'.", repoId));
        }

    }

    private StackRepoDetails createStackRepo(StackDetails stack) {
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(stack.getVersion());
        repo.setStack(stack.getRepo().getStack());
        repo.setUtil(stack.getRepo().getUtil());
        return repo;
    }

    private ClouderaManagerProduct createProductRepo(StackDetails stack, String osType) {
        ClouderaManagerProduct cmProduct = new ClouderaManagerProduct();
        Map<String, String> stackInfo = stack.getRepo().getStack();
        cmProduct.
                withVersion(stackInfo.get(StackRepoDetails.REPOSITORY_VERSION)).
                withName(stackInfo.get(StackRepoDetails.REPO_ID_TAG).split("-")[0]).
                withParcel(stackInfo.get(osType));
        return cmProduct;
    }

    public void decorateImageWithUserDataForStack(Stack stack, Map<InstanceGroupType, String> userData) throws CloudbreakImageNotFoundException {
        Image image = componentConfigProviderService.getImage(stack.getId());
        image.setUserdata(userData);
        Component imageComponent = new Component(IMAGE, IMAGE.name(), new Json(image), stack);
        componentConfigProviderService.replaceImageComponentWithNew(imageComponent);
    }
}
