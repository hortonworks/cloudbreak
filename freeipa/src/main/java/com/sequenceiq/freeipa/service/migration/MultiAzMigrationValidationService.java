package com.sequenceiq.freeipa.service.migration;

import static java.util.function.Predicate.not;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;

@Service
public class MultiAzMigrationValidationService {

    private static final Set<Variant> SUPPORTED_VARIANTS_FOR_MULTI_AZ_MIGRATION = Set.of(
            AwsConstants.AwsVariant.AWS_VARIANT.variant(),
            AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(),
            AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant()
    );

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    public ValidationResult validateMultiAzMigrationRequest(String environmentCrn, String accountId, Stack stack) {
        return ValidationResult.builder()
                .merge(validateMultiAzMigrationEntitlement(accountId))
                .merge(validateVariantMigrationEntitlementIfNeeded(accountId, stack))
                .merge(validateVariantMigrationSupportedByOsIfNeeded(stack))
                .merge(validateStackMultiAzFlag(stack))
                .merge(validateVariantSupported(stack))
                .merge(validateFreeIpaStatus(stack))
                .merge(validateInstanceStatuses(stack.getNotDeletedInstanceMetaDataSet()))
                .merge(validateEnvironmentZones(environmentCrn))
                .build();
    }

    private ValidationResult validateMultiAzMigrationEntitlement(String accountId) {
        if (!entitlementService.isFreeIpaMultiAzMigrationEnabled(accountId)) {
            return ValidationResult.ofError("The account is not entitled to use FreeIPA multi-AZ migration.");
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateVariantMigrationEntitlementIfNeeded(String accountId, Stack stack) {
        if (AwsConstants.AwsVariant.AWS_VARIANT.variant().equals(Variant.variant(stack.getPlatformvariant()))
                && !entitlementService.awsVariantMigrationEnabled(accountId)) {
            return ValidationResult.ofError("The account is not entitled to use AWS variant migration, " +
                    "which is required for the multi-AZ migration of FreeIPA using AWS CloudFormation.");
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateVariantMigrationSupportedByOsIfNeeded(Stack stack) {
        if (AwsConstants.AwsVariant.AWS_VARIANT.variant().equals(Variant.variant(stack.getPlatformvariant()))) {
            Optional<Boolean> osSupportsAwsVariantMigration = Optional.ofNullable(stack.getImage())
                    .map(ImageEntity::getOsType)
                    .map(OsType::isRhel);
            if (osSupportsAwsVariantMigration.isPresent()) {
                if (!osSupportsAwsVariantMigration.get()) {
                    return ValidationResult.ofError("The current OS does not support the AWS variant migration, " +
                            "which is required for the multi-AZ migration of FreeIPA using AWS CloudFormation.");
                }
            } else {
                return ValidationResult.ofError("Could not retrieve the current OS used by FreeIPA to check for support of AWS variant migration, " +
                        "which is required for the multi-AZ migration of FreeIPA using AWS CloudFormation.");
            }
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateStackMultiAzFlag(Stack stack) {
        if (stack.isMultiAz()) {
            return ValidationResult.ofError("The FreeIPA is already multi-AZ enabled, no need to start the multi-AZ migration.");
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateVariantSupported(Stack stack) {
        if (!SUPPORTED_VARIANTS_FOR_MULTI_AZ_MIGRATION.contains(Variant.variant(stack.getPlatformvariant()))) {
            return ValidationResult.ofError(String.format("Multi-AZ migration is not supported for platform variant of the FreeIPA (%s). " +
                            "It is currently only supported for variants [%s]",
                    stack.getPlatformvariant(), SUPPORTED_VARIANTS_FOR_MULTI_AZ_MIGRATION.stream().map(Variant::value).collect(Collectors.joining(", "))));
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateFreeIpaStatus(Stack stack) {
        if (!stack.isAvailable()) {
            return ValidationResult.ofError("The FreeIPA is not in available state, it must be in available state to start the multi-AZ migration.");
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateInstanceStatuses(Set<InstanceMetaData> allInstances) {
        if (allInstances.stream().anyMatch(not(InstanceMetaData::isAvailable))) {
            Set<String> notAvailableInstances = allInstances.stream()
                    .filter(not(InstanceMetaData::isAvailable))
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toSet());
            return ValidationResult.ofError(
                    "The FreeIPA has non-available instances, all instances must be in available state to start the multi-AZ migration. " +
                            "The following instances are not in available state " + notAvailableInstances);
        }
        return ValidationResult.empty();
    }

    private ValidationResult validateEnvironmentZones(String environmentCrn) {
        try {
            DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(environmentCrn);
            Optional<Set<String>> environmentZones = Optional.ofNullable(environment)
                    .map(DetailedEnvironmentResponse::getNetwork)
                    .map(network -> network.getAvailabilityZones(CloudPlatform.fromName(environment.getCloudPlatform())));
            if (environmentZones.isEmpty()) {
                return ValidationResult.ofError("Could not determine availability zones available for the environment.");
            }
            if (AwsConstants.AWS_PLATFORM.value().equals(environment.getCloudPlatform()) && environmentZones.get().size() < 2) {
                return ValidationResult.ofError("The environment has less than 2 distinct availability zones. " +
                        "For multi-AZ migration on AWS, there needs to be at least 2 subnets in different AZs available.");
            }
        } catch (WebApplicationException e) {
            return ValidationResult.ofError("Could not retrieve environment for validation: " + e.getMessage());
        }
        return ValidationResult.empty();
    }
}
