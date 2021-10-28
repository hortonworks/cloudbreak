package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class TargetImageAwareMixedPackageVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetImageAwareMixedPackageVersionService.class);

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private MixedPackageMessageProvider mixedPackageMessageProvider;

    @Inject
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    public void examinePackageVersionsWithTargetImage(Long stackId, Image targetImage, String activeCmVersion, Set<ParcelInfo> activeParcels) {
        LOGGER.debug("Comparing active package versions {} and active CM version {} with target image {}", activeParcels, activeCmVersion, targetImage);
        Map<String, String> targetProducts = getProductsFromImage(targetImage);
        if (areAllComponentVersionsMatchImageVersions(targetImage, targetProducts, activeCmVersion, activeParcels)) {
            LOGGER.debug("The parcel versions {} and CM {} are the same as the versions on the target image {}", activeParcels, activeCmVersion, targetImage);
        } else {
            Map<String, String> newerComponents = getComponentsWithNewerVersionThenTheTarget(targetProducts, targetImage, activeParcels, activeCmVersion);
            String activeParcelsMessage = createActiveParcelsMessage(activeParcels);
            if (newerComponents.isEmpty()) {
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED,
                        List.of(activeCmVersion, activeParcelsMessage, createSuggestedVersionsMessage(targetImage, activeParcels, targetProducts)));
            } else {
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED, List.of(activeCmVersion, activeParcelsMessage,
                        createMessageFromMap(newerComponents),
                        createMessageFromMap(filterTargetPackageVersionsByNewerPackageVersions(targetProducts, targetImage, newerComponents))));
            }
        }
    }

    private boolean areAllComponentVersionsMatchImageVersions(Image image, Map<String, String> productsFromImage, String currentCmVersion,
            Set<ParcelInfo> activeParcels) {
        String cmVersionFomImage = getCmVersion(image.getPackageVersions());
        return mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(cmVersionFomImage, productsFromImage, currentCmVersion, activeParcels);
    }

    private Map<String, String> getComponentsWithNewerVersionThenTheTarget(Map<String, String> targetProducts, Image targetImage,
            Set<ParcelInfo> activeParcels, String activeCmVersion) {
        String targetCmVersion = getCmVersion(targetImage.getPackageVersions());
        return mixedPackageVersionComparator.getComponentsWithNewerVersionThanTheTarget(targetProducts, targetCmVersion, activeParcels, activeCmVersion);
    }

    private Map<String, String> filterTargetPackageVersionsByNewerPackageVersions(Map<String, String> targetProducts, Image targetImage,
            Map<String, String> newerComponentVersions) {
        String cmVersion = getCmVersion(targetImage.getPackageVersions());
        return mixedPackageVersionComparator.filterTargetPackageVersionsByNewerPackageVersions(targetProducts, cmVersion, newerComponentVersions);
    }

    private Map<String, String> getProductsFromImage(Image targetImage) {
        return clouderaManagerProductTransformer.transformToMap(targetImage, true, true);
    }

    private String createActiveParcelsMessage(Set<ParcelInfo> activeParcels) {
        return mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels);
    }

    private String createSuggestedVersionsMessage(Image image, Set<ParcelInfo> activeParcels, Map<String, String> targetProducts) {
        return mixedPackageMessageProvider.createSuggestedVersionsMessage(targetProducts, activeParcels, getCmVersion(image.getPackageVersions()));
    }

    private String createMessageFromMap(Map<String, String> components) {
        return mixedPackageMessageProvider.createMessageFromMap(components);
    }

    private String getCmVersion(Map<String, String> packageVersions) {
        return packageVersions.get(CM.getKey());
    }

    private void sendNotification(Long stackId, ResourceEvent resourceEvent, List<String> args) {
        eventService.fireCloudbreakEvent(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent, args);
    }
}
