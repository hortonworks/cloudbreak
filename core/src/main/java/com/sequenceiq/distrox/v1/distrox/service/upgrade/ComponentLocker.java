package com.sequenceiq.distrox.v1.distrox.service.upgrade;

import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages.LATEST_ONLY;
import static com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages.SHOW;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeShowAvailableImages;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroxUpgradeV1Request;

@Component
public class ComponentLocker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentLocker.class);

    @Inject
    private DistroxUpgradeAvailabilityService upgradeAvailabilityService;

    public DistroxUpgradeV1Request lockComponentsIfRuntimeUpgradeIsDisabled(DistroxUpgradeV1Request request, String userCrn, String clusterNameOrCrn) {
        if (!upgradeAvailabilityService.isRuntimeUpgradeEnabled(userCrn) && (!isUpgradeTypeSpecified(request) || isShowOnly(request))) {
            LOGGER.info("Set lock-components since no upgrade type is specified and runtime upgrade is disabled for cluster: {}", clusterNameOrCrn);
            request.setLockComponents(Boolean.TRUE);
        }
        return request;
    }

    private boolean isUpgradeTypeSpecified(DistroxUpgradeV1Request request) {
        return !(request.isEmpty() || isDryRunOnly(request));
    }

    private boolean isShowOnly(DistroxUpgradeV1Request request) {
        DistroxUpgradeShowAvailableImages showAvailableImages = request.getShowAvailableImages();
        return (LATEST_ONLY.equals(showAvailableImages) || SHOW.equals(showAvailableImages)) && isRequestTypeEmpty(request);
    }

    private boolean isDryRunOnly(DistroxUpgradeV1Request request) {
        return request.isDryRun() && isRequestTypeEmpty(request);
    }

    private boolean isRequestTypeEmpty(DistroxUpgradeV1Request request) {
        return StringUtils.isEmpty(request.getRuntime()) && StringUtils.isEmpty(request.getImageId()) && !isOsUpgrade(request);
    }

    private boolean isOsUpgrade(DistroxUpgradeV1Request request) {
        return Boolean.TRUE.equals(request.getLockComponents()) && StringUtils.isEmpty(request.getRuntime());
    }
}
