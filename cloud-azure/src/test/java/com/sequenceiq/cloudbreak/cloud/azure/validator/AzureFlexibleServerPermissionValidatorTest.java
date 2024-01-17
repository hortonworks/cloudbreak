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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.AzureDatabaseType;

@ExtendWith(MockitoExtension.class)
class AzureFlexibleServerPermissionValidatorTest {

    private static final String PRINCIPAL_ID = "principal-id";

    private static final String ROLE_DEFINITION_ID = "role-definition-id-1";

    private static final String FLEXIBLE_MINIMAL_ROLE_DEF_PATH = "json/azure-flexible-minimal-role-def-test.json";

    private static final String DEFAULT_ROLE_DEF_PATH = "json/azure-role-def-test.json";

    private static final String SERVICE_ENDPOINT_MINIMAL_ROLE_DEF_PATH = "json/azure-service-endpoint-role-def-test.json";

    private static final String PRIVATE_ENDPOINT_MINIMAL_ROLE_DEF_PATH = "json/azure-private-endpoint-minimal-role-def-test.json";

    @InjectMocks
    private AzureFlexibleServerPermissionValidator underTest;

    @Mock
    private AzureFlexibleServerRoleDefinitionProvider azureFlexibleServerRoleDefinitionProvider;

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
        RoleDefinition roleDefinition = createRoleDefinition(requiredRoles.getActions());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureFlexibleServerRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(requiredRoles);

        underTest.validate(client, databaseServer);
    }

    @Test
    void testShouldNotThrowExceptionAvailableRolesContainsStar() {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(List.of("*"));
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);

        underTest.validate(client, databaseServer);

        verifyNoInteractions(azureFlexibleServerRoleDefinitionProvider);
    }

    @Test
    void testShouldThrowExceptionWithDefaultRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(DEFAULT_ROLE_DEF_PATH).getActions());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureFlexibleServerRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validate(client, databaseServer));

        assertEquals("Permission validation failed because the following actions are missing from your role definition: "
                + "[Microsoft.DBforPostgreSQL/flexibleServers/write, Microsoft.DBforPostgreSQL/flexibleServers/delete, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/start/action, Microsoft.DBforPostgreSQL/flexibleServers/read, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldThrowExceptionWithServiceEndpointMinimalRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(SERVICE_ENDPOINT_MINIMAL_ROLE_DEF_PATH).getActions());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureFlexibleServerRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validate(client, databaseServer));

        assertEquals("Permission validation failed because the following actions are missing from your role definition: "
                + "[Microsoft.DBforPostgreSQL/flexibleServers/write, Microsoft.DBforPostgreSQL/flexibleServers/delete, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/start/action, Microsoft.DBforPostgreSQL/flexibleServers/read, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldThrowExceptionWithPrivateEndpointMinimalRole() throws IOException {
        RoleAssignment roleAssignment = createRoleAssignment(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = createRoleDefinition(readPermissions(PRIVATE_ENDPOINT_MINIMAL_ROLE_DEF_PATH).getActions());
        DatabaseServer databaseServer = createDatabaseServer(FLEXIBLE_SERVER);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
        when(azureFlexibleServerRoleDefinitionProvider.loadAzureFlexibleMinimalRoleDefinition()).thenReturn(readPermissions(FLEXIBLE_MINIMAL_ROLE_DEF_PATH));

        AzureResourceException exception = assertThrows(AzureResourceException.class, () -> underTest.validate(client, databaseServer));

        assertEquals("Permission validation failed because the following actions are missing from your role definition: "
                + "[Microsoft.DBforPostgreSQL/flexibleServers/write, Microsoft.DBforPostgreSQL/flexibleServers/delete, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/start/action, Microsoft.DBforPostgreSQL/flexibleServers/read, "
                + "Microsoft.DBforPostgreSQL/flexibleServers/stop/action]", exception.getMessage());
    }

    @Test
    void testShouldNotThrowExceptionWhenTheDatabaseTypeIsSingleServer() {
        underTest.validate(client, createDatabaseServer(SINGLE_SERVER));

        verifyNoInteractions(client, azureFlexibleServerRoleDefinitionProvider);
    }

    private RoleAssignment createRoleAssignment(String roleDefinitionId) {
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.roleDefinitionId()).thenReturn(roleDefinitionId);
        return roleAssignment;
    }

    private RoleDefinition createRoleDefinition(List<String> allowedActions) {
        RoleDefinition roleDefinition = mock(RoleDefinition.class);
        Permission permission = mock(Permission.class);
        when(roleDefinition.permissions()).thenReturn(Set.of(permission));
        when(permission.actions()).thenReturn(allowedActions);
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