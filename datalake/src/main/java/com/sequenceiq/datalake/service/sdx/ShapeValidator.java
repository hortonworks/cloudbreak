package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.ENTERPRISE_DATALAKE_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_MAXIMUM_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MEDIUM_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.MICRO_DUTY_REQUIRED_VERSION;
import static com.sequenceiq.datalake.service.sdx.SdxVersionRuleEnforcer.SHAPES_WITHOUT_HBASE_REQUIRED_VERSION;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
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

    private static final Set<SdxClusterShape> SHAPES_WITHOUT_HBASE_AND_HDFS = Set.of(SdxClusterShape.LIGHT_DUTY_PRO,
            SdxClusterShape.ENTERPRISE_PRO);

    // Host group that carries the volume-pinned services (ZooKeeper+KRaft, Kafka, Solr) and the minimum
    // number of attached volumes it must have so the blueprint's fs1/fs2/fs3 data-dir mounts exist.
    private static final Map<SdxClusterShape, Map.Entry<String, Integer>> MIN_VOLUMES_PER_SHAPE = Map.of(
            SdxClusterShape.ENTERPRISE_PRO, Map.entry("core", 3),
            SdxClusterShape.LIGHT_DUTY_PRO, Map.entry("master", 3));

    @Inject
    private EntitlementService entitlementService;

    public void validateShape(SdxClusterShape shape, String runtime, DetailedEnvironmentResponse environment) {
        validateShape(shape, runtime, runtime, environment);
    }

    /**
     * @param runtime the runtime line (e.g. {@code 7.3.2}) used by the shape-specific minimum/maximum checks
     * @param servicePackQualifiedRuntime the runtime (e.g. {@code 7.3.2.10000}) used by the WITHOUT_HBASE check,
     *                              which needs service-pack build granularity; callers fall back to {@code runtime} when the build cannot be resolved
     */
    public void validateShape(SdxClusterShape shape, String runtime, String servicePackQualifiedRuntime, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (SdxClusterShape.MICRO_DUTY.equals(shape)) {
            validateMicroDutyShape(runtime, environment, validationBuilder);
        } else if (SdxClusterShape.MEDIUM_DUTY_HA.equals(shape)) {
            validateMediumDutyShape(runtime, validationBuilder, environment.getAccountId());
        } else if (SdxClusterShape.ENTERPRISE.equals(shape)) {
            validateEnterpriseShape(runtime, validationBuilder);
        } else if (SHAPES_WITHOUT_HBASE_AND_HDFS.contains(shape)) {
            validateShapesWithoutHBaseAndHDFS(validationBuilder, shape, environment.getAccountId(), servicePackQualifiedRuntime);
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    public void validateVolumeCount(SdxClusterShape shape, StackV4Request stackRequest) {
        if (MIN_VOLUMES_PER_SHAPE.containsKey(shape) && stackRequest != null && stackRequest.getInstanceGroups() != null) {
            Map.Entry<String, Integer> requirement = MIN_VOLUMES_PER_SHAPE.get(shape);
            String groupName = requirement.getKey();
            int minVolumes = requirement.getValue();
            stackRequest.getInstanceGroups().stream()
                    .filter(instanceGroup -> groupName.equals(instanceGroup.getName()))
                    .findFirst()
                    .ifPresent(instanceGroup -> {
                        int volumeCount = countAttachedVolumes(instanceGroup);
                        if (volumeCount < minVolumes) {
                            throw new BadRequestException(String.format(
                                    "SDX shape %s requires the '%s' host group to have at least %d attached volumes " +
                                            "(ZooKeeper+KRaft, Kafka and Solr data-dir isolation); the cluster template provides %d.",
                                    shape.name(), groupName, minVolumes, volumeCount));
                        }
                    });
        }
    }

    private int countAttachedVolumes(InstanceGroupV4Request instanceGroup) {
        return Optional.ofNullable(instanceGroup.getTemplate())
                .map(InstanceTemplateV4Request::getAttachedVolumes)
                .orElseGet(Set::of)
                .stream()
                .mapToInt(volume -> Optional.ofNullable(volume.getCount()).orElse(0))
                .sum();
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

    private void validateShapesWithoutHBaseAndHDFS(ValidationResultBuilder validationBuilder, SdxClusterShape shape, String accountId, String runtime) {
        if (!entitlementService.isDataLakeShapesWithoutHBaseAndHDFSEnabled(accountId)) {
            String message = String.format("Your account is not entitled to provision SDX with '%s' shape. " +
                    "Contact Cloudera support to enable CDP_DATALAKE_SHAPES_WITHOUT_HBASE_AND_HDFS entitlement for the account.", shape.name());
            validationBuilder.error(message);
        }
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, SHAPES_WITHOUT_HBASE_REQUIRED_VERSION)) {
            String message = String.format("Provisioning an %s SDX shape is only valid for runtime version greater than or equal to %s and not %s",
                    shape.name(), SHAPES_WITHOUT_HBASE_REQUIRED_VERSION, runtime);
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
