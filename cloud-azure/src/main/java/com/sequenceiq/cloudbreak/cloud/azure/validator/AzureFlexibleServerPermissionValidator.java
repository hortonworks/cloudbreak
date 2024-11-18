package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceException;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class AzureFlexibleServerPermissionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFlexibleServerPermissionValidator.class);

    @Inject
    private AzureFlexibleServerRoleDefinitionProvider azureFlexibleServerRoleDefinitionProvider;

    public void validate(AzureClient client, DatabaseServer databaseServer) {
        AzureDatabaseType databaseType = getAzureDatabaseType(databaseServer);
        if (AzureDatabaseType.FLEXIBLE_SERVER.equals(databaseType)) {
            validatePermission(client);
        } else {
            LOGGER.debug("Skip validation because the database server type is {}", databaseType);
        }
    }

    public void validatePermission(AzureClient client) {
        Set<RoleDefinition> roleDefinitions = getRoleDefinitions(client);
        Set<String> notAllowedActions = getNotAllowedActions(roleDefinitions);
        Set<String> allowedActions = getAllowedActions(roleDefinitions);
        Pair<Set<String>, Set<String>> incorrectPermissions = findMissingPermissions(allowedActions, notAllowedActions);
        ValidationResult.ValidationResultBuilder errors = ValidationResult.builder();
        if (!incorrectPermissions.getRight().isEmpty()) {
            errors.error("The following required action(s) are explicitly denied in your role definition (in 'notActions' section): " +
                    incorrectPermissions.getRight());
        }
        if (!incorrectPermissions.getLeft().isEmpty()) {
            errors.error("The following required action(s) are missing from your role definition: " + incorrectPermissions.getLeft());
        }
        ValidationResult result = errors.build();
        if (result.hasError()) {
            LOGGER.info("Flexible Server validation result: {}", result.getErrors());
            throw new AzureResourceException(result.getFormattedErrors());
        }
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
        return getPermissions(roleDefinitions)
                .map(Permission::actions)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> getNotAllowedActions(Set<RoleDefinition> roleDefinitions) {
        return getPermissions(roleDefinitions)
                .map(Permission::notActions)
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

    private Pair<Set<String>, Set<String>> findMissingPermissions(Set<String> allowedActions, Set<String> notAllowedActions) {
        if (allowedActions.contains("*") && CollectionUtils.isEmpty(notAllowedActions)) {
            LOGGER.debug("All action is allowed because a \"*\" is present in the role definition");
            return Pair.of(Collections.emptySet(), Collections.emptySet());
        } else {
            Set<String> allowedActionsRegex = convertAllowedActionsToRegex(allowedActions);
            List<String> requiredActions = azureFlexibleServerRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition().getActions();

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