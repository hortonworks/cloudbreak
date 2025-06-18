package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.common.model.Architecture;

@Component
public class CmSyncImageFinderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncImageFinderService.class);

    private static final String CDH = "CDH";

    private static final String REPOSITORY_VERSION = "repository-version";

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public Optional<Image> findTargetImageForImageSync(CmSyncOperationSummary cmSyncResult, Set<Image> candidateImages, String currentImageId,
            boolean datalake) {
        LOGGER.debug("Searching for new image after CM sync. Current imageId: {}, Candidate imageIds: {}", currentImageId,
                candidateImages.stream().map(Image::getUuid).collect(Collectors.toSet()));
        Optional<Image> targetImage = findTargetImage(cmSyncResult, candidateImages, currentImageId, datalake);
        if (targetImage.isPresent()) {
            LOGGER.debug("Found the latest image that contains all the requires parcels for the cluster: {}", targetImage.get().getUuid());
        } else {
            LOGGER.debug("There is no image found for the cluster that contains all the required parcels for the cluster. Currently installed components: {}",
                    cmSyncResult.getSyncOperationResult());
        }
        return targetImage;
    }

    private Optional<Image> findTargetImage(CmSyncOperationSummary cmSyncResult, Set<Image> candidateImages, String currentImageId, boolean datalake) {
        Image currentImage = findCurrentImageFromImageCatalog(currentImageId, candidateImages);
        return candidateImages.stream()
                .filter(candidateImage ->
                        architectureMatches(currentImage, candidateImage) &&
                                cmVersionMatches(candidateImage, cmSyncResult) &&
                                cdhVersionsMatches(candidateImage, cmSyncResult) &&
                                (datalake || allParcelVersionMatches(candidateImage, cmSyncResult, currentImage)))
                .max(Comparator.comparing(Image::getCreated));
    }

    private boolean architectureMatches(Image currentImage, Image candidateImage) {
        return Objects.equals(Architecture.fromStringWithFallback(currentImage.getArchitecture()),
                Architecture.fromStringWithFallback(candidateImage.getArchitecture()));
    }

    private boolean cmVersionMatches(Image candidateImage, CmSyncOperationSummary cmSyncOperationSummary) {
        Optional<String> cmVersion = cmSyncOperationSummary.getSyncOperationResult().getCmRepoSyncOperationResult().getInstalledCmVersion();
        return cmVersion.isPresent() && cmVersion.get().equals(getCmVersionFromImage(candidateImage));
    }

    private String getCmVersionFromImage(Image candidateImage) {
        return candidateImage.getPackageVersion(ImagePackageVersion.CM) + "-" + candidateImage.getPackageVersion(ImagePackageVersion.CM_BUILD_NUMBER);
    }

    private boolean cdhVersionsMatches(Image candidateImage, CmSyncOperationSummary cmSyncOperationSummary) {
        String cdhVersionFromImage = candidateImage.getStackDetails().getRepo().getStack().get(REPOSITORY_VERSION);
        Set<ParcelInfo> activeParcels = cmSyncOperationSummary.getSyncOperationResult().getCmParcelSyncOperationResult().getActiveParcels();
        return activeParcels.stream()
                .anyMatch(activeParcel -> activeParcel.getName().equals(CDH) && activeParcel.getVersion().equals(cdhVersionFromImage));
    }

    private boolean allParcelVersionMatches(Image candidateImage, CmSyncOperationSummary cmSyncOperationSummary, Image currentImage) {
        Set<ClouderaManagerProduct> productsFromCandidateImage = clouderaManagerProductTransformer.transform(candidateImage, false, true);
        Set<ParcelInfo> activeParcels = cmSyncOperationSummary.getSyncOperationResult().getCmParcelSyncOperationResult().getActiveParcels();
        Set<String> parcelsOnCurrentImage = getParcelsFromCurrentImage(currentImage);
        return activeParcels.stream()
                .filter(activeParcel -> !activeParcel.getName().equals(CDH) && parcelsOnCurrentImage.contains(activeParcel.getName()))
                .allMatch(activeParcel -> productsFromCandidateImage.stream()
                        .anyMatch(productOnImage -> productOnImage.getName().equals(activeParcel.getName())
                                && productOnImage.getVersion().equals(activeParcel.getVersion())));
    }

    private Image findCurrentImageFromImageCatalog(String currentImageId, Set<Image> candidateImages) {
        return candidateImages.stream().filter(image -> image.getUuid().equals(currentImageId)).findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format("The current image %s is not found in the image catalog.", currentImageId)));
    }

    private Set<String> getParcelsFromCurrentImage(Image currentImage) {
        return clouderaManagerProductTransformer.transform(currentImage, false, true).stream()
                .map(ClouderaManagerProduct::getName)
                .collect(Collectors.toSet());
    }
}
