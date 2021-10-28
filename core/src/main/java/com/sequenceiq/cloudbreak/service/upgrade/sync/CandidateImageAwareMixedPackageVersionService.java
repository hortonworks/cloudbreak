package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_MULTIPLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_NO_CANDIDATE;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.sync.common.ParcelInfo;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class CandidateImageAwareMixedPackageVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CandidateImageAwareMixedPackageVersionService.class);

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private MixedPackageMessageProvider mixedPackageMessageProvider;

    @Inject
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public void examinePackageVersionsWithAllCandidateImages(Long stackId, Set<Image> candidateImages, String currentCmVersion, Set<ParcelInfo> activeParcels,
            String imageCatalogUrl) {
        LOGGER.debug("Target image not found for cluster therefore comparing package versions with other candidate images.");
        if (areComponentVersionsMatchWithAnyImage(activeParcels, currentCmVersion, candidateImages)) {
            LOGGER.debug("The combination of the cluster package versions are compatible.");
        } else {
            Set<Image> imagesWithSameCmVersion = getImageByCurrentCmVersion(currentCmVersion, candidateImages);
            String activeParcelsMessage = createActiveParcelsMessage(activeParcels);
            if (imagesWithSameCmVersion.isEmpty()) {
                LOGGER.debug("There is no other image in the catalog with the same {} CM version.", currentCmVersion);
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_NO_CANDIDATE, List.of(currentCmVersion, activeParcelsMessage));
            } else if (imagesWithSameCmVersion.size() > 1) {
                LOGGER.debug("Multiple image is available with the same CM version {}", imagesWithSameCmVersion);
                Image latestImage = getLatestImage(imagesWithSameCmVersion);
                String suggestedVersionsMessage = createSuggestedVersionsMessage(latestImage, activeParcels);
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED_MULTIPLE,
                        List.of(currentCmVersion, activeParcelsMessage, suggestedVersionsMessage, imageCatalogUrl));
            } else {
                LOGGER.debug("There is only one image with the same CM version.");
                String suggestedVersions = createSuggestedVersionsMessage(imagesWithSameCmVersion.iterator().next(), activeParcels);
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED, List.of(currentCmVersion, activeParcelsMessage, suggestedVersions));
            }
        }
    }

    private boolean areComponentVersionsMatchWithAnyImage(Set<ParcelInfo> activeParcels, String cmVersion, Set<Image> images) {
        return images.stream().anyMatch(image -> cmVersion.equals(getCmVersion(image.getPackageVersions())) && matchParcelVersions(activeParcels, image));
    }

    private String getCmVersion(Map<String, String> packageVersions) {
        return packageVersions.get(CM.getKey());
    }

    private boolean matchParcelVersions(Set<ParcelInfo> activeParcels, Image image) {
        return mixedPackageVersionComparator.matchParcelVersions(activeParcels, getProductsFromImage(image));
    }

    private Map<String, String> getProductsFromImage(Image image) {
        return clouderaManagerProductTransformer.transformToMap(image, true, true);
    }

    private Set<Image> getImageByCurrentCmVersion(String currentCmVersion, Set<Image> candidateImages) {
        return candidateImages.stream()
                .filter(image -> currentCmVersion.equals(getCmVersion(image.getPackageVersions())))
                .collect(Collectors.toSet());
    }

    private String createActiveParcelsMessage(Set<ParcelInfo> activeParcels) {
        return mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels);
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image getLatestImage(Set<Image> images) {
        return images.stream()
                .max(Comparator.comparing(Image::getCreated))
                .orElse(images.iterator().next());
    }

    private String createSuggestedVersionsMessage(Image image, Set<ParcelInfo> activeParcels) {
        return mixedPackageMessageProvider.createSuggestedVersionsMessage(getProductsFromImage(image), activeParcels, getCmVersion(image.getPackageVersions()));
    }

    private void sendNotification(Long stackId, ResourceEvent resourceEvent, List<String> args) {
        eventService.fireCloudbreakEvent(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent, args);
    }
}
