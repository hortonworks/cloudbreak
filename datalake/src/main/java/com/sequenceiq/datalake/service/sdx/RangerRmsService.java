package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MIN_RUNTIME_VERSION_FOR_RMS;

import java.util.Comparator;

import jakarta.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@Service
public class RangerRmsService {

    @Inject
    private EntitlementService entitlementService;

    public void validateRmsEnablement(String runtime, boolean razEnabled, boolean rmsEnabled, String cloudPlatformString, String accountId) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, cloudPlatformString);
        if (rmsEnabled) {
            if (!razEnabled) {
                validationResultBuilder.error("Ranger RMS cannot be deployed without Ranger RAZ");
            }
            if (AWS != cloudPlatform) {
                validationResultBuilder.error("Ranger RMS can be deployed only on AWS.");
            }
            if (!entitlementService.isRmsEnabledOnDatalake(accountId)) {
                validationResultBuilder.error("Provisioning Ranger RMS is not enabled for this account");
            }
            Comparator<Versioned> versionComparator = new VersionComparator();
            if (versionComparator.compare(() -> runtime, MIN_RUNTIME_VERSION_FOR_RMS) < 0) {
                validationResultBuilder.error(String.format("Provisioning Ranger RMS is only valid for Cloudera Runtime version greater then or equal to %s" +
                        " and not %s", MIN_RUNTIME_VERSION_FOR_RMS.getVersion(), runtime));
            }
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }
}
