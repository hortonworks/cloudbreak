package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.image.ClusterUpgradeTargetImageService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationResult;

@Service
public class MixedPackageVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixedPackageVersionService.class);

    @Inject
    private ClusterUpgradeTargetImageService clusterUpgradeTargetImageService;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Inject
    private TargetImageAwareMixedPackageVersionService targetImageAwareMixedPackageVersionService;

    @Inject
    private CandidateImageAwareMixedPackageVersionService candidateImageAwareMixedPackageVersionService;

    public void validatePackageVersions(Long stackId, CmSyncOperationResult syncResult, Set<Image> candidateImages) {
        Optional<String> activeCmVersion = syncResult.getCmRepoSyncOperationResult().getInstalledCmVersion();
        if (activeCmVersion.isPresent()) {
            Set<ParcelInfo> activeParcels = syncResult.getCmParcelSyncOperationResult().getActiveParcels();
            Optional<StatedImage> currentImage = findCurrentImage(stackId);
            if (currentImage.isPresent()) {
                if (areComponentVersionsEqualWithTheOriginalImage(currentImage.get(), activeCmVersion.get(), activeParcels)) {
                    LOGGER.debug("The current component versions {} are the same as the versions on the cluster's image {}", activeParcels, currentImage);
                } else {
                    findTargetImage(stackId).ifPresentOrElse(
                            image -> examinePackageVersionsWithTargetImage(stackId, image, activeCmVersion.get(), activeParcels),
                            () -> examinePackageVersionsWithAllCandidateImages(stackId, candidateImages, activeCmVersion.get(), activeParcels,
                                    getImageCatalogUrl(currentImage)));
                }
            } else {
                examinePackageVersionsWithAllCandidateImages(stackId, candidateImages, activeCmVersion.get(), activeParcels, getImageCatalogUrl(currentImage));
            }
        } else {
            LOGGER.warn("The validation of the package versions cannot be performed because the CM version is not present in the sync result. {}", syncResult);
        }
    }

    private Optional<Image> findTargetImage(Long stackId) {
        Optional<com.sequenceiq.cloudbreak.cloud.model.Image> targetImage = clusterUpgradeTargetImageService.findTargetImage(stackId);
        return targetImage.isPresent() ? findImageInCatalog(targetImage.get()) : Optional.empty();
    }

    private Optional<Image> findImageInCatalog(com.sequenceiq.cloudbreak.cloud.model.Image image) {
        try {
            return Optional.of(imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()).getImage());
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("There is no image {} found in catalog {}.", image.getImageId(), image.getImageCatalogUrl());
            return Optional.empty();
        }
    }

    private void examinePackageVersionsWithTargetImage(Long stackId, Image targetImage, String activeCmVersion, Set<ParcelInfo> activeParcels) {
        targetImageAwareMixedPackageVersionService.examinePackageVersionsWithTargetImage(stackId, targetImage, activeCmVersion, activeParcels);
    }

    private void examinePackageVersionsWithAllCandidateImages(Long stackId, Set<Image> candidateImages, String currentCmVersion, Set<ParcelInfo> activeParcels,
            String imageCatalogUrl) {
        candidateImageAwareMixedPackageVersionService.examinePackageVersionsWithAllCandidateImages(stackId, candidateImages, currentCmVersion, activeParcels,
                imageCatalogUrl);
    }

    private Boolean areComponentVersionsEqualWithTheOriginalImage(StatedImage currentImage, String currentCmVersion, Set<ParcelInfo> activeParcels) {
        String cmVersionFomImage = getCmVersion(currentImage.getImage().getPackageVersions());
        return mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(cmVersionFomImage, getProductsFromImage(currentImage.getImage()),
                currentCmVersion, activeParcels);
    }

    private Map<String, String> getProductsFromImage(Image image) {
        return clouderaManagerProductTransformer.transformToMap(image, true, true);
    }

    private Optional<StatedImage> findCurrentImage(Long stackId) {
        try {
            com.sequenceiq.cloudbreak.cloud.model.Image image = imageService.getImage(stackId);
            return Optional.ofNullable(imageCatalogService.getImage(image.getImageCatalogUrl(), image.getImageCatalogName(), image.getImageId()));
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            LOGGER.warn("Current image not found for this cluster.");
            return Optional.empty();
        }
    }

    private String getCmVersion(Map<String, String> packageVersions) {
        return packageVersions.get(CM.getKey());
    }

    private String getImageCatalogUrl(Optional<StatedImage> currentImage) {
        return currentImage.map(StatedImage::getImageCatalogUrl).orElseGet(() -> imageCatalogService.getCloudbreakDefaultImageCatalog().getImageCatalogUrl());
    }

}
