package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.msi.models.Identity;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceException;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class AzurePermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePermissionValidator.class);

    @Inject
    private AzureRoleDefinitionProvider azureRoleDefinitionProvider;

    public void validateCMKManagedIdentityPermissions(AzureClient azureClient, Identity managedIdentity, Vault vault) {
        Set<RoleDefinition> roleDefinitions = getRoleDefinitionsForVault(azureClient, managedIdentity, vault);
        Set<String> notAllowedDataActions = getNotAllowedDataActions(roleDefinitions);
        Set<String> allowedDataActions = getAllowedDataActions(roleDefinitions);
        List<String> requiredDataActions = azureRoleDefinitionProvider.loadAzureCMKMinimalRoleDefinition().getDataActions();
        ValidationResult validationResult = validatePermission(allowedDataActions, notAllowedDataActions, requiredDataActions, "CMK");
        if (validationResult.hasError()) {
            LOGGER.info("CMK managed identity permission validation result: {}", validationResult.getErrors());
            throw new AzureResourceException(validationResult.getFormattedErrors());
        }
    }

    public void validateFlexibleServerPermission(AzureClient client, DatabaseServer databaseServer) {
        AzureDatabaseType databaseType = getAzureDatabaseType(databaseServer);
        if (AzureDatabaseType.FLEXIBLE_SERVER.equals(databaseType)) {
            validateFlexibleServerPermission(client);
        } else {
            LOGGER.debug("Skip validation because the database server type is {}", databaseType);
        }
    }

    public void validateFlexibleServerPermission(AzureClient client) {
        Set<RoleDefinition> roleDefinitions = getRoleDefinitions(client);
        Set<String> notAllowedActions = getNotAllowedActions(roleDefinitions);
        Set<String> allowedActions = getAllowedActions(roleDefinitions);
        List<String> requiredActions = azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition().getActions();
        ValidationResult validationResult = validatePermission(allowedActions, notAllowedActions, requiredActions, "Flexible Server");
        if (validationResult.hasError()) {
            LOGGER.info("Flexible Server validation result: {}", validationResult.getErrors());
            throw new AzureResourceException(validationResult.getFormattedErrors());
        }
    }

    private Set<RoleDefinition> getRoleDefinitionsForVault(AzureClient azureClient, Identity managedIdentity, Vault vault) {
        return azureClient.listRoleAssignmentsByServicePrincipal(managedIdentity.principalId()).stream()
                .filter(roleAssignment -> vault.id().contains(roleAssignment.scope()))
                .map(RoleAssignment::roleDefinitionId)
                .map(azureClient::getRoleDefinitionById)
                .collect(Collectors.toSet());
    }

    private ValidationResult validatePermission(Set<String> allowedActions, Set<String> notAllowedActions, List<String> requiredActions,
            String validationType) {
        Pair<Set<String>, Set<String>> incorrectPermissions = findMissingPermissions(allowedActions, notAllowedActions, requiredActions);
        ValidationResult.ValidationResultBuilder errors = ValidationResult.builder();
        if (!incorrectPermissions.getRight().isEmpty()) {
            errors.error(String.format("The following required %s action(s) are explicitly denied in your role definition (in 'notActions' section): %s",
                    validationType, incorrectPermissions.getRight()));
        }
        if (!incorrectPermissions.getLeft().isEmpty()) {
            errors.error(String.format("The following required %s action(s) are missing from your role definition: %s",
                    validationType, incorrectPermissions.getLeft()));
        }
        return errors.build();
    }

    private AzureDatabaseType getAzureDatabaseType(DatabaseServer databaseServer) {
        return new AzureDatabaseServerView(databaseServer).getAzureDatabaseType();
    }

    private Set<RoleDefinition> getRoleDefinitions(AzureClient client) {
        try {
            String principalId = client.getServicePrincipalId();
            Set<String> assignedRoleDefinitionIds = getAssignedRoleDefinitionIds(client, principalId);
            return assignedRoleDefinitionIds.stream()
                    .map(client::getRoleDefinitionById)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve role definitions", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private Set<String> getAllowedActions(Set<RoleDefinition> roleDefinitions) {
        return getActions(roleDefinitions, Permission::actions);
    }

    private Set<String> getNotAllowedActions(Set<RoleDefinition> roleDefinitions) {
        return getActions(roleDefinitions, Permission::notActions);
    }

    private Set<String> getAllowedDataActions(Set<RoleDefinition> roleDefinitions) {
        return getActions(roleDefinitions, Permission::dataActions);
    }

    private Set<String> getNotAllowedDataActions(Set<RoleDefinition> roleDefinitions) {
        return getActions(roleDefinitions, Permission::notDataActions);
    }

    private Set<String> getActions(Set<RoleDefinition> roleDefinitions, Function<Permission, List<String>> actionsPorvider) {
        return getPermissions(roleDefinitions)
                .map(actionsPorvider)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private Stream<Permission> getPermissions(Set<RoleDefinition> roleDefinitions) {
        return roleDefinitions.stream()
                .map(RoleDefinition::permissions)
                .flatMap(Set::stream);
    }

    private Set<String> getAssignedRoleDefinitionIds(AzureClient client, String servicePrincipalId) {
        return client.listRoleAssignmentsByServicePrincipal(servicePrincipalId).stream()
                .map(RoleAssignment::roleDefinitionId)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a Pair of missing permissions and denied permissions based on the input parameters
     *
     * @param allowedActions    the allowed actions
     * @param notAllowedActions the denied actions
     * @param requiredActions   the required actions
     * @return a Pair of missing permissions and denied permissions based on the input parameters
     */
    private Pair<Set<String>, Set<String>> findMissingPermissions(Set<String> allowedActions, Set<String> notAllowedActions, List<String> requiredActions) {
        if (allowedActions.contains("*") && CollectionUtils.isEmpty(notAllowedActions)) {
            LOGGER.debug("All action is allowed because a \"*\" is present in the role definition");
            return Pair.of(Collections.emptySet(), Collections.emptySet());
        } else {
            Set<String> allowedActionsRegex = convertAllowedActionsToRegex(allowedActions);

            Set<String> deniedPermissions = notAllowedActions.stream()
                    .filter(requiredActions::contains)
                    .collect(Collectors.toSet());

            Set<String> missingPermissions = requiredActions.stream()
                    .filter(requiredAction -> isMissing(allowedActionsRegex, requiredAction))
                    .collect(Collectors.toSet());
            return Pair.of(missingPermissions, deniedPermissions);
        }
    }

    private Set<String> convertAllowedActionsToRegex(Set<String> allowedActions) {
        return allowedActions.stream()
                .map(action -> action.replace(".", "\\.").replace("*", ".*"))
                .collect(Collectors.toSet());
    }

    private boolean isMissing(Set<String> allowedActions, String requiredAction) {
        return allowedActions.stream().noneMatch(requiredAction::matches);
    }
}
