package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfoFactory;
import com.sequenceiq.cloudbreak.service.upgrade.image.locked.LockedComponentChecker;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;

@Service
public class OsChangeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsChangeUtil.class);

    @Inject
    private LockedComponentChecker lockedComponentChecker;

    @Inject
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Inject
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private OsChangeUpgradeCondition osChangeUpgradeCondition;

    @Inject
    private CurrentImageUsageCondition currentImageUsageCondition;

    public boolean isHelperImageAvailable(List<Image> images, Image targetImage, Set<String> stackRelatedParcels, OsType currentOsType) {
        Optional<Image> helperImage = findHelperImage(images, targetImage, stackRelatedParcels, currentOsType);
        return helperImage.isPresent();
    }

    public boolean isHelperImageAvailable(long resourceId, String imageCatalogName, Image targetImage, Set<String> stackRelatedParcels, OsType currentOsType) {
        StackDto stack = stackDtoService.getById(resourceId);
        List<Image> cdhImages = getAllCdhImages(stack, imageCatalogName);
        Optional<Image> helperImage = findHelperImage(cdhImages, targetImage, stackRelatedParcels, currentOsType);
        return helperImage.isPresent();
    }

    public Optional<Image> findHelperImageIfNecessary(String targetImageId, long resourceId) {
        try {
            UpgradeImageInfo upgradeImageInfo = upgradeImageInfoFactory.create(targetImageId, resourceId);
            StatedImage targetImage = upgradeImageInfo.targetStatedImage();
            Set<OsType> osUsedByInstances = currentImageUsageCondition.getOSUsedByInstances(resourceId);
            if (osChangeUpgradeCondition.isNextMajorOsImage(osUsedByInstances, targetImage.getImage())) {
                OsType currentOsType = osChangeUpgradeCondition.getPreviousOs(OsType.getByOsTypeStringWithCentos7Fallback(targetImage.getImage().getOsType()))
                        .get();
                StackDto stack = stackDtoService.getById(resourceId);
                List<Image> cdhImages = getAllCdhImages(stack, targetImage.getImageCatalogName());
                Map<String, String> stackRelatedParcels = imageFilterParamsFactory.getStackRelatedParcels(stack);
                Optional<Image> helperImage = findHelperImage(cdhImages, targetImage.getImage(), stackRelatedParcels.keySet(), currentOsType);
                LOGGER.debug("Helper image {} found", helperImage.isPresent() ? helperImage.get().getUuid() : "not");
                return helperImage;
            }
            return Optional.empty();
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.error("Error during finding centOS image for RedHat upgrade: ", e);
            return Optional.empty();
        }
    }

    public boolean isOsUpgradePermitted(Long stackId, com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image image,
            Map<String, String> stackRelatedParcels) {
        return isOsChangeAllowed(stackId, image) && containsSameComponentVersions(image, getCmBuildNumber(currentImage), stackRelatedParcels);
    }

    private boolean isOsChangeAllowed(Long stackId, Image image) {
        return osChangeUpgradeCondition.isNextMajorOsImage(stackId, image);
    }

    private Optional<Image> findHelperImage(List<Image> images, Image targetImage, Set<String> stackRelatedParcels, OsType currentOsType) {
        Map<String, String> requiredParcelsFromTargetImage = getRequiredParcelsFromTargetImage(targetImage, stackRelatedParcels);
        return images.stream()
                .filter(image -> currentOsType.matches(image.getOs(), image.getOsType()) &&
                        Architecture.fromStringWithFallback(image.getArchitecture()) == Architecture.fromStringWithFallback(targetImage.getArchitecture()) &&
                        containsSameComponentVersions(image, targetImage.getCmBuildNumber(), requiredParcelsFromTargetImage))
                .findFirst();
    }

    private boolean containsSameComponentVersions(Image targetImage, String cmBuildNumber, Map<String, String> stackRelatedParcels) {
        return lockedComponentChecker.isUpgradePermitted(targetImage, stackRelatedParcels, cmBuildNumber);

    }

    private Map<String, String> getRequiredParcelsFromTargetImage(Image targetImage, Set<String> stackRelatedParcels) {
        Map<String, String> targetImagePackageVersions = targetImage.getPackageVersions();
        Map<String, String> stackRelatedParcelsFromTargetImage = new HashMap<>();
        Optional.of(targetImage.getStackDetails().getRepo().getStack().get(REPOSITORY_VERSION))
                .ifPresent(cdhVersion -> stackRelatedParcelsFromTargetImage.put(CDH.name(), cdhVersion));
        stackRelatedParcelsFromTargetImage.putAll(targetImagePackageVersions.entrySet().stream()
                .filter(entry -> stackRelatedParcels.stream().anyMatch(stackRelatedParcel -> entry.getKey().equalsIgnoreCase(stackRelatedParcel)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        LOGGER.debug("The following parcels are required from the target image: {}", stackRelatedParcelsFromTargetImage);
        return stackRelatedParcelsFromTargetImage;
    }

    private String getCmBuildNumber(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return currentImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER);
    }

    private List<Image> getAllCdhImages(StackDto stack, String imageCatalogName) {
        try {
            return imageCatalogService.getAllCdhImages(ThreadBasedUserCrnProvider.getAccountId(), stack.getWorkspaceId(),
                    imageCatalogName, platformStringTransformer.getPlatformStringForImageCatalogSet(stack.getCloudPlatform(), stack.getPlatformVariant()));
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Failed to retrieve images from catalog", e);
            return Collections.emptyList();
        }
    }
}