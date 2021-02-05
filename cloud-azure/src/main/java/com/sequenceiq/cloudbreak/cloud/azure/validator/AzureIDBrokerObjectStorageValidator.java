package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.factory.Sets;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.graphrbac.implementation.RoleAssignmentInner;
import com.microsoft.azure.management.msi.Identity;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;

@Component
public class AzureIDBrokerObjectStorageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureIDBrokerObjectStorageValidator.class);

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Inject
    private AzureStorage azureStorage;

    public ValidationResult validateObjectStorage(AzureClient client,
            SpiFileSystem spiFileSystem,
            ValidationResultBuilder resultBuilder) {
        LOGGER.info("Validating Azure identities...");
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        if (Objects.nonNull(cloudFileSystems) && cloudFileSystems.size() > 0) {
            for (CloudFileSystemView cloudFileSystemView : cloudFileSystems) {
                CloudAdlsGen2View cloudFileSystem = (CloudAdlsGen2View) cloudFileSystemView;
                String managedIdentityId = cloudFileSystem.getManagedIdentity();
                Identity identity = client.getIdentityById(managedIdentityId);

                if (identity != null) {
                    CloudIdentityType cloudIdentityType = cloudFileSystem.getCloudIdentityType();
                    if (CloudIdentityType.ID_BROKER.equals(cloudIdentityType)) {
                        PagedList<RoleAssignmentInner> roleAssignments = client.listRoleAssignments();
                        validateIDBroker(client, roleAssignments, identity, cloudFileSystem, resultBuilder);
                    } else if (CloudIdentityType.LOG.equals(cloudIdentityType)) {
                        validateLog(client, identity, cloudFileSystem, resultBuilder);
                    }
                } else {
                    addError(resultBuilder, String.format("Identity with id %s does not exist in the given Azure subscription.", managedIdentityId));
                }
            }
        }
        return resultBuilder.build();
    }

    private void validateIDBroker(AzureClient client, PagedList<RoleAssignmentInner> roleAssignments, Identity identity,
            CloudAdlsGen2View cloudFileSystem, ValidationResultBuilder resultBuilder) {
        LOGGER.debug(String.format("Validating IDBroker identity %s", identity.principalId()));

        Set<Identity> allMappedExistingIdentity = validateAllMappedIdentities(client, cloudFileSystem, resultBuilder);
        validateRoleAssigment(roleAssignments, resultBuilder, allMappedExistingIdentity);
        validateRoleAssigmentAndScope(roleAssignments, resultBuilder, identity,
                List.of("/subscriptions/" + client.getCurrentSubscription().subscriptionId()), false);
        LOGGER.debug("Validating IDBroker identity is finished");

    }

    private void validateLog(AzureClient client, Identity identity, CloudAdlsGen2View cloudFileSystem,
            ValidationResultBuilder resultBuilder) {
        LOGGER.debug(String.format("Validating logger identity %s", identity.principalId()));

        List<StorageLocationBase> locations = cloudFileSystem.getLocations();
        if (Objects.nonNull(locations) && !locations.isEmpty()) {
            AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(locations.get(0).getValue());
            String storageAccountName = adlsGen2Config.getAccount();
            Optional<String> storageAccountIdOptional = azureStorage.findStorageAccountIdInVisibleSubscriptions(client, storageAccountName);
            if (storageAccountIdOptional.isEmpty()) {
                LOGGER.debug("Storage account {} not found or insufficient permission to list subscriptions and / or storage accounts.", storageAccountName);
                addError(resultBuilder, String.format("Storage account with name %s not found.", storageAccountName));
                return;
            }
            List<RoleAssignmentInner> roleAssignments = client.listRoleAssignmentsByScopeInner(storageAccountIdOptional.get());
            ResourceId storageAccountResourceId = ResourceId.fromString(storageAccountIdOptional.get());
            boolean differentSubscriptions = !client.getCurrentSubscription().subscriptionId().equals(storageAccountResourceId.subscriptionId());
            List<RoleAssignmentInner> roleAssignmentsForSubscription =
                    getRoleAssignmentsOfSubscription(roleAssignments, storageAccountResourceId.subscriptionId(), client, differentSubscriptions);
            validateRoleAssigmentAndScope(roleAssignmentsForSubscription, resultBuilder, identity,
                    List.of(storageAccountName, storageAccountResourceId.resourceGroupName(), storageAccountResourceId.subscriptionId()),
                    differentSubscriptions);
        } else {
            LOGGER.debug("There is no storage location set for logger identity, this should not happen!");
        }
        LOGGER.info("Validating logger identity is finished");
    }

    private List<RoleAssignmentInner> getRoleAssignmentsOfSubscription(
            List<RoleAssignmentInner> roleAssignmentsOfCurrentSubscription, String targetSubscriptionId, AzureClient client, boolean differentSubscriptions) {
        if (!differentSubscriptions) {
            return roleAssignmentsOfCurrentSubscription;
        }

        return client.listRoleAssignmentsBySubscription(targetSubscriptionId);
    }

    private Set<Identity> validateAllMappedIdentities(AzureClient client, CloudFileSystemView cloudFileSystemView,
            ValidationResultBuilder resultBuilder) {
        Set<Identity> validMappedIdentities = Collections.emptySet();
        AccountMappingBase accountMappings = cloudFileSystemView.getAccountMapping();
        if (accountMappings != null) {
            Set<String> mappedIdentityIds = new HashSet<>();
            mappedIdentityIds.addAll(accountMappings.getUserMappings().values());
            mappedIdentityIds.addAll(accountMappings.getGroupMappings().values());
            mappedIdentityIds = mappedIdentityIds.stream()
                    .map(id -> id.replaceFirst("(?i)/resourceGroups/", "/resourcegroups/"))
                    .collect(Collectors.toSet());
            PagedList<Identity> existingIdentities = client.listIdentities();

            Set<String> existingIdentityIds = existingIdentities.stream().map(Identity::id).collect(Collectors.toSet());
            MutableSet<String> nonExistingIdentityIds = Sets.difference(mappedIdentityIds, existingIdentityIds);
            nonExistingIdentityIds.stream().forEach(identityId ->
                    addError(resultBuilder, String.format("Identity with id %s does not exist in the given Azure subscription.", identityId))
            );
            Set<String> validMappedIdentityIds = Sets.difference(mappedIdentityIds, nonExistingIdentityIds);
            validMappedIdentities = existingIdentities.stream().filter(identity -> validMappedIdentityIds.contains(identity.id())).collect(Collectors.toSet());
        }
        return validMappedIdentities;
    }

    private void validateRoleAssigment(PagedList<RoleAssignmentInner> roleAssignments, ValidationResultBuilder resultBuilder, Set<Identity> identities) {
        identities
                .stream()
                .dropWhile(mappedIdentity -> roleAssignments
                        .stream()
                        .anyMatch(roleAssignment -> roleAssignment.principalId().equals(mappedIdentity.principalId())))
                .forEach(identityWithNoAssignment -> addError(resultBuilder,
                        String.format("Identity with id %s has no role assignment.", identityWithNoAssignment.id())));
    }

    private void validateRoleAssigmentAndScope(List<RoleAssignmentInner> roleAssignments, ValidationResultBuilder resultBuilder, Identity identity,
            List<String> scopes, boolean logOnly) {
        if (Objects.nonNull(roleAssignments) && !roleAssignments.isEmpty()) {
            if (!hasMatchingRoles(roleAssignments, identity, scopes)) {
                addErrorOrLog(resultBuilder,
                        String.format("Identity with id %s has no role assignment on scope(s) %s.", identity.id(), scopes), logOnly);
            }
        } else {
            addErrorOrLog(resultBuilder, "There are no role assignments for the given Azure subscription.", logOnly);
        }
    }

    private boolean hasMatchingRoles(List<RoleAssignmentInner> roleAssignments, Identity identity, List<String> scopes) {
        long numberOfMatchingRoles = 0;
        for (String scope : scopes) {
            numberOfMatchingRoles += roleAssignments.stream()
                    .filter(roleAssignment -> roleAssignment.principalId().equals(identity.principalId())
                            && roleAssignment.scope().contains(scope))
                    .count();
        }
        return numberOfMatchingRoles > 0;
    }

    private void addError(ValidationResultBuilder resultBuilder, String msg) {
        LOGGER.info(msg);
        resultBuilder.error(msg);
    }

    private void addErrorOrLog(ValidationResultBuilder resultBuilder, String msg, boolean logOnly) {
        if (logOnly) {
            LOGGER.info("Validation error only logged in this case: " + msg);
        } else {
            LOGGER.info(msg);
            resultBuilder.error(msg);
        }
    }
}
