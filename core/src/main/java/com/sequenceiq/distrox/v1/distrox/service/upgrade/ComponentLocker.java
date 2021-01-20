package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages.LATEST_ONLY;
import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages.SHOW;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;

@Component
public class ComponentLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentLocker.class);

    @Inject
    private DistroXUpgradeAvailabilityService upgradeAvailabilityService;

    public DistroXUpgradeV1Request lockComponentsIfRuntimeUpgradeIsDisabled(DistroXUpgradeV1Request request, String userCrn, String clusterNameOrCrn) {
        if (!upgradeAvailabilityService.isRuntimeUpgradeEnabled(userCrn) && (!isUpgradeTypeSpecified(request) || isShowOnly(request))) {
            LOGGER.info("Set lock-components since no upgrade type is specified and runtime upgrade is disabled for cluster: {}", clusterNameOrCrn);
            request.setLockComponents(Boolean.TRUE);
        }
        return request;
    }

    private boolean isUpgradeTypeSpecified(DistroXUpgradeV1Request request) {
        return !(request.isEmpty() || isDryRunOnly(request));
    }

    private boolean isShowOnly(DistroXUpgradeV1Request request) {
        DistroXUpgradeShowAvailableImages showAvailableImages = request.getShowAvailableImages();
        return (LATEST_ONLY.equals(showAvailableImages) || SHOW.equals(showAvailableImages)) && isRequestTypeEmpty(request);
    }

    private boolean isDryRunOnly(DistroXUpgradeV1Request request) {
        return request.isDryRun() && isRequestTypeEmpty(request);
    }

    private boolean isRequestTypeEmpty(DistroXUpgradeV1Request request) {
        return StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId()) && !isOsUpgrade(request);
    }

    private boolean isOsUpgrade(DistroXUpgradeV1Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }
}
