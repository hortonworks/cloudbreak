package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.ENTERPRISE_DATALAKE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_MAXIMUM_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MICRO_DUTY_REQUIRED_VERSION;

import java.util.Comparator;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class ShapeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeValidator.class);

    @Inject
    private EntitlementService entitlementService;

    public void validateShape(SdxClusterShape shape, String runtime, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (SdxClusterShape.MICRO_DUTY.equals(shape)) {
            validateMicroDutyShape(runtime, environment, validationBuilder);
        } else if (SdxClusterShape.MEDIUM_DUTY_HA.equals(shape)) {
            validateMediumDutyShape(runtime, validationBuilder, environment.getAccountId());
        } else if (SdxClusterShape.ENTERPRISE.equals(shape)) {
            validateEnterpriseShape(runtime, validationBuilder);
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateMicroDutyShape(String runtime, DetailedEnvironmentResponse environment, ValidationResultBuilder validationBuilder) {
        if (!entitlementService.microDutySdxEnabled(Crn.safeFromString(environment.getCreator()).getAccountId())) {
            validationBuilder.error(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. " +
                    "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", environment.getCloudPlatform()));
        }
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MICRO_DUTY_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning a Micro Duty SDX shape is only valid for CM version greater than or equal to "
                    + MICRO_DUTY_REQUIRED_VERSION + " and not " + runtime);
        }
    }

    private void validateMediumDutyShape(String runtime, ValidationResultBuilder validationBuilder, String accountId) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MEDIUM_DUTY_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                    + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + runtime);
        }
        if (!isShapeVersionSupportedByMaximumRuntimeVersion(runtime, MEDIUM_DUTY_MAXIMUM_VERSION)
                && !entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(accountId)) {
            validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for 7.2.17 and below. If you want to provision a " +
                    runtime + " SDX, Please use the ENTERPRISE shape!");
        }
    }

    private void validateEnterpriseShape(String runtime, ValidationResultBuilder validationBuilder) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, ENTERPRISE_DATALAKE_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning an Enterprise SDX shape is only valid for CM version greater than or equal to "
                    + ENTERPRISE_DATALAKE_REQUIRED_VERSION + " and not " + runtime);
        }
    }

    private boolean isShapeVersionSupportedByMinimumRuntimeVersion(String runtime, String shapeVersion) {
        if (isTriggeredInternally(runtime)) {
            return true;
        }

        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> shapeVersion) > -1;
    }

    private boolean isShapeVersionSupportedByMaximumRuntimeVersion(String runtime, String shapeVersion) {
        if (isTriggeredInternally(runtime)) {
            return true;
        }

        Comparator<Versioned> versionedComparator = new VersionComparator();
        return versionedComparator.compare(() -> runtime, () -> shapeVersion) < 1;
    }

    private boolean isTriggeredInternally(String runtime) {
        LOGGER.debug("Runtime is '{}'. If runtime is empty, then SDX internal call was used and runtime version will not be validated.", runtime);
        return StringUtils.isEmpty(runtime);
    }
}
