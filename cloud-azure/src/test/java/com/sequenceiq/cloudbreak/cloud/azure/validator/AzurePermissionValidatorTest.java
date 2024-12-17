package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.common.model.AzureDatabaseType.AZURE_DATABASE_TYPE_KEY;
import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.msi.models.Identity;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class AzurePermissionValidatorTest {

    private static final String PRINCIPAL_ID = "principal-id";

    private static final String ROLE_DEFINITION_ID = "role-definition-id-1";

    private static final String CMK_MINIMAL_ROLE_DEF_PATH = "json/azure-cmk-minimal-role-def-test.json";

    private static final String CMK_MISSING_ROLE_DEF_PATH = "json/azure-cmk-missing-role-def-test.json";

    private static final String FLEXIBLE_MINIMAL_ROLE_DEF_PATH = "json/azure-flexible-minimal-role-def-test.json";

    private static final String FLEXIBLE_MINIMAL_ROLE_DEF_PATTERN = "json/azure-flexible-minimal-role-def-test-%s.json";

    private static final String DEFAULT_ROLE_DEF_PATH = "json/azure-role-def-test.json";

    private static final String SERVICE_ENDPOINT_MINIMAL_ROLE_DEF_PATH = "json/azure-service-endpoint-role-def-test.json";

    private static final String PRIVATE_ENDPOINT_MINIMAL_ROLE_DEF_PATH = "json/azure-private-endpoint-minimal-role-def-test.json";

    @InjectMocks
    private AzurePermissionValidator underTest;

    @Mock
    private AzureRoleDefinitionProvider azureRoleDefinitionProvider;

    @Mock
    private AzureClient client;

    @BeforeEach
    void before() {
        lenient().when(client.getServicePrincipalId()).thenReturn(PRINCIPAL_ID);
    }

    @Test
    void testShouldNotThrowExceptionWithMinimalRole() throws IOException {
        AzureRoleDefinitionProperties requiredRoles = readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH);
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(requiredRoles.getActions(), List.of());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(requiredRoles);

        underTest.validateFlexibleServerPermission(client, databaseServer);
    }

    @Test
    void testShouldNotThrowExceptionWithCMKMinimalRole() throws IOException {
        AzureRoleDefinitionProperties requiredRoles = readPermissions(CMK_MINIMAL_ROLE_DEF_PATH);
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        when(roleAssignment.scope()).thenReturn("subscription/1");
        RoleDefinition roleDefinition = createRoleDefinitionWithDataActions(requiredRoles.getDataActions(), List.of());
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureCMKMinimalRoleDefinition()).thenReturn(requiredRoles);
        Vault vault = mock(Vault.class);
        when(vault.id()).thenReturn("subscription/1/vaultid");
        Identity identity = mock(Identity.class);
        when(identity.principalId()).thenReturn(PRINCIPAL_ID);

        underTest.validateCMKManagedIdentityPermissions(client, identity, vault);
    }

    @Test
    void testShouldNotThrowExceptionWithCMKMissingRole() throws IOException {
        AzureRoleDefinitionProperties requiredRoles = readPermissions(CMK_MINIMAL_ROLE_DEF_PATH);
        AzureRoleDefinitionProperties actualRoles = readPermissions(CMK_MISSING_ROLE_DEF_PATH);
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        when(roleAssignment.scope()).thenReturn("subscription/1/vaultid");
        RoleDefinition roleDefinition = createRoleDefinitionWithDataActions(actualRoles.getDataActions(), List.of());
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureCMKMinimalRoleDefinition()).thenReturn(requiredRoles);
        Vault vault = mock(Vault.class);
        when(vault.id()).thenReturn("subscription/1/vaultid");
        Identity identity = mock(Identity.class);
        when(identity.principalId()).thenReturn(PRINCIPAL_ID);

        AzureResourceException actualException = assertThrows(AzureResourceException.class,
                () -> underTest.validateCMKManagedIdentityPermissions(client, identity, vault));

        assertEquals("The following required CMK action(s) are missing from your role definition: [Microsoft.KeyVault/vaults/keys/wrap/action]",
                actualException.getMessage());
    }

    @Test
    void testShouldNotThrowExceptionAvailableRolesContainsStar() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(List.of("*"), List.of());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        underTest.validateFlexibleServerPermission(client, databaseServer);
    }

    @Test
    void testShouldThrowExceptionWithDefaultRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(DEFAULT_ROLE_DEF_PATH).getActions(), List.of());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validateFlexibleServerPermission(client, databaseServer));

        assertEquals("The following required Flexible Server action(s) are missing from your role definition: " +
                "[Microsoft.DBforPostgreSQL/flexibleServers/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write, Microsoft.DBforPostgreSQL/flexibleServers/configurations/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/delete, Microsoft.DBforPostgreSQL/flexibleServers/start/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/read, Microsoft.DBforPostgreSQL/flexibleServers/privateEndpointConnectionsApproval/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldThrowExceptionWithServiceEndpointMinimalRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(SERVICE_ENDPOINT_MINIMAL_ROLE_DEF_PATH).getActions(), List.of());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validateFlexibleServerPermission(client, databaseServer));

        assertEquals("The following required Flexible Server action(s) are missing from your role definition: " +
                "[Microsoft.DBforPostgreSQL/flexibleServers/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write, Microsoft.DBforPostgreSQL/flexibleServers/configurations/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/delete, Microsoft.DBforPostgreSQL/flexibleServers/start/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/read, Microsoft.DBforPostgreSQL/flexibleServers/privateEndpointConnectionsApproval/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldThrowExceptionWithPrivateEndpointMinimalRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(PRIVATE_ENDPOINT_MINIMAL_ROLE_DEF_PATH).getActions(), List.of());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validateFlexibleServerPermission(client, databaseServer));

        assertEquals("The following required Flexible Server action(s) are missing from your role definition: " +
                "[Microsoft.DBforPostgreSQL/flexibleServers/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write, Microsoft.DBforPostgreSQL/flexibleServers/configurations/write, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/delete, Microsoft.DBforPostgreSQL/flexibleServers/start/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/read, Microsoft.DBforPostgreSQL/flexibleServers/privateEndpointConnectionsApproval/action, " +
                "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldNotThrowExceptionWhenTheDatabaseTypeIsSingleServer() {
        underTest.validateFlexibleServerPermission(client, createDatabaseServer(SINGLE_SERVER));

        verifyNoInteractions(client, azureRoleDefinitionProvider);
    }

    @ParameterizedTest
    @MethodSource("flexibleServerTestDataProvider")
    void testDifferentScenariosShouldThrowExceptionWithDifferentRoles(Integer ordinal, String expectedError) throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        AzureRoleDefinitionProperties roleDefinitionProperties = readPermissions(String.format(FLEXIBLE_MINIMAL_ROLE_DEF_PATTERN, ordinal));
        RoleDefinition roleDefinition = createRoleDefinition(roleDefinitionProperties.getActions(), roleDefinitionProperties.getNotActions());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validateFlexibleServerPermission(client, databaseServer));

        assertEquals(expectedError, exception.getMessage());
    }

    public static Stream<Arguments> flexibleServerTestDataProvider() {
        return Stream.of(
                Arguments.of(1, "The following required Flexible Server action(s) are explicitly denied in your role definition (in 'notActions' section): " +
                        "[Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write, Microsoft.DBforPostgreSQL/flexibleServers/configurations/write]"),
                Arguments.of(2, "The following required Flexible Server action(s) are missing from your role definition: " +
                        "[Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write]"),
                Arguments.of(3, "1. The following required Flexible Server action(s) are explicitly denied in your role definition " +
                        "(in 'notActions' section): [Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write]\n" +
                        "2. The following required Flexible Server action(s) are missing from your role definition: " +
                        "[Microsoft.DBforPostgreSQL/flexibleServers/firewallRules/write]")
        );
    }

    private RoleAssignment createRoleAssignment(String roleDefinitionId) {
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.roleDefinitionId()).thenReturn(roleDefinitionId);
        return roleAssignment;
    }

    private RoleDefinition createRoleDefinition(List<String> allowedActions, List<String> notAllowedActions) {
        RoleDefinition roleDefinition = mock(RoleDefinition.class);
        Permission permission = mock(Permission.class);
        when(roleDefinition.permissions()).thenReturn(Set.of(permission));
        when(permission.actions()).thenReturn(allowedActions);
        when(permission.notActions()).thenReturn(notAllowedActions);
        return roleDefinition;
    }

    private RoleDefinition createRoleDefinitionWithDataActions(List<String> allowedDataActions, List<String> notAllowedDataActions) {
        RoleDefinition roleDefinition = mock(RoleDefinition.class);
        Permission permission = mock(Permission.class);
        when(roleDefinition.permissions()).thenReturn(Set.of(permission));
        when(permission.dataActions()).thenReturn(allowedDataActions);
        when(permission.notDataActions()).thenReturn(notAllowedDataActions);
        return roleDefinition;
    }

    private AzureRoleDefinitionProperties readPermissions(String roleDefinitionPath) throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(roleDefinitionPath);
        return JsonUtil.readValue(json, AzureRoleDefinitionProperties.class);
    }

    private DatabaseServer createDatabaseServer(AzureDatabaseType azureDatabaseType) {
        return DatabaseServer.builder().withParams(Map.of(AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name())).build();
    }

}