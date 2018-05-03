package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
@Transactional
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    private static final String DEFAULT_REGION = "default";

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private BlueprintUtils blueprintUtils;

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        return componentConfigProvider.getImage(stackId);
    }

    @Transactional(TxType.NEVER)
    public void create(Stack stack, PlatformParameters params, String imageCatalog, Optional<String> imageId, Optional<Blueprint> blueprint)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        try {
            Platform platform = platform(stack.cloudPlatform());
            String platformString = platform(stack.cloudPlatform()).value().toLowerCase();
            String region = stack.getRegion();
            String cbPrivKey = stack.getSecurityConfig().getCloudbreakSshPrivateKeyDecoded();
            String cbSshKey = stack.getSecurityConfig().getCloudbreakSshPublicKeyDecoded();
            byte[] cbSshKeyDer = PkiUtil.getPublicKeyDer(cbPrivKey);
            String sshUser = stack.getStackAuthentication().getLoginUserName();
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(platform, cbSshKeyDer, cbSshKey, sshUser, params,
                    stack.getSecurityConfig().getSaltBootPassword());

            StatedImage imgFromCatalog = determineImageFromCatalog(imageId, platformString, imageCatalog, blueprint);
            LOGGER.info("Determined image from catalog: {}", imgFromCatalog);

            String imageName = determineImageName(platformString, region, imgFromCatalog.getImage());
            LOGGER.info("Selected VM image for CloudPlatform '{}' and region '{}' is: {} from: {} image catalog",
                    platformString, region, imageName, imgFromCatalog.getImageCatalogUrl());

            List<Component> components = getComponents(stack, userData, imgFromCatalog.getImage(), imageName,
                    imgFromCatalog.getImageCatalogUrl(),
                    imgFromCatalog.getImageCatalogName(),
                    imgFromCatalog.getImage().getUuid());
            componentConfigProvider.store(components);
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to create json", e);
        }
    }

    private StatedImage determineImageFromCatalog(Optional<String> imageId, String platformString, String catalogName, Optional<Blueprint> blueprint)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage statedImage;
        if (imageId.isPresent()) {
            statedImage = imageCatalogService.getImageByCatalogName(imageId.get(), catalogName);
        } else {
            String clusterType = ImageCatalogService.UNDEFINED;
            String clusterVersion = ImageCatalogService.UNDEFINED;
            if (blueprint.isPresent()) {
                try {
                    JsonNode root = JsonUtil.readTree(blueprint.get().getBlueprintText());
                    clusterType = blueprintUtils.getBlueprintStackName(root);
                    clusterVersion = blueprintUtils.getBlueprintHdpVersion(root);
                } catch (IOException ex) {
                    LOGGER.warn("Can not initiate default hdp info: ", ex);
                }
            }
            LOGGER.warn(
                    "Image id hasn't been specified for the stack, falling back to a prewarmed image of {}-{} or to a base image if prewarmed doesn't exist.",
                    clusterType, clusterVersion);
            statedImage = imageCatalogService.getDefaultImage(platformString, clusterType, clusterVersion);
        }
        return statedImage;
    }

    private String determineImageName(String platformString, String region, com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog)
            throws CloudbreakImageNotFoundException {
        String imageName;
        Optional<Map<String, String>> imagesForPlatform = findStringKeyWithEqualsIgnoreCase(platformString, imgFromCatalog.getImageSetsByProvider());
        if (imagesForPlatform.isPresent()) {
            Map<String, String> imagesByRegion = imagesForPlatform.get();
            Optional<String> imageNameOpt = findStringKeyWithEqualsIgnoreCase(region, imagesByRegion);
            if (!imageNameOpt.isPresent()) {
                imageNameOpt = findStringKeyWithEqualsIgnoreCase(DEFAULT_REGION, imagesByRegion);
            }
            if (imageNameOpt.isPresent()) {
                imageName = imageNameOpt.get();
            } else {
                String msg = String.format("Virtual machine image couldn't found in image: '%s' for the selected platform: '%s' and region: '%s'.",
                        imgFromCatalog, platformString, region);
                throw new CloudbreakImageNotFoundException(msg);
            }
        } else {
            String msg = String.format("The selected image: '%s' doesn't contain virtual machine image for the selected platform: '%s'.",
                    imgFromCatalog, platformString);
            throw new CloudbreakImageNotFoundException(msg);
        }
        return imageName;
    }

    private <T> Optional<T> findStringKeyWithEqualsIgnoreCase(String key, Map<String, T> map) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private List<Component> getComponents(Stack stack, Map<InstanceGroupType, String> userData,
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog,
            String imageName, String imageCatalogUrl, String imageCatalogName, String imageId) throws JsonProcessingException, CloudbreakImageCatalogException {
        List<Component> components = new ArrayList<>();
        Image image = new Image(imageName, userData, imgFromCatalog.getOs(), imgFromCatalog.getOsType(), imageCatalogUrl, imageCatalogName, imageId);
        Component imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
        components.add(imageComponent);

        if (imgFromCatalog.getStackDetails() != null) {
            components.add(getAmbariComponent(stack, imgFromCatalog));
            StackDetails stackDetails = imgFromCatalog.getStackDetails();

            Component stackRepoComponent;
            if (!imgFromCatalog.getStackDetails().getRepo().getKnox().isEmpty()) {
                StackRepoDetails hdfRepo = createHDFRepo(stackDetails);
                stackRepoComponent = new Component(ComponentType.HDF_REPO_DETAILS, ComponentType.HDF_REPO_DETAILS.name(), new Json(hdfRepo), stack);
            } else {
                StackRepoDetails repo = createHDPRepo(stackDetails);
                stackRepoComponent = new Component(ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name(), new Json(repo), stack);
            }
            components.add(stackRepoComponent);
        }
        return components;
    }

    private Component getAmbariComponent(Stack stack, com.sequenceiq.cloudbreak.cloud.model.catalog.Image imgFromCatalog)
            throws JsonProcessingException, CloudbreakImageCatalogException {
        if (imgFromCatalog.getRepo() != null) {
            AmbariRepo ambariRepo = conversionService.convert(imgFromCatalog, AmbariRepo.class);
            if (ambariRepo.getBaseUrl() == null) {
                throw new CloudbreakImageCatalogException(String.format("Ambari repo not found in image for os: '%s'.", imgFromCatalog.getOsType()));
            }
            return new Component(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(), new Json(ambariRepo), stack);
        } else {
            throw new CloudbreakImageCatalogException(String.format("Invalid Ambari repo present in image catalog: '%s'.", imgFromCatalog.getRepo()));
        }
    }

    private StackRepoDetails createHDPRepo(StackDetails hdpStack) {
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(hdpStack.getVersion());
        repo.setStack(hdpStack.getRepo().getStack());
        repo.setUtil(hdpStack.getRepo().getUtil());
        return repo;
    }

    private StackRepoDetails createHDFRepo(StackDetails hdfStack) {
        com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails hdfRepo = hdfStack.getRepo();
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(hdfStack.getVersion());
        repo.setStack(hdfRepo.getStack());
        repo.setUtil(hdfRepo.getUtil());
        repo.setKnox(hdfRepo.getKnox());
        return repo;
    }
}
