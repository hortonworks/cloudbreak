package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

@Service
public class TargetImageAwareMixedPackageVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetImageAwareMixedPackageVersionService.class);

    @Inject
    private MixedPackageMessageProvider mixedPackageMessageProvider;

    @Inject
    private MixedPackageVersionComparator mixedPackageVersionComparator;

    @Inject
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Inject
    private MixedPackageNotificationService mixedPackageNotificationService;

    public void examinePackageVersionsWithTargetImage(Long stackId, Image targetImage, String activeCmVersion, Set<ParcelInfo> activeParcels) {
        examinePackageVersions(stackId, getProductsFromImage(targetImage), getCmVersion(targetImage.getPackageVersions()), activeCmVersion, activeParcels);
    }

    public void examinePackageVersionsWithTargetProperties(Long stackId, ClusterUpgradeProperties properties, String activeCmVersion,
            Set<ParcelInfo> activeParcels) {
        if (properties.targetImage().clouderaManagerRepo() == null) {
            LOGGER.warn("Cannot compare package versions because the target CM repository is missing.");
            return;
        }
        Map<String, String> targetProducts = new HashMap<>();
        for (ClouderaManagerProduct product : properties.targetImage().getAllProducts()) {
            if (StringUtils.isBlank(product.getName()) || StringUtils.isBlank(product.getVersion())) {
                LOGGER.warn("Cannot compare package versions because a target parcel name or version is missing.");
                return;
            }
            String previousVersion = targetProducts.putIfAbsent(product.getName(), product.getVersion());
            if (previousVersion != null && !previousVersion.equals(product.getVersion())) {
                LOGGER.warn("Cannot compare package versions because target parcel {} has conflicting versions {} and {}.",
                        product.getName(), previousVersion, product.getVersion());
                return;
            }
        }
        examinePackageVersions(stackId, targetProducts, properties.targetImage().clouderaManagerRepo().getFullVersion(), activeCmVersion, activeParcels);
    }

    private void examinePackageVersions(Long stackId, Map<String, String> targetProducts, String targetCmVersion, String activeCmVersion,
            Set<ParcelInfo> activeParcels) {
        LOGGER.debug("Comparing active parcels {} and CM {} with target parcels {} and CM {}", activeParcels, activeCmVersion, targetProducts, targetCmVersion);
        if (mixedPackageVersionComparator.areAllComponentVersionsMatchingWithImage(targetCmVersion, targetProducts, activeCmVersion, activeParcels)) {
            LOGGER.debug("The parcel versions {} and CM {} match the target components", activeParcels, activeCmVersion);
        } else {
            Map<String, String> newerComponents = mixedPackageVersionComparator.getComponentsWithNewerVersionThanTheTarget(
                    targetProducts, targetCmVersion, activeParcels, activeCmVersion);
            String activeParcelsMessage = createActiveParcelsMessage(activeParcels);
            if (newerComponents.isEmpty()) {
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_FAILED,
                        List.of(activeCmVersion, activeParcelsMessage,
                                mixedPackageMessageProvider.createSuggestedVersionsMessage(targetProducts, activeParcels, targetCmVersion)));
            } else {
                sendNotification(stackId, STACK_CM_MIXED_PACKAGE_VERSIONS_NEWER_FAILED, List.of(activeCmVersion, activeParcelsMessage,
                        createMessageFromMap(newerComponents), createMessageFromMap(mixedPackageVersionComparator
                                .filterTargetPackageVersionsByNewerPackageVersions(targetProducts, targetCmVersion, newerComponents))));
            }
        }
    }

    private Map<String, String> getProductsFromImage(Image targetImage) {
        return clouderaManagerProductTransformer.transformToMap(targetImage, true, true);
    }

    private String createActiveParcelsMessage(Set<ParcelInfo> activeParcels) {
        return mixedPackageMessageProvider.createActiveParcelsMessage(activeParcels);
    }

    private String createMessageFromMap(Map<String, String> components) {
        return mixedPackageMessageProvider.createMessageFromMap(components);
    }

    private String getCmVersion(Map<String, String> packageVersions) {
        return String.format("%s-%s", packageVersions.get(CM.getKey()), packageVersions.get(CM_BUILD_NUMBER.getKey()));
    }

    private void sendNotification(Long stackId, ResourceEvent resourceEvent, List<String> args) {
        mixedPackageNotificationService.sendNotification(stackId, resourceEvent, args);
    }
}
