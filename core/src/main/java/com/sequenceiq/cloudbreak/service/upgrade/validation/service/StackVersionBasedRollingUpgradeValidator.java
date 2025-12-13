package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_17;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionEqualToLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionOlderThanLimited;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class StackVersionBasedRollingUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackVersionBasedRollingUpgradeValidator.class);

    private static final int MIN_PATCH_VERSION_FOR_7218_UPGRADE = 300;

    private static final int MIN_PATCH_VERSION_FOR_7217_UPGRADE = 200;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(ServiceUpgradeValidationRequest request) {
        if (request.rollingUpgradeEnabled() && rollingUpgradeValidationEnabled()) {
            getCurrentCdhPatchVersion(request).ifPresentOrElse(currentPatchVersion -> {
                String currentCdhVersion = getCurrentCdhVersion(request.upgradeImageInfo());
                if (targetVersionIs7218OrNewer(request) && (currentRuntimeVersionOlderThan7217(currentCdhVersion) ||
                        currentVersionIs7217AndPatchVersionIsLowerThanLimited(currentCdhVersion, currentPatchVersion, MIN_PATCH_VERSION_FOR_7218_UPGRADE))) {
                    throwValidationError(currentCdhVersion, currentPatchVersion, MIN_PATCH_VERSION_FOR_7218_UPGRADE);
                } else if (currentVersionIs7217AndPatchVersionIsLowerThanLimited(currentCdhVersion, currentPatchVersion, MIN_PATCH_VERSION_FOR_7217_UPGRADE)) {
                    throwValidationError(currentCdhVersion, currentPatchVersion, MIN_PATCH_VERSION_FOR_7217_UPGRADE);
                }
            }, () -> LOGGER.debug("Skip validation because the current CDH patch version not present."));
        }
    }

    private boolean currentVersionIs7217AndPatchVersionIsLowerThanLimited(String currentCdhVersion, int currentCdhPatchVersion, int minPatchVersion) {
        return isVersionEqualToLimited(currentCdhVersion, CLOUDERA_STACK_VERSION_7_2_17) && currentCdhPatchVersion < minPatchVersion;
    }

    private void throwValidationError(String cdhVersion, int patchVersion, int minimumVersion) {
        String errorMessage = String.format("You are not eligible to perform rolling upgrade to the selected runtime because your current runtime "
                        + "version (%s.p%s) does not contains important changes. "
                        + "Please run a runtime upgrade to the latest (or at least 7.2.17.p%s) service pack for your current runtime version."
                        + "After this you will be able to launch a rolling upgrade to the more recent runtime.", cdhVersion, patchVersion, minimumVersion);
        LOGGER.debug(errorMessage);
        throw new UpgradeValidationFailedException(errorMessage);
    }

    private String getCurrentCdhVersion(UpgradeImageInfo upgradeImageInfo) {
        return upgradeImageInfo.getCurrentImage().getPackageVersion(ImagePackageVersion.STACK);
    }

    private boolean targetVersionIs7218OrNewer(ServiceUpgradeValidationRequest request) {
        return isVersionNewerOrEqualThanLimited(request.upgradeImageInfo().targetStatedImage().getImage().getVersion(), CLOUDERA_STACK_VERSION_7_2_18);
    }

    private boolean currentRuntimeVersionOlderThan7217(String currentCdhVersion) {
        return isVersionOlderThanLimited(currentCdhVersion, CLOUDERA_STACK_VERSION_7_2_17);
    }

    private boolean rollingUpgradeValidationEnabled() {
        if (entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            LOGGER.debug("Skipping this validation because the CDP_SKIP_ROLLING_UPGRADE_VALIDATION entitlement is enabled");
            return false;
        } else {
            return true;
        }
    }

    private Optional<Integer> getCurrentCdhPatchVersion(ServiceUpgradeValidationRequest validationRequest) {
        return clusterComponentConfigProvider.getCdhProduct(validationRequest.stack().getCluster().getId())
                .flatMap(cdhProduct -> CdhVersionProvider.getCdhPatchVersionFromVersionString(cdhProduct.getVersion()));
    }
}
