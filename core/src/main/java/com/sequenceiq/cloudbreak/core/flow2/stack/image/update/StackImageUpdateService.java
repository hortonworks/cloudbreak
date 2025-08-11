package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import java.util.HashMap;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackTypeResolver;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeCondition;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@Service
public class StackImageUpdateService {

    public static final String MIN_VERSION = "2.8.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageUpdateService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackTypeResolver stackTypeResolver;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private PackageVersionChecker packageVersionChecker;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackImageService stackImageService;

    @Inject
    private ImageService imageService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private CentosToRedHatUpgradeCondition centosToRedHatUpgradeCondition;

    public StatedImage getNewImageIfVersionsMatch(StackDtoDelegate stack, String newImageId, String imageCatalogName, String imageCatalogUrl) {
        try {
            restRequestThreadLocalService.setWorkspaceId(stack.getWorkspaceId());

            Image currentImage = stackImageService.getCurrentImage(stack.getId());

            StatedImage newImage = getNewImage(stack.getWorkspace().getId(), newImageId, imageCatalogName, imageCatalogUrl, currentImage);

            if (!isCloudPlatformMatches(stack, newImage)) {
                String message = messagesService.getMessage(Msg.CLOUDPLATFORM_DIFFERENT.code(),
                        Lists.newArrayList(String.join(",", newImage.getImage().getImageSetsByProvider().keySet()), stack.getCloudPlatform()));
                LOGGER.debug(message);
                throw new OperationException(message);
            }

            if (!isOsVersionsMatch(currentImage, newImage) && !isCentosToRedhatUpgrade(stack, newImage)) {
                String message = messagesService.getMessage(Msg.OSVERSION_DIFFERENT.code(),
                        Lists.newArrayList(newImage.getImage().getOs(), newImage.getImage().getOsType(), currentImage.getOs(), currentImage.getOsType()));
                LOGGER.debug("Image change not permitted because: {} Current image: {}, new image: {}", message, currentImage, newImage.getImage());
                throw new OperationException(message);
            }

            if (!isStackMatchIfPrewarmed(newImage)) {
                String message = "Stack versions don't match on prewarmed image with cluster's";
                LOGGER.debug(message);
                throw new OperationException(message);
            }

            return newImage;

        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info("Cloudbreak Image not found", e);
            throw new BadRequestException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.info("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    private boolean isCloudPlatformMatches(StackDtoDelegate stack, StatedImage newImage) {
        ImageCatalogPlatform platformString = platformStringTransformer
                .getPlatformStringForImageCatalog(stack.getCloudPlatform(), stack.getPlatformVariant());
        return newImage.getImage().getImageSetsByProvider()
                .keySet()
                .stream()
                .anyMatch(key -> key.equalsIgnoreCase(platformString.nameToLowerCase()));
    }

    private boolean isOsVersionsMatch(Image currentImage, StatedImage newImage) {
        return newImage.getImage().getOs().equalsIgnoreCase(currentImage.getOs())
                && newImage.getImage().getOsType().equalsIgnoreCase(currentImage.getOsType());
    }

    private StatedImage getNewImage(Long workspaceId, String newImageId, String imageCatalogName, String imageCatalogUrl, Image currentImage)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage newImage;
        newImage = StringUtils.isNotBlank(imageCatalogName) && StringUtils.isNotBlank(imageCatalogUrl)
                ? imageCatalogService.getImage(workspaceId, imageCatalogUrl, imageCatalogName, newImageId)
                : imageCatalogService.getImage(workspaceId, currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), newImageId);
        return newImage;
    }

    public Image getImageFromStated(StackDtoDelegate stack, StatedImage image) throws CloudbreakImageNotFoundException {
        String imageName = imageService.determineImageName(
                stack.getCloudPlatform(),
                platformStringTransformer.getPlatformStringForImageCatalog(stack.getCloudPlatform(), stack.getPlatformVariant()),
                stack.getRegion(),
                image.getImage()
        );
        return new Image(imageName, new HashMap<>(), image.getImage().getOs(), image.getImage().getOsType(), image.getImage().getArchitecture(),
                image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImage().getUuid(),
                image.getImage().getPackageVersions(), image.getImage().getDate(), image.getImage().getCreated(), image.getImage().getTags());
    }

    public boolean isCbVersionOk(StackDtoDelegate stack) {
        CloudbreakDetails cloudbreakDetails = componentConfigProviderService.getCloudbreakDetails(stack.getId());
        VersionComparator versionComparator = new VersionComparator();
        String version = StringUtils.substringBefore(cloudbreakDetails.getVersion(), "-");
        int compare = versionComparator.compare(() -> version, () -> MIN_VERSION);
        return compare >= 0;
    }

    public boolean isStackMatchIfPrewarmed(StatedImage image) {
        if (image.getImage().getStackDetails() != null) {
            try {
                StackType imageStackType = stackTypeResolver.determineStackType(image.getImage().getStackDetails());
                return imageStackType == StackType.CDH;
            } catch (CloudbreakImageCatalogException e) {
                throw new CloudbreakServiceException(e);
            }
        }
        return true;
    }

    public CheckResult checkPackageVersions(StackDtoDelegate stack, StatedImage newImage) {
        List<InstanceMetadataView> instanceMetaDataSet = stack.getAllAvailableInstances();

        CheckResult instanceHaveMultipleVersionResult = packageVersionChecker.checkInstancesHaveMultiplePackageVersions(instanceMetaDataSet);
        if (instanceHaveMultipleVersionResult.getStatus() == EventStatus.FAILED) {
            LOGGER.debug("Check packages - Instances do have multiple package versions: {}", instanceHaveMultipleVersionResult);
            return instanceHaveMultipleVersionResult;
        }

        CheckResult instancesHaveAllMandatoryPackageVersionResult = packageVersionChecker.checkInstancesHaveAllMandatoryPackageVersion(instanceMetaDataSet);
        if (instancesHaveAllMandatoryPackageVersionResult.getStatus() == EventStatus.FAILED) {
            LOGGER.debug("Check packages - Instances are missing one or more package versions: {}", instancesHaveAllMandatoryPackageVersionResult);
            return instancesHaveAllMandatoryPackageVersionResult;
        }

        CheckResult compareImageAndInstancesMandatoryPackageVersion =
                packageVersionChecker.compareImageAndInstancesMandatoryPackageVersion(newImage, instanceMetaDataSet);
        if (compareImageAndInstancesMandatoryPackageVersion.getStatus() == EventStatus.FAILED) {
            LOGGER.debug("Check packages - Image and instances mandatory packages do differ: {}", compareImageAndInstancesMandatoryPackageVersion);
            return compareImageAndInstancesMandatoryPackageVersion;
        }

        return CheckResult.ok();
    }

    public boolean isValidImage(Stack stack, String newImageId, String imageCatalogName, String imageCatalogUrl) {
        if (isCbVersionOk(stack)) {
            try {
                Image currentImage = stackImageService.getCurrentImage(stack.getId());
                StatedImage newImage = getNewImage(stack.getWorkspace().getId(), newImageId, imageCatalogName, imageCatalogUrl, currentImage);

                boolean cloudPlatformMatches = isCloudPlatformMatches(stack, newImage);
                boolean osVersionsMatch = isOsVersionsMatch(currentImage, newImage);
                boolean stackMatchIfPrewarmed = isStackMatchIfPrewarmed(newImage);
                CheckResult checkPackageVersionsStatus = checkPackageVersions(stack, newImage);

                boolean aggregatedValidationResult = cloudPlatformMatches && osVersionsMatch && stackMatchIfPrewarmed;
                if (!aggregatedValidationResult) {
                    LOGGER.info("Image validation for {}:\n "
                                    + "Valid platform? {}\n "
                                    + "Valid os? {}\n "
                                    + "Valid stack (prewarmed only)? {}",
                            newImageId,
                            cloudPlatformMatches,
                            osVersionsMatch,
                            stackMatchIfPrewarmed);
                }
                if (checkPackageVersionsStatus.getStatus() != EventStatus.OK) {
                    LOGGER.info("Image validation for {}:\n "
                                    + "Valid package versions? {}",
                            newImageId,
                            checkPackageVersionsStatus);
                }
                return aggregatedValidationResult;
            } catch (CloudbreakImageNotFoundException e) {
                LOGGER.debug("Cloudbreak Image not found", e);
                return false;
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.debug("Cloudbreak Image catalog error", e);
                return false;
            }
        }
        return false;
    }

    private boolean isCentosToRedhatUpgrade(StackDtoDelegate stack, StatedImage newImage) {
        return centosToRedHatUpgradeCondition.isCentosToRedhatUpgrade(stack.getId(), newImage.getImage());
    }

    private enum Msg {
        CLOUDPLATFORM_DIFFERENT("stack.image.update.cloudplatform.different"),
        OSVERSION_DIFFERENT("stack.image.update.osversion.different");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

}