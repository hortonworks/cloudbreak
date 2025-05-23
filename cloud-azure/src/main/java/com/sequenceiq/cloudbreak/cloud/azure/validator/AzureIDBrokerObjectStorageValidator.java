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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.authorization.fluent.models.RoleAssignmentInner;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.factory.Sets;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResultFactory;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureClientCachedOperations;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.service.identitymapping.AccountMappingSubject;
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
    private AzureUtils azureUtils;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureListResultFactory azureListResultFactory;

    @Inject
    private AzureClientCachedOperations azureClientCachedOperations;

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public ValidationResult validateObjectStorage(AzureClient client, String accountId,
            SpiFileSystem spiFileSystem,
            ObjectStorageValidateRequest objectStorageValidateRequest,
            String singleResourceGroupName,
            ValidationResultBuilder resultBuilder) {
        LOGGER.info("Validating Azure identities...");
        List<CloudFileSystemView> cloudFileSystems = spiFileSystem.getCloudFileSystems();
        String logsLocationBase = objectStorageValidateRequest.getLogsLocationBase();
        String backupLocationBase = objectStorageValidateRequest.getBackupLocationBase();
        validateHierarchicalNamespace(client, spiFileSystem, logsLocationBase, backupLocationBase, accountId, resultBuilder);
        if (Objects.nonNull(cloudFileSystems) && !cloudFileSystems.isEmpty()) {
            for (CloudFileSystemView cloudFileSystemView : cloudFileSystems) {
                CloudAdlsGen2View cloudFileSystem = (CloudAdlsGen2View) cloudFileSystemView;
                String managedIdentityId = cloudFileSystem.getManagedIdentity();
                Identity identity = client.getIdentityById(managedIdentityId);
                CloudIdentityType cloudIdentityType = cloudFileSystem.getCloudIdentityType();
                if (identity != null) {
                    if (ID_BROKER.equals(cloudIdentityType)) {
                        List<Identity> existingIdentities = client.listIdentities().getAll();
                        validateIDBroker(client, identity, cloudFileSystem, singleResourceGroupName, accountId, existingIdentities, resultBuilder);
                        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)) {
                            Set<Identity> existingServiceIdentities = getServiceIdentities(cloudFileSystem.getAccountMapping(), existingIdentities);
                            String actualBackupLocationBase = StringUtils.isNotEmpty(backupLocationBase) ? backupLocationBase : logsLocationBase;
                            validateLocation(client, existingServiceIdentities, actualBackupLocationBase, accountId, resultBuilder);
                        }
                    } else if (LOG.equals(cloudIdentityType)) {
                        validateLocation(client, identity, logsLocationBase, accountId, resultBuilder);
                        if (entitlementService.isDatalakeBackupRestorePrechecksEnabled(accountId)
                                && !objectStorageValidateRequest.getSkipLogRoleValidationforBackup()
                                && StringUtils.isNotEmpty(backupLocationBase)
                                && !backupLocationBase.equals(logsLocationBase)) {
                            validateLocation(client, identity, backupLocationBase, accountId, resultBuilder);
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
            String accountId, ValidationResultBuilder resultBuilder) {
        for (String storageAccountName : getStorageAccountNames(spiFileSystem, logsLocationBase, backupLocationBase)) {
            Optional<StorageAccount> storageAccount = azureClientCachedOperations.getStorageAccount(
                    client,
                    accountId,
                    storageAccountName,
                    azureUtils.getSupportedAzureStorageKinds());
            boolean hierarchical = storageAccount.map(StorageAccount::isHnsEnabled).orElse(false);
            if (storageAccount.isPresent() && !hierarchical) {
                addError(resultBuilder, String.format("Hierarchical namespace is mandatory for Storage Account '%s'. " +
                        "Please create an ADLS Gen2 storage account with hierarchical namespace enabled. " +
                        "The storage account must be in the same region as the environment.", storageAccountName));
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

    private void validateIDBroker(AzureClient client, Identity identity, CloudAdlsGen2View cloudFileSystem, String singleResourceGroupName,
            String accountId, List<Identity> existingIdentities, ValidationResultBuilder resultBuilder) {
        LOGGER.debug(String.format("Validating IDBroker identity %s", identity.name()));

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

        validateAllMappedIdentities(client, cloudFileSystem, existingIdentities, resultBuilder);

        validateRoleAssigment(roleAssignments, resultBuilder, Set.of(identity));
        validateRoleAssigmentAndScope(roleAssignments, resultBuilder, identity,
                getScopesForIDBrokerValidation(client.getCurrentSubscription().subscriptionId(), singleResourceGroup),
                false, cloudFileSystem.getCloudIdentityType());

        List<StorageLocationBase> locations = cloudFileSystem.getLocations();
        if (Objects.nonNull(locations) && !locations.isEmpty()) {
            validateStorageAccount(client, getServiceIdentities(cloudFileSystem.getAccountMapping(), existingIdentities),
                    locations.get(0).getValue(), ID_BROKER, accountId, resultBuilder);
        } else {
            LOGGER.warn("There is no storage location set for IDBroker identity, this should not happen!");
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

    private void validateLocation(AzureClient client, Set<Identity> identities, String locationBase, String accountId, ValidationResultBuilder resultBuilder) {
        identities.stream().forEach(identity -> {
            validateLocation(client, identity, locationBase, accountId, resultBuilder);
        });
    }

    private void validateLocation(AzureClient client, Identity identity, String locationBase, String accountId, ValidationResultBuilder resultBuilder) {
        if (StringUtils.isNotEmpty(locationBase)) {
            validateStorageAccount(client, Set.of(identity), locationBase, LOG, accountId, resultBuilder);
        } else {
            LOGGER.warn("There is no storage location set for identity {}, this should not happen!", identity.name());
        }
        LOGGER.info("Validating identity {} is finished", identity.name());
    }

    private void validateStorageAccount(AzureClient client, Set<Identity> identities, String location, CloudIdentityType cloudIdentityType,
            String accountId, ValidationResultBuilder resultBuilder) {
        identities.stream().forEach(identity -> {
            LOGGER.debug("Validating identity on {} Location: {}", identity.name(), location);
        });
        AdlsGen2Config adlsGen2Config = adlsGen2ConfigGenerator.generateStorageConfig(location);
        String storageAccountName = adlsGen2Config.getAccount();
        Optional<String> storageAccountIdOptional = azureStorage.findStorageAccountIdInVisibleSubscriptions(client, storageAccountName, accountId);
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

    private void validateAllMappedIdentities(AzureClient client, CloudFileSystemView cloudFileSystemView,
            List<Identity> existingIdentities, ValidationResultBuilder resultBuilder) {
        AccountMappingBase accountMappings = cloudFileSystemView.getAccountMapping();
        Set<String> mappedIdentityIds = getAllMappedIdentityIds(accountMappings);
        Set<String> existingIdentityIds = existingIdentities.stream().map(Identity::id).collect(Collectors.toSet());
        MutableSet<String> nonExistingIdentityIds = Sets.difference(mappedIdentityIds, existingIdentityIds);
        nonExistingIdentityIds.stream().forEach(identityId ->
                addError(resultBuilder, String.format("Identity with id %s does not exist in the given Azure subscription. %s",
                        identityId, getAdviceMessage(IDENTITY, ID_BROKER)))
        );
    }

    private Set<Identity> getServiceIdentities(AccountMappingBase accountMappings, List<Identity> existingIdentities) {
        Set<String> mappedServiceIdentityIds = getMappedServiceIdentityIds(accountMappings);
        return existingIdentities.stream()
                .filter(identity -> mappedServiceIdentityIds.contains(identity.id()))
                .collect(Collectors.toSet());
    }

    private Set<String> getMappedServiceIdentityIds(AccountMappingBase accountMappings) {
        Set<String> mappedServiceIdentityIds = new HashSet<>();
        if (accountMappings != null) {
            mappedServiceIdentityIds.addAll(accountMappings.getUserMappings().entrySet().stream()
                    .filter(entry -> AccountMappingSubject.ALL_SPECIAL_USERS.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet()));
            mappedServiceIdentityIds.addAll(accountMappings.getGroupMappings().entrySet().stream()
                    .filter(entry -> AccountMappingSubject.ALL_SPECIAL_USERS.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet()));
            mappedServiceIdentityIds = mappedServiceIdentityIds.stream()
                    .map(id -> id.replaceFirst("(?i)/resourceGroups/", "/resourcegroups/"))
                    .collect(Collectors.toSet());
        }
        return mappedServiceIdentityIds;
    }

    public Set<String> getAllMappedIdentityIds(AccountMappingBase accountMappings) {
        Set<String> mappedIdentityIds = new HashSet<>();
        if (accountMappings != null) {
            mappedIdentityIds.addAll(accountMappings.getUserMappings().values());
            mappedIdentityIds.addAll(accountMappings.getGroupMappings().values());
            mappedIdentityIds = mappedIdentityIds.stream()
                    .map(id -> id.replaceFirst("(?i)/resourceGroups/", "/resourcegroups/"))
                    .collect(Collectors.toSet());
        }
        return mappedIdentityIds;
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
