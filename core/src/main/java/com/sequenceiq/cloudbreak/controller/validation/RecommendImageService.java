package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Component
public class RecommendImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendImageService.class);

    @Inject
    private ImageService imageService;

    @Inject
    private UserService userService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    public Image recommendImage(Long workspaceId, CloudbreakUser cloudbreakUser, ImageSettingsV4Request imageSettings, String region, String blueprintName,
            CloudPlatformVariant cloudPlatform, Architecture architecture) {
        String platform = cloudPlatform.getPlatform().getValue();
        Blueprint blueprint = determineBlueprint(workspaceId, blueprintName);
        User user = userService.getOrCreate(cloudbreakUser);
        try {
            StatedImage statedImage = imageService.determineImageFromCatalog(
                    workspaceId, imageSettings, architecture, platform, null, blueprint, false, false, user, image -> true);
            LOGGER.debug("Determined stated image from catalog: {}", statedImage);
            ImageCatalogPlatform imageCatalogPlatform =
                    platformStringTransformer.getPlatformStringForImageCatalog(platform, cloudPlatform.getVariant().getValue());
            com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogImage = statedImage.getImage();
            String imageName = imageService.determineImageName(platform, imageCatalogPlatform, region, catalogImage);
            LOGGER.debug("Recommended image name: {}", imageName);
            return new Image(imageName,
                    null,
                    catalogImage.getOs(),
                    catalogImage.getOsType(),
                    catalogImage.getArchitecture(),
                    statedImage.getImageCatalogUrl(),
                    statedImage.getImageCatalogName(),
                    catalogImage.getUuid(),
                    catalogImage.getPackageVersions(),
                    catalogImage.getDate(),
                    catalogImage.getCreated(),
                    catalogImage.getTags());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.error("Validate recommended image failed. Failed to select image.", e);
            throw new BadRequestException("Validate recommended image failed. Failed to select image: " + e.getMessage());
        }
    }

    private Blueprint determineBlueprint(Long workspaceId, String bpName) {
        Set<Blueprint> blueprints = blueprintService.findAllByWorkspaceId(workspaceId);
        return blueprints.stream()
                .filter(cd -> cd.getName().equals(bpName))
                .findFirst().orElseThrow(() -> new BadRequestException(String.format("Cluster definition with name %s not found!", bpName)));
    }
}
