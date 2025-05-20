package com.sequenceiq.datalake.service.validation.upgrade;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ClouderaManagerLicenseProvider;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.PaywallAccessChecker;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;

@Component
public class SdxUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeValidator.class);

    @Value("${sdx.paywall.url}")
    private String paywallUrl;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private PaywallAccessChecker paywallAccessChecker;

    @Inject
    private ClouderaManagerLicenseProvider clouderaManagerLicenseProvider;

    public void validateRollingUpgradeByClusterShape(SdxUpgradeRequest request, SdxClusterShape clusterShape) {
        boolean rollingUpgradeEnabled = Boolean.TRUE.equals(request.getRollingUpgradeEnabled());
        if (rollingUpgradeEnabled) {
            if (SdxClusterShape.ENTERPRISE.equals(clusterShape) ||
                    (skipRollingUpgradeValidationEnabled() && (clusterShape.isHA() || SdxClusterShape.CUSTOM.equals(clusterShape)))) {
                LOGGER.debug("Allowing rolling upgrade for {} cluster shape", clusterShape.name());
            } else {
                String message = String.format("Rolling upgrade is not supported for %s cluster shape.", clusterShape.name());
                LOGGER.warn(message);
                throw new BadRequestException(message);
            }
        }
    }

    private boolean skipRollingUpgradeValidationEnabled() {
        return entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }

    public void verifyPaywallAccess(String userCrn, SdxUpgradeRequest upgradeRequest) {
        if (upgradeRequest != null && !Boolean.TRUE.equals(upgradeRequest.getLockComponents())) {
            if (!entitlementService.isInternalRepositoryForUpgradeAllowed(Crn.fromString(userCrn).getAccountId())) {
                LOGGER.info("Verify if the CM license is valid to authenticate to {}", paywallUrl);
                JsonCMLicense license = clouderaManagerLicenseProvider.getLicense(userCrn);
                paywallAccessChecker.checkPaywallAccess(license, paywallUrl);
            } else {
                LOGGER.info("Internal repo is allowed for upgrade, skip CM license validation");
            }
        }
    }
}
