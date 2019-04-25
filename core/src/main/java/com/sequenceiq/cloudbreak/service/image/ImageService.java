package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private UserDataBuilder userDataBuilder;

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

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        return componentConfigProviderService.getImage(stackId);
    }

    @Measure(ImageService.class)
    public void create(Stack stack, String platformString, PlatformParameters params, StatedImage imgFromCatalog)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        try {
            Platform platform = platform(stack.cloudPlatform());
            String region = stack.getRegion();
            SecurityConfig securityConfig = stack.getSecurityConfig();
            SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
            String cbPrivKey = saltSecurityConfig.getSaltBootSignPrivateKey();
            byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(cbPrivKey)));
            String sshUser = stack.getStackAuthentication().getLoginUserName();
            String cbCert = securityConfig.getClientCert();
            String saltBootPassword = saltSecurityConfig.getSaltBootPassword();
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(platform, cbSshKeyDer, sshUser, params, saltBootPassword, cbCert);

            LOGGER.debug("Determined image from catalog: {}", imgFromCatalog);

            String imageName = determineImageName(platformString, region, imgFromCatalog.getImage());
            LOGGER.debug("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                    platformString, region, imageName, imgFromCatalog.getImageCatalogUrl());

            List<Component> components = getComponents(stack, userData, imgFromCatalog.getImage(), imageName,
                    imgFromCatalog.getImageCatalogUrl(),
                    imgFromCatalog.getImageCatalogName(),
                    imgFromCatalog.getImage().getUuid());
            componentConfigProviderService.store(components);
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to create json", e);
        }
    }

    //CHECKSTYLE:OFF
    @Measure(ImageService.class)
    public StatedImage determineImageFromCatalog(Long workspaceId, ImageSettingsV4Request image, String platformString,
            Blueprint blueprint, boolean useBaseImage, User user) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        String clusterType = ImageCatalogService.UNDEFINED;
        String clusterVersion = ImageCatalogService.UNDEFINED;
        if (blueprint != null) {
            try {
                JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
                if (blueprintUtils.isAmbariBlueprint(blueprint.getBlueprintText())) {
                    clusterType = blueprintUtils.getBlueprintStackName(root);
                    clusterVersion = blueprintUtils.getBlueprintStackVersion(root);
                } else {
                    clusterType = "CDH";
                    clusterVersion = blueprintUtils.getCDHStackVersion(root);
                }
            } catch (IOException ex) {
                LOGGER.warn("Can not initiate default hdp info: ", ex);
            }
        }
        Set<String> operatingSystems = stackMatrixService.getSupportedOperatingSystems(clusterType, clusterVersion);
        if (image != null && !StringUtils.isEmpty(image.getOs())) {
            operatingSystems = operatingSystems.stream().filter(os -> os.equalsIgnoreCase(image.getOs())).collect(Collectors.toSet());
        }
        if (image != null && image.getId() != null) {
            return imageCatalogService.getImageByCatalogName(workspaceId, image.getId(), image.getCatalog());
        }
        if (useBaseImage) {
            LOGGER.debug("Image id isn't specified for the stack, falling back to a base image, because repo information is provided");
            return imageCatalogService.getLatestBaseImageDefaultPreferred(platformString, operatingSystems, user);
        }
        LOGGER.debug("Image id isn't specified for the stack, falling back to a prewarmed "
                + "image of {}-{} or to a base image if prewarmed doesn't exist", clusterType, clusterVersion);
        return imageCatalogService.getPrewarmImageDefaultPreferred(platformString, clusterType, clusterVersion, operatingSystems, user);
    }
    //CHECKSTYLE:ON

    public String determineImageName(String platformString, String region, com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog)
            throws CloudbreakImageNotFoundException {
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            Optional<String> imageNameOpt = findStringKeyWithEqualsIgnoreCase(region, imagesByRegion);
            if (!imageNameOpt.isPresent()) {
                imageNameOpt = findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
            }
            if (imageNameOpt.isPresent()) {
                return imageNameOpt.get();
            }
            String msg = String.format("Virtual machine image couldn't found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                    imgFromCatalog, platformString, region);
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

    private List<Component> getComponents(Stack stack, Map<InstanceGroupType, String> userData,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog,
            String imageName, String imageCatalogUrl, String imageCatalogName, String imageId) throws JsonProcessingException, CloudbreakImageCatalogException {
        List<Component> components = new ArrayList<>();
        Image image = new Image(imageName, userData, imgFromCatalog.getOs(), imgFromCatalog.getOsType(), imageCatalogUrl, imageCatalogName, imageId,
                imgFromCatalog.getPackageVersions());
        Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
        components.add(imageComponent);
        if (imgFromCatalog.getStackDetails() != null) {
            StackDetails stackDetails = imgFromCatalog.getStackDetails();
            StackType stackType = determineStackType(stackDetails);
            Component stackRepoComponent = getStackComponent(stack, stackDetails, stackType, imgFromCatalog.getOsType());
            components.add(stackRepoComponent);
            components.add(getClusterManagerComponent(stack, imgFromCatalog, stackType));
        }
        return components;
    }

    private Component getStackComponent(Stack stack, StackDetails stackDetails, StackType stackType, String osType) throws JsonProcessingException {
        ComponentType componentType = stackType.getComponentType();
        if (ComponentType.CDH_PRODUCT_DETAILS.equals(componentType)) {
            ClouderaManagerProduct product = createProductRepo(stackDetails, osType);
            return new Component(componentType, componentType.name(), new Json(product), stack);
        } else {
            StackRepoDetails repo = createStackRepo(stackDetails);
            return new Component(componentType, componentType.name(), new Json(repo), stack);
        }
    }

    private Component getClusterManagerComponent(Stack stack, com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog, StackType stackType)
            throws JsonProcessingException, CloudbreakImageCatalogException {
        if (imgFromCatalog.getRepo() != null) {
            if (StackType.CDH.equals(stackType)) {
                ClouderaManagerRepo clouderaManagerRepo = conversionService.convert(imgFromCatalog, ClouderaManagerRepo.class);
                if (clouderaManagerRepo.getBaseUrl() == null) {
                    throw new CloudbreakImageCatalogException(
                            String.format("Cloudera Manager repo was not found in image for os: '%s'.", imgFromCatalog.getOsType()));
                }
                return new Component(ComponentType.CM_REPO_DETAILS, ComponentType.CM_REPO_DETAILS.name(), new Json(clouderaManagerRepo), stack);
            } else {
                AmbariRepo ambariRepo = conversionService.convert(imgFromCatalog, AmbariRepo.class);
                if (ambariRepo.getBaseUrl() == null) {
                    throw new CloudbreakImageCatalogException(String.format("Ambari repo was not found in image for os: '%s'.", imgFromCatalog.getOsType()));
                }
                return new Component(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(), new Json(ambariRepo), stack);
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
        repo.setMpacks(stack.getMpackList().stream().map(icmpack -> {
            ManagementPackComponent mpack = new ManagementPackComponent();
            mpack.setMpackUrl(icmpack.getMpackUrl());
            mpack.setStackDefault(true);
            mpack.setPreInstalled(true);
            return mpack;
        }).collect(Collectors.toList()));
        if (!stack.getMpackList().isEmpty()) {
            // Backward compatibility for the previous UI versions
            repo.getStack().put(StackRepoDetails.MPACK_TAG, stack.getMpackList().get(0).getMpackUrl());
        }
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
}
