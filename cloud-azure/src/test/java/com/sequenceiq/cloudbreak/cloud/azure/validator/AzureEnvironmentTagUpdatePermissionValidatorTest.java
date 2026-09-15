package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.resourcemanager.authorization.models.Permission;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentTagUpdatePermissionValidatorTest {

    private static final String PRINCIPAL_ID = "principal-id";

    private static final String ROLE_DEFINITION_ID = "role-definition-id-1";

    private static final String CREDENTIAL_NAME = "cred-1";

    private static final String TAG_UPDATE_ROLE_DEF_PATH = "definitions/azure-tag-update-minimal-role-def.json";

    private static final String DISK_READ = "Microsoft.Compute/disks/read";

    private static final String DISK_WRITE = "Microsoft.Compute/disks/write";

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureRoleDefinitionProvider azureRoleDefinitionProvider;

    @InjectMocks
    private AzurePermissionValidator azurePermissionValidator;

    @Mock
    private AzureClient client;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private AzureEnvironmentTagUpdatePermissionValidator underTest;

    @BeforeEach
    void before() throws IOException {
        ReflectionTestUtils.setField(underTest, "azurePermissionValidator", azurePermissionValidator);
        lenient().when(cloudCredential.getName()).thenReturn(CREDENTIAL_NAME);
        lenient().when(azureClientService.getClient(cloudCredential)).thenReturn(client);
        lenient().when(client.getServicePrincipalId()).thenReturn(PRINCIPAL_ID);
        lenient().when(azureRoleDefinitionProvider.loadAzureTagUpdateMinimalRoleDefinition()).thenReturn(readPermissions(TAG_UPDATE_ROLE_DEF_PATH));
    }

    @Test
    void supportedPlatformIsAzure() {
        assertEquals("AZURE", underTest.supportedPlatform());
    }

    @Test
    void tagUpdateRoleDefinitionIsPresentOnClasspath() {
        assertNotNull(getClass().getResource("/" + TAG_UPDATE_ROLE_DEF_PATH),
                "Azure tag-update minimal role definition JSON must exist on the classpath");
    }

    @Test
    void validatePassesWhenTheRoleCoversEveryRequiredAction() throws Exception {
        stubRole(requiredActions(), List.of());

        underTest.validate(cloudCredential);
    }

    @Test
    void validatePassesWhenTheRoleAllowsEveryActionWithAWildcard() throws Exception {
        stubRole(List.of("*"), List.of());

        underTest.validate(cloudCredential);
    }

    @Test
    void validateThrowsWhenAWritePermissionIsMissing() throws Exception {
        stubRole(requiredActionsExcept(DISK_WRITE), List.of());

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of(DISK_WRITE), thrown.getFailedActions());
        assertTrue(thrown.getMessage().contains(CREDENTIAL_NAME), "the message must name the credential");
        assertTrue(thrown.getMessage().contains(DISK_WRITE), "the message must name the missing action");
    }

    @Test
    void validateThrowsWhenOnlyAReadPermissionIsMissing() throws Exception {
        stubRole(requiredActionsExcept(DISK_READ), List.of());

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of(DISK_READ), thrown.getFailedActions());
        assertTrue(thrown.getMessage().contains(DISK_READ), "the strategies read the existing tags before writing the merged map back");
    }

    @Test
    void validateRequiresAReadActionForEveryWriteAction() {
        List<String> required = requiredActions();

        List<String> writesWithoutARead = required.stream()
                .filter(action -> action.endsWith("/write"))
                .map(action -> action.substring(0, action.length() - "/write".length()) + "/read")
                .filter(read -> !required.contains(read))
                .toList();

        assertTrue(writesWithoutARead.isEmpty(),
                "every write action needs its matching read action, missing: " + writesWithoutARead);
    }

    @Test
    void validateThrowsWhenARequiredActionIsExplicitlyDenied() throws Exception {
        stubRole(requiredActions(), List.of(DISK_WRITE));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of(DISK_WRITE), thrown.getFailedActions());
        assertTrue(thrown.getMessage().contains("explicitly denied"), "an explicit deny must be reported as such");
    }

    @Test
    void validateReportsAMissingAndADeniedActionOnlyOnce() throws Exception {
        stubRole(requiredActionsExcept(DISK_WRITE), List.of(DISK_WRITE));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of(DISK_WRITE), thrown.getFailedActions());
    }

    @Test
    void validateWrapsRoleListingFailureAsAValidationErrorWithoutClaimingAnActionIsMissing() {
        RuntimeException cause = new RuntimeException("Authorization failed for listing role assignments");
        when(client.getServicePrincipalId()).thenThrow(cause);

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertTrue(thrown.getFailedActions().isEmpty(), "a failed role lookup must not be reported as a missing action");
        assertTrue(thrown.getMessage().contains(CREDENTIAL_NAME));
        assertTrue(thrown.getMessage().contains("Authorization failed for listing role assignments"));
    }

    @Test
    void validateWrapsClientCreationFailureAsAValidationError() {
        IllegalStateException cause = new IllegalStateException("could not build the Azure client");
        when(azureClientService.getClient(cloudCredential)).thenThrow(cause);

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertSame(cause, thrown.getCause());
        assertTrue(thrown.getFailedActions().isEmpty());
    }

    private void stubRole(List<String> allowedActions, List<String> notAllowedActions) {
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.roleDefinitionId()).thenReturn(ROLE_DEFINITION_ID);
        RoleDefinition roleDefinition = mock(RoleDefinition.class);
        Permission permission = mock(Permission.class);
        when(roleDefinition.permissions()).thenReturn(Set.of(permission));
        when(permission.actions()).thenReturn(allowedActions);
        when(permission.notActions()).thenReturn(notAllowedActions);
        when(client.listRoleAssignmentsByServicePrincipal(PRINCIPAL_ID)).thenReturn(List.of(roleAssignment));
        when(client.getRoleDefinitionById(ROLE_DEFINITION_ID)).thenReturn(roleDefinition);
    }

    private List<String> requiredActions() {
        try {
            return readPermissions(TAG_UPDATE_ROLE_DEF_PATH).getActions();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> requiredActionsExcept(String action) {
        return requiredActions().stream()
                .filter(required -> !required.equals(action))
                .toList();
    }

    private AzureRoleDefinitionProperties readPermissions(String roleDefinitionPath) throws IOException {
        String json = FileReaderUtils.readFileFromClasspath(roleDefinitionPath);
        return JsonUtil.readValue(json, AzureRoleDefinitionProperties.class);
    }
}
