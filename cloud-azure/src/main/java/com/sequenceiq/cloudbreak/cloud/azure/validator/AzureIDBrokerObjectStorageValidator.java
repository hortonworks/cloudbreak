package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.cloudbreak.cloud.azure.util.AzureValidationMessageUtil.AzureMessageResourceType.IDENTITY;
import static com.sequenceiq.cloudbreak.cloud.azure.util.AzureValidationMessageUtil.AzureMessageResourceType.STORAGE_LOCATION;
import static com.sequenceiq.cloudbreak.cloud.azure.util.AzureValidationMessageUtil.getAdviceMessage;
import static com.sequenceiq.cloudbreak.cloud.azure.util.AzureValidationMessageUtil.getIdentityType;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.Scope.managementGroup;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.Scope.resource;
import static com.sequenceiq.common.model.CloudIdentityType.ID_BROKER;
import static com.sequenceiq.common.model.CloudIdentityType.LOG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.authorization.fluent.models.RoleAssignmentInner;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.factory.Sets;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResultFactory;
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

    private static final Pattern STORAGE_ACCOUNT_NAME_PATTERN = Pattern.compile("@(.*?)\\.dfs\\.core\\.windows\\.net");

    @Inject
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureListResultFactory azureListResultFactory;

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public ValidationResult validateObjectStorage(AzureClient client, String accountId,
            SpiFileSystem spiFileSystem,
            String logsLocationBase,
            String backupLocationBase,
            String singleResourceGroupName,
            ValidationResultBuilder resultBuilder) {
        LOGGER.info("Validating Azure identities...");
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        validateHierarchicalNamespace(client, spiFileSystem, logsLocationBase, backupLocationBase, resultBuilder);
        if (Objects.nonNull(cloudFileSystems) && cloudFileSystems.size() > 0) {
            for (CloudFileSystemView cloudFileSystemView : cloudFileSystems) {
                CloudAdlsGen2View cloudFileSystem = (CloudAdlsGen2View) cloudFileSystemView;
                String managedIdentityId = cloudFileSystem.getManagedIdentity();
                Identity identity = client.getIdentityById(managedIdentityId);
                CloudIdentityType cloudIdentityType = cloudFileSystem.getCloudIdentityType();
                if (identity != null) {
                    if (ID_BROKER.equals(cloudIdentityType)) {
                        List<RoleAssignmentInner> roleAssignments;
                        Optional<ResourceGroup> singleResourceGroup;
                        if (singleResourceGroupName != null) {
                            ResourceGroup resourceGroup = client.getResourceGroup(singleResourceGroupName);
                            roleAssignments = client.listRoleAssignmentsByScopeInner(resourceGroup.id());
                            singleResourceGroup = Optional.of(resourceGroup);
                        } else {
                            roleAssignments = client.listRoleAssignments().getAll();
                            singleResourceGroup = Optional.empty();
                        }

                        validateIDBroker(client, roleAssignments, identity, cloudFileSystem, singleResourceGroup, resultBuilder);
                        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)) {
                            Set<Identity> allMappedExistingIdentity = validateAllMappedIdentities(client, cloudFileSystem, resultBuilder);
                            validateLocation(client, allMappedExistingIdentity,
                                    (StringUtils.isNotEmpty(backupLocationBase)) ? backupLocationBase : logsLocationBase,
                                    resultBuilder);
                        }
                    } else if (LOG.equals(cloudIdentityType)) {
                        validateLocation(client, identity, logsLocationBase, resultBuilder);
                        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)) {
                            validateLocation(client, identity, backupLocationBase, resultBuilder);
                        }
                    }
                } else {
                    addError(resultBuilder, String.format("%s Identity with id %s does not exist in the given Azure subscription. %s",
                            getIdentityType(cloudIdentityType), managedIdentityId, getAdviceMessage(IDENTITY, cloudIdentityType)));
                }
            }
        }
        return resultBuilder.build();
    }

    private void validateHierarchicalNamespace(AzureClient client, SpiFileSystem spiFileSystem,
            String logsLocationBase, String backupLocationBase,
            ValidationResultBuilder resultBuilder) {
        for (String storageAccountName : getStorageAccountNames(spiFileSystem, logsLocationBase, backupLocationBase)) {
            Optional<StorageAccount> storageAccount = client.getStorageAccount(storageAccountName, Kind.STORAGE_V2);
            boolean hierarchical = storageAccount.map(StorageAccount::isHnsEnabled).orElse(false);
            if (storageAccount.isPresent() && !hierarchical) {
                addError(resultBuilder, String.format("Hierarchical namespace is not allowed for Storage Account '%s'.", storageAccountName));
            }
        }
    }

    private Set<String> getStorageAccountNames(SpiFileSystem spiFileSystem, String logsLocationBase, String backupLocationBase) {
        Set<String> locations = spiFileSystem.getCloudFileSystems().stream()
                .flatMap(cloudFileSystemView -> cloudFileSystemView.getLocations().stream())
                .map(StorageLocationBase::getValue)
                .collect(Collectors.toSet());
        if (StringUtils.isNotEmpty(logsLocationBase)) {
            locations.add(logsLocationBase);
        }
        if (StringUtils.isNotEmpty(backupLocationBase)) {
            locations.add(backupLocationBase);
        }
        return extractStorageAccount(locations);
    }

    private Set<String> extractStorageAccount(Set<String> locations) {
        Set<String> storageAccountNames = new HashSet<>();
        for (String location : locations) {
            Matcher m = STORAGE_ACCOUNT_NAME_PATTERN.matcher(location);
            if (m.find()) {
                storageAccountNames.add(m.group(1));
            }
        }
        return storageAccountNames;
    }

    private void validateIDBroker(AzureClient client, List<RoleAssignmentInner> roleAssignments, Identity identity,
            CloudAdlsGen2View cloudFileSystem, Optional<ResourceGroup> singleResourceGroup, ValidationResultBuilder resultBuilder) {
        LOGGER.debug(String.format("Validating IDBroker identity %s", identity.name()));

        Set<Identity> allMappedExistingIdentity = validateAllMappedIdentities(client, cloudFileSystem, resultBuilder);

        validateRoleAssigment(roleAssignments, resultBuilder, Set.of(identity));
        validateRoleAssigmentAndScope(roleAssignments, resultBuilder, identity,
                getScopesForIDBrokerValidation(client.getCurrentSubscription().subscriptionId(), singleResourceGroup),
                false, cloudFileSystem.getCloudIdentityType());

        List<StorageLocationBase> locations = cloudFileSystem.getLocations();
        if (Objects.nonNull(locations) && !locations.isEmpty()) {
            validateStorageAccount(client, allMappedExistingIdentity, locations.get(0).getValue(), ID_BROKER, resultBuilder);
        } else {
            LOGGER.debug("There is no storage location set for logger identity, this should not happen!");
        }
        LOGGER.debug("Validating IDBroker identity is finished");

    }

    private List<Scope> getScopesForIDBrokerValidation(String subscriptionId, Optional<ResourceGroup> singleResourceGroup) {
        List<Scope> scopes = new ArrayList<>();
        scopes.add(resource("/subscriptions/" + subscriptionId));
        singleResourceGroup.ifPresent(rg -> scopes.add(resource(rg.id())));
        scopes.add(managementGroup());
        return scopes;
    }

    private void validateLocation(AzureClient client, Set<Identity> identities, String locationBase, ValidationResultBuilder resultBuilder) {
        identities.stream().forEach(identity -> {
            validateLocation(client, identity, locationBase, resultBuilder);
        });
    }

    private void validateLocation(AzureClient client, Identity identity, String locationBase, ValidationResultBuilder resultBuilder) {
        if (StringUtils.isNotEmpty(locationBase)) {
            validateStorageAccount(client, Set.of(identity), locationBase, LOG, resultBuilder);
        } else {
            LOGGER.debug("There is no storage location set for identity, this should not happen!");
        }
        LOGGER.info("Validating identity {} is finished", identity.name());
    }

    private void validateStorageAccount(AzureClient client, Set<Identity> identities, String location, CloudIdentityType cloudIdentityType,
            ValidationResultBuilder resultBuilder) {
        identities.stream().forEach(identity -> {
            LOGGER.debug(String.format("Validating identity on %s Location: %s", identity.name(), location));
        });
        AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(location);
        String storageAccountName = adlsGen2Config.getAccount();
        Optional<String> storageAccountIdOptional = azureStorage.findStorageAccountIdInVisibleSubscriptions(client, storageAccountName);
        if (storageAccountIdOptional.isEmpty()) {
            LOGGER.debug("Storage account {} not found or insufficient permission to list subscriptions and / or storage accounts.", storageAccountName);
            addError(resultBuilder, String.format("Storage account with name %s not found in the given Azure subscription. %s",
                    storageAccountName, getAdviceMessage(STORAGE_LOCATION, cloudIdentityType)));
            return;
        }
        List<RoleAssignmentInner> roleAssignments = client.listRoleAssignmentsByScopeInner(storageAccountIdOptional.get());
        ResourceId storageAccountResourceId = ResourceId.fromString(storageAccountIdOptional.get());
        boolean differentSubscriptions = !client.getCurrentSubscription().subscriptionId().equals(storageAccountResourceId.subscriptionId());
        List<RoleAssignmentInner> roleAssignmentsForSubscription =
                getRoleAssignmentsOfSubscription(roleAssignments, storageAccountResourceId.subscriptionId(), client, differentSubscriptions);
        for (Identity identity : identities) {
            validateRoleAssigmentAndScope(roleAssignmentsForSubscription, resultBuilder, identity,
                    List.of(resource(adlsGen2Config.getFileSystem()),
                            resource(storageAccountName),
                            resource(storageAccountResourceId.resourceGroupName()),
                            resource(storageAccountResourceId.subscriptionId())),
                    differentSubscriptions, cloudIdentityType);
        }
    }

    private List<RoleAssignmentInner> getRoleAssignmentsOfSubscription(
            List<RoleAssignmentInner> roleAssignmentsOfCurrentSubscription, String targetSubscriptionId, AzureClient client, boolean differentSubscriptions) {
        if (!differentSubscriptions) {
            return roleAssignmentsOfCurrentSubscription;
        }

        return azureListResultFactory.create(client.listRoleAssignmentsBySubscription(targetSubscriptionId)).getAll();
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
            List<Identity> existingIdentities = client.listIdentities().getAll();

            Set<String> existingIdentityIds = existingIdentities.stream().map(Identity::id).collect(Collectors.toSet());
            MutableSet<String> nonExistingIdentityIds = Sets.difference(mappedIdentityIds, existingIdentityIds);
            nonExistingIdentityIds.stream().forEach(identityId ->
                    addError(resultBuilder, String.format("Identity with id %s does not exist in the given Azure subscription. %s",
                            identityId, getAdviceMessage(IDENTITY, ID_BROKER)))
            );
            Set<String> validMappedIdentityIds = Sets.difference(mappedIdentityIds, nonExistingIdentityIds);
            validMappedIdentities = existingIdentities.stream().filter(identity -> validMappedIdentityIds.contains(identity.id())).collect(Collectors.toSet());
        }
        return validMappedIdentities;
    }

    private void validateRoleAssigment(List<RoleAssignmentInner> roleAssignments, ValidationResultBuilder resultBuilder, Set<Identity> identities) {
        identities
                .stream()
                .dropWhile(mappedIdentity -> roleAssignments
                        .stream()
                        .anyMatch(roleAssignment -> roleAssignment.principalId().equals(mappedIdentity.principalId())))
                .forEach(identityWithNoAssignment -> addError(resultBuilder,
                        String.format("Identity with id %s has no role assignment. %s",
                                identityWithNoAssignment.id(), getAdviceMessage(IDENTITY, ID_BROKER))));
    }

    private void validateRoleAssigmentAndScope(List<RoleAssignmentInner> roleAssignments, ValidationResultBuilder resultBuilder, Identity identity,
            List<Scope> scopes, boolean logOnly, CloudIdentityType cloudIdentityType) {
        if (Objects.nonNull(roleAssignments) && !roleAssignments.isEmpty()) {
            if (!hasMatchingRoles(roleAssignments, identity, scopes)) {
                addErrorOrLog(resultBuilder,
                        String.format("Identity with id %s has no role assignment on scope(s) %s. %s",
                                identity.id(), scopes, getAdviceMessage(IDENTITY, cloudIdentityType)), logOnly);
            }
        } else {
            addErrorOrLog(resultBuilder, String.format("There are no role assignments for the given Azure subscription. %s",
                    getAdviceMessage(IDENTITY, cloudIdentityType)), logOnly);
        }
    }

    private boolean hasMatchingRoles(List<RoleAssignmentInner> roleAssignments, Identity identity, List<Scope> scopes) {
        long numberOfMatchingRoles = 0;
        for (Scope scope : scopes) {
            numberOfMatchingRoles += roleAssignments.stream()
                    .filter(roleAssignment -> roleAssignment.principalId().equals(identity.principalId())
                            && scope.match(roleAssignment.scope()))
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
