package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;

/**
 * The environment tag-modification flow is preceded by a permission pre-flight that checks the actions of
 * {@code definitions/azure-tag-update-minimal-role-def.json} against the roles assigned to the credential. A
 * credential following one of the documented roles must therefore pass that check, otherwise tag propagation is
 * rejected up front with HTTP 400 even though the customer granted exactly what Cloudera asked for.
 *
 * <p>Add-on roles (CMK, flexible server) are assigned next to one of these roles, so they are not checked on their
 * own.</p>
 */
class AzureTagUpdateRoleCoverageTest {

    private static final String DEFAULT_ROLE = "definitions/azure-role-def.json";

    private static final String MINIMAL_ROLE = "definitions/azure-minimal-role-def.json";

    // CB-35253 decides how resource-group tags are updated (and so which action the minimal role needs);
    // drop the exclusion once the minimal role and the tag-update manifest are aligned there.
    private static final Map<String, Set<String>> KNOWN_EXCLUSIONS = Map.of(
            MINIMAL_ROLE, Set.of("Microsoft.Resources/subscriptions/resourceGroups/write"));

    private final AzureRoleDefinitionProvider roleDefinitionProvider = new AzureRoleDefinitionProvider();

    private final AzurePermissionValidator permissionValidator = new AzurePermissionValidator();

    @ParameterizedTest
    @ValueSource(strings = {DEFAULT_ROLE, MINIMAL_ROLE})
    void roleGrantsEveryTagUpdateAction(String rolePath) {
        AzureRoleDefinitionProperties role = roleDefinitionProvider.loadAzureRoleDefinition(rolePath);
        Set<String> exclusions = KNOWN_EXCLUSIONS.getOrDefault(rolePath, Set.of());

        List<String> notGranted = roleDefinitionProvider.loadAzureTagUpdateMinimalRoleDefinition().getActions().stream()
                .filter(action -> !exclusions.contains(action))
                .filter(action -> !isGranted(action, role))
                .toList();
        List<String> staleExclusions = exclusions.stream()
                .filter(action -> isGranted(action, role))
                .toList();

        assertThat(notGranted).as("tag-update actions not granted by %s", rolePath).isEmpty();
        assertThat(staleExclusions).as("known exclusions already granted by %s", rolePath).isEmpty();
    }

    private boolean isGranted(String action, AzureRoleDefinitionProperties role) {
        return matchesAny(action, role.getActions()) && !matchesAny(action, role.getNotActions());
    }

    private boolean matchesAny(String action, List<String> patterns) {
        return patterns != null && patterns.stream().anyMatch(pattern -> permissionValidator.matchesActionPattern(action, pattern));
    }
}
