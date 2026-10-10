package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
        List<RoleAssignment> roleAssignments = azureClient.listRoleAssignmentsByServicePrincipal(managedIdentity.principalId());
        String targetScope = vault.id();
        List<String> requiredDataActions = azureRoleDefinitionProvider.loadAzureCMKMinimalRoleDefinition().getDataActions();
        List<RoleDefinition> applicableRoles = getApplicableRoleDefinitions(roleAssignments, azureClient, targetScope);
        Pair<Set<String>, Set<String>> result = findMissingPermissions(requiredDataActions, applicableRoles,
                Permission::dataActions, Permission::notDataActions);
        ValidationResult validationResult = reportValidationErrors(result, "CMK");
        if (validationResult.hasError()) {
            LOGGER.info("CMK managed identity permission validation result: {}", validationResult.getErrors());
            throw new AzureResourceException(validationResult.getFormattedErrors());
        }
    }

    public void validateFlexibleServerPermission(AzureClient client, DatabaseServer databaseServer, String targetScope) {
        AzureDatabaseType databaseType = getAzureDatabaseType(databaseServer);
        if (AzureDatabaseType.FLEXIBLE_SERVER.equals(databaseType)) {
            validateFlexibleServerPermission(client, targetScope);
        } else {
            LOGGER.debug("Skip validation because the database server type is {}", databaseType);
        }
    }

    public void validateFlexibleServerPermission(AzureClient client, String targetScope) {
        List<RoleAssignment> roleAssignments = listRoleAssignments(client);
        List<String> requiredActions = azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition().getActions();
        List<RoleDefinition> applicableRoles = getApplicableRoleDefinitions(roleAssignments, client, targetScope);
        Pair<Set<String>, Set<String>> result = findMissingPermissions(requiredActions, applicableRoles,
                Permission::actions, Permission::notActions);
        ValidationResult validationResult = reportValidationErrors(result, "Flexible Server");
        if (validationResult.hasError()) {
            LOGGER.info("Flexible Server validation result: {}", validationResult.getErrors());
            throw new AzureResourceException(validationResult.getFormattedErrors());
        }
    }

    /**
     * Diffs the actions the tag-update flow needs against the role definitions assigned to the credential's service
     * principal. Returned as data instead of a thrown {@link AzureResourceException} because the caller has to report
     * the individual actions in a structured payload.
     *
     * @return a Pair of the missing actions and the explicitly denied ('notActions') ones
     */
    public Pair<Set<String>, Set<String>> findMissingTagUpdatePermissions(AzureClient client) {
        List<RoleAssignment> roleAssignments = listRoleAssignments(client);
        List<String> requiredActions = azureRoleDefinitionProvider.loadAzureTagUpdateMinimalRoleDefinition().getActions();
        List<RoleDefinition> applicableRoles = getApplicableRoleDefinitions(roleAssignments, client, null);
        return findMissingPermissions(requiredActions, applicableRoles, Permission::actions, Permission::notActions);
    }

    private List<RoleAssignment> listRoleAssignments(AzureClient client) {
        try {
            String principalId = client.getServicePrincipalId();
            return client.listRoleAssignmentsByServicePrincipal(principalId);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve role assignments", e);
            throw new CloudbreakServiceException(e);
        }
    }

    private List<RoleDefinition> getApplicableRoleDefinitions(List<RoleAssignment> roleAssignments, AzureClient client, String targetScope) {
        Set<String> applicableRoleDefIds = roleAssignments.stream()
                .filter(assignment -> isScopeApplicable(assignment.scope(), targetScope))
                .map(RoleAssignment::roleDefinitionId)
                .collect(Collectors.toSet());
        return applicableRoleDefIds.stream()
                .map(client::getRoleDefinitionById)
                .collect(Collectors.toList());
    }

    /**
     * Returns a Pair of (missing actions, denied actions).
     * Missing: not matched by any Action pattern in any applicable role.
     * Denied: matched by an Action pattern but excluded by NotActions in every role that grants it.
     */
    private Pair<Set<String>, Set<String>> findMissingPermissions(
            List<String> requiredActions,
            List<RoleDefinition> applicableRoles,
            Function<Permission, List<String>> actionsExtractor,
            Function<Permission, List<String>> notActionsExtractor) {
        Set<String> missingActions = new LinkedHashSet<>();
        Set<String> deniedActions = new LinkedHashSet<>();
        for (String required : requiredActions) {
            if (!isEffectivelyGranted(required, applicableRoles, actionsExtractor, notActionsExtractor)) {
                if (isMatchedByAnyAction(required, applicableRoles, actionsExtractor)) {
                    deniedActions.add(required);
                } else {
                    missingActions.add(required);
                }
            }
        }
        return Pair.of(missingActions, deniedActions);
    }

    private boolean isEffectivelyGranted(
            String action,
            List<RoleDefinition> roleDefinitions,
            Function<Permission, List<String>> actionsExtractor,
            Function<Permission, List<String>> notActionsExtractor) {
        for (RoleDefinition roleDef : roleDefinitions) {
            for (Permission permission : roleDef.permissions()) {
                if (matchesAny(action, actionsExtractor.apply(permission))
                        && !matchesAny(action, notActionsExtractor.apply(permission))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMatchedByAnyAction(
            String action,
            List<RoleDefinition> roleDefinitions,
            Function<Permission, List<String>> actionsExtractor) {
        for (RoleDefinition roleDef : roleDefinitions) {
            for (Permission permission : roleDef.permissions()) {
                if (matchesAny(action, actionsExtractor.apply(permission))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAny(String action, List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> matchesActionPattern(action, pattern));
    }

    boolean matchesActionPattern(String action, String pattern) {
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return action.matches(regex);
    }

    boolean isScopeApplicable(String assignmentScope, String targetScope) {
        if (targetScope == null || assignmentScope == null) {
            return true;
        }
        String normalizedAssignment = assignmentScope.toLowerCase(Locale.ROOT);
        String normalizedTarget = targetScope.toLowerCase(Locale.ROOT);
        return normalizedTarget.equals(normalizedAssignment)
                || normalizedTarget.startsWith(normalizedAssignment + "/");
    }

    private ValidationResult reportValidationErrors(Pair<Set<String>, Set<String>> missingAndDenied, String validationType) {
        ValidationResult.ValidationResultBuilder errors = ValidationResult.builder();
        if (!missingAndDenied.getRight().isEmpty()) {
            errors.error(String.format("The following required %s action(s) are explicitly denied in your role definition (in 'notActions' section): %s",
                    validationType, missingAndDenied.getRight()));
        }
        if (!missingAndDenied.getLeft().isEmpty()) {
            errors.error(String.format("The following required %s action(s) are missing from your role definition: %s",
                    validationType, missingAndDenied.getLeft()));
        }
        return errors.build();
    }

    private AzureDatabaseType getAzureDatabaseType(DatabaseServer databaseServer) {
        return new AzureDatabaseServerView(databaseServer).getAzureDatabaseType();
    }
}
