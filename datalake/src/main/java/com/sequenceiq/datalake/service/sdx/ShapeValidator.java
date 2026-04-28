package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.ENTERPRISE_DATALAKE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_MAXIMUM_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MICRO_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.SHAPES_WITHOUT_HBASE_REQUIRED_VERSION;

import java.util.Comparator;
import java.util.Set;

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

    private static final Set<SdxClusterShape> SHAPES_WITHOUT_HBASE_AND_HDFS = Set.of(SdxClusterShape.LIGHT_DUTY_WITHOUT_HBASE,
            SdxClusterShape.ENTERPRISE_WITHOUT_HBASE);

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
        } else if (SHAPES_WITHOUT_HBASE_AND_HDFS.contains(shape)) {
            validateShapesWithoutHBaseAndHDFS(runtime, validationBuilder, shape, environment.getAccountId());
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateMicroDutyShape(String runtime, DetailedEnvironmentResponse environment, ValidationResultBuilder validationBuilder) {
        if (!entitlementService.microDutySdxEnabled(Crn.safeFromString(environment.getCreator()).getAccountId())) {
            String message = String.format("Provisioning a micro duty data lake cluster is not enabled for %s. " +
                    "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", environment.getCloudPlatform());
            validationBuilder.error(message);
        }
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MICRO_DUTY_REQUIRED_VERSION)) {
            String message = String.format("Provisioning a Micro Duty SDX shape is only valid for runtime version greater than or equal to %s and not %s",
                    MICRO_DUTY_REQUIRED_VERSION, runtime);
            validationBuilder.error(message);
        }
    }

    private void validateMediumDutyShape(String runtime, ValidationResultBuilder validationBuilder, String accountId) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MEDIUM_DUTY_REQUIRED_VERSION)) {
            String message = String.format("Provisioning a Medium Duty SDX shape is only valid for runtime version greater than or equal to %s and not %s",
                    MEDIUM_DUTY_REQUIRED_VERSION, runtime);
            validationBuilder.error(message);
        }
        if (!isShapeVersionSupportedByMaximumRuntimeVersion(runtime, MEDIUM_DUTY_MAXIMUM_VERSION)
                && !entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(accountId)) {
            String message = String.format("Provisioning a Medium Duty SDX shape is only valid for 7.2.17 and below. " +
                    "If you want to provision a %s SDX, Please use the ENTERPRISE shape!", runtime);
            validationBuilder.error(message);
        }
    }

    private void validateEnterpriseShape(String runtime, ValidationResultBuilder validationBuilder) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, ENTERPRISE_DATALAKE_REQUIRED_VERSION)) {
            String message = String.format("Provisioning an Enterprise SDX shape is only valid for runtime version greater than or equal to %s and not %s",
                    ENTERPRISE_DATALAKE_REQUIRED_VERSION, runtime);
            validationBuilder.error(message);
        }
    }

    private void validateShapesWithoutHBaseAndHDFS(String runtime, ValidationResultBuilder validationBuilder, SdxClusterShape shape, String accountId) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, SHAPES_WITHOUT_HBASE_REQUIRED_VERSION)) {
            String message = String.format("Provisioning an %s SDX shape is only valid for runtime version greater than or equal to %s and not %s",
                    shape.name(), SHAPES_WITHOUT_HBASE_REQUIRED_VERSION, runtime);
            validationBuilder.error(message);
        }
        if (!entitlementService.isDataLakeShapesWithoutHBaseAndHDFSEnabled(accountId)) {
            String message = String.format("Your account is not entitled to provision SDX with '%s' shape. " +
                    "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", shape.name());
            validationBuilder.error(message);
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
