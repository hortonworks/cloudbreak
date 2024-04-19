package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.authorization.fluent.models.RoleAssignmentInner;
import com.azure.resourcemanager.msi.models.Identity;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.azure.resourcemanager.resources.models.Subscription;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureClientCachedOperations;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class AzureIDBrokerObjectStorageValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String SUBSCRIPTION_ID = "a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa";

    private static final String LOG_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/logger";

    private static final String ASSUMER_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/assumer";

    private static final String LOG_IDENTITY_PRINCIPAL_ID
            = "e2589f51-53e9-4ad0-998a-1e801618e070";

    private static final String ASSUMER_IDENTITY_PRINCIPAL_ID
            = "a2589f51-53e9-4ad0-998a-1e801618e071";

    private static final String STORAGE_LOCATION_RANGER = "abfs://fs@storageaccount.dfs.core.windows.net/ranger/audit";

    private static final String LOG_LOCATION = "abfs://logs@storageaccount.dfs.core.windows.net/";

    private static final String BACKUP_LOCATION = "abfs://backup@storageaccount.dfs.core.windows.net/";

    private static final String ABFS_STORAGE_ACCOUNT_NAME = "anAccount";

    private static final String SUBSCRIPTION_FULL_ID = "/subscriptions/" + SUBSCRIPTION_ID;

    private static final String STORAGE_RESOURCE_GROUP_NAME = "rg-storage";

    private static final String MANAGEMENT_GROUP_SCOPE = "/providers/Microsoft.Management/managementGroups/";

    private static final String STORAGE_RESOURCE_GROUP_ID = SUBSCRIPTION_FULL_ID
            + "/resourceGroups/" + STORAGE_RESOURCE_GROUP_NAME;

    private static final String ABFS_STORAGE_ACCOUNT_ID = STORAGE_RESOURCE_GROUP_ID
            + "/providers/Microsoft.Storage/storageAccounts/" + ABFS_STORAGE_ACCOUNT_NAME;

    private static final String ABFS_FILESYSTEM_NAME = "aFileSystem";

    private static final String USER_1 = "user1";

    private static final String GROUP_1 = "group1";

    private static final String USER_IDENTITY_1 =
            "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/user";

    private static final String USER_IDENTITY_1_CASE =
            "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourceGroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/user";

    private static final String GROUP_IDENTITY_1 =
            "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/group";

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String RESOURCE_GROUP_ID = "resourceGroupId";

    @Mock
    private AzureClient client;

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private ResourceGroup resourceGroup;

    @Mock
    private StorageAccount storageAccount;

    @Mock
    private EntitlementService entitlementService;

    @Spy
    private AzureClientCachedOperations azureClientCachedOperations;

    @InjectMocks
    private AzureIDBrokerObjectStorageValidator underTest;

    @Spy
    private Identity logger;

    @Spy
    private Identity assumer;

    @BeforeEach
    public void setup() {
        lenient().when(client.getIdentityById(LOG_IDENTITY)).thenReturn(logger);
        lenient().when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(assumer);
        lenient().when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        lenient().when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        lenient().when(client.getResourceGroup(RESOURCE_GROUP_NAME)).thenReturn(resourceGroup);
        lenient().when(client.getStorageAccount(any(), any())).thenReturn(Optional.of(storageAccount));
        lenient().when(storageAccount.isHnsEnabled()).thenReturn(Boolean.TRUE);
        lenient().when(resourceGroup.id()).thenReturn(RESOURCE_GROUP_ID);
        AdlsGen2Config adlsGen2Config = new AdlsGen2Config("abfs://", ABFS_FILESYSTEM_NAME, ABFS_STORAGE_ACCOUNT_NAME, false);
        lenient().when(adlsGen2ConfigGenerator.generateStorageConfig(anyString())).thenReturn(adlsGen2Config);
        lenient().when(logger.id()).thenReturn(LOG_IDENTITY);
        lenient().when(logger.principalId()).thenReturn(LOG_IDENTITY_PRINCIPAL_ID);
        lenient().when(assumer.id()).thenReturn(ASSUMER_IDENTITY);
        lenient().when(assumer.principalId()).thenReturn(ASSUMER_IDENTITY_PRINCIPAL_ID);
        lenient().when(azureStorage.findStorageAccountIdInVisibleSubscriptions(any(), anyString(), any())).thenReturn(Optional.of(ABFS_STORAGE_ACCOUNT_ID));
        lenient().when(entitlementService.isDatalakeBackupRestorePrechecksEnabled(any())).thenReturn(true);
    }

    @Test
    public void testValidateObjectStorageWithoutFileSystems() {
        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    public void testValidateObjectStorageWhenLoggerStorageAccountScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, ABFS_STORAGE_ACCOUNT_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    private SpiFileSystem setupSpiFileSystem(boolean addMapping) {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(addMapping);
        return new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
    }

    @Test
    public void testValidateObjectStorageWhenLoggerResourceGroupScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageWhenLoggerSubscriptionScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, RESOURCE_GROUP_NAME, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageNonExistingAssumerIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Assumer Identity with id %s does not exist in the given Azure subscription. " +
                "Please check if you've used the correct Identity when setting up Data Access.", ASSUMER_IDENTITY));
    }

    @Test
    public void testValidateObjectStorageNonExistingLoggerIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID);
        when(client.getIdentityById(LOG_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Log Identity with id %s does not exist in the given Azure subscription. " +
                "Please check if you've used the correct Identity when setting up Logs-Storage and Audit.", LOG_IDENTITY));
    }

    @Test
    public void testValidateObjectStorageNonExistingMapperIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        List<Identity> identityPagedList = new ArrayList<>();
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(identityPagedList);
        when(client.listIdentities()).thenReturn(azureListResult);

        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(STORAGE_LOCATION_RANGER)
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, RESOURCE_GROUP_NAME, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s does not exist in the given Azure subscription.", USER_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s does not exist in the given Azure subscription.", GROUP_IDENTITY_1))));
    }

    @Test
    public void testValidateObjectStorageMappingCaseSensitivityCB6600() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        List<Identity> identityPagedList = new ArrayList<>();
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(identityPagedList);
        when(client.listIdentities()).thenReturn(azureListResult);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageLogLocation() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        List<Identity> identityPagedList = new ArrayList<>();
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(identityPagedList);
        when(client.listIdentities()).thenReturn(azureListResult);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .withBackupLocationBase(BACKUP_LOCATION)
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageNoMappedRoles() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        List<Identity> identityPagedList = new ArrayList<>();
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(identityPagedList);
        when(client.listIdentities()).thenReturn(azureListResult);

        final String wrongAssumerIdentityPrincipalid = "489e3729-aed1-4d54-a95b-b231b70d383f";
        final String wrongLoggerIdentityPrincipalid = "61a70b9b-7331-4fa3-8717-2652fc70434e";

        new RoleAssignmentBuilder(client)
                .withAssignment(wrongAssumerIdentityPrincipalid, SUBSCRIPTION_FULL_ID)
                .withAssignment(wrongLoggerIdentityPrincipalid, STORAGE_RESOURCE_GROUP_NAME);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(STORAGE_LOCATION_RANGER)
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(6, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment.", USER_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment on scope", GROUP_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment on scope", USER_IDENTITY_1))));

    }

    @Test
    public void testValidateObjectStorageWithNoRoleAssignments() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(validationResult.getErrors().get(1), "There are no role assignments for the given Azure subscription. " +
                "Please check if you've used the correct Identity when setting up Data Access.");
        assertEquals(validationResult.getErrors().get(0), String.format("Identity with id %s has no role assignment. " +
                "Please check if you've used the correct Identity when setting up Data Access.", ASSUMER_IDENTITY));
    }

    @Test
    public void testValidateObjectStorageWithNoSubscriptionScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(validationResult.getErrors().get(1), String.format("Identity with id %s has no role assignment. " +
                "Please check if you've used the correct Identity when setting up Data Access.", ASSUMER_IDENTITY));
        assertEquals(validationResult.getErrors().get(0), String.format("Identity with id %s has no role assignment on scope(s) " +
                        "[/subscriptions/%s, %s]. Please check if you've used the correct Identity when setting up Data Access.",
                ASSUMER_IDENTITY, SUBSCRIPTION_ID, MANAGEMENT_GROUP_SCOPE));
    }

    @Test
    public void testValidateObjectStorageWithSingleResourceGroupAndNoResourceGroupRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, RESOURCE_GROUP_NAME, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        verify(client, times(0)).listRoleAssignments();
        verify(client, times(1)).listRoleAssignmentsByScopeInner(RESOURCE_GROUP_ID);
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        assertEquals(validationResult.getErrors().get(1), String.format("Identity with id %s has no role assignment. " +
                "Please check if you've used the correct Identity when setting up Data Access.", ASSUMER_IDENTITY));
        assertEquals(validationResult.getErrors().get(0), String.format("Identity with id %s has no role assignment on scope(s) " +
                        "[/subscriptions/%s, %s, %s]. Please check if you've used the correct Identity when setting up Data Access.",
                ASSUMER_IDENTITY, SUBSCRIPTION_ID, RESOURCE_GROUP_ID, MANAGEMENT_GROUP_SCOPE));
    }

    @Test
    public void testValidateObjectStorageWithManagementGroupScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, MANAGEMENT_GROUP_SCOPE);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, RESOURCE_GROUP_NAME, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        verify(client, times(0)).listRoleAssignments();
        verify(client, times(1)).listRoleAssignmentsByScopeInner(RESOURCE_GROUP_ID);
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageWithNoStorageAccountScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(STORAGE_LOCATION_RANGER)
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s has no role assignment on scope(s) [%s, %s, %s, %s]. " +
                        "Please check if you've used the correct Identity when setting up Logs-Storage and Audit.", LOG_IDENTITY,
                ABFS_FILESYSTEM_NAME, ABFS_STORAGE_ACCOUNT_NAME, STORAGE_RESOURCE_GROUP_NAME, SUBSCRIPTION_ID));
    }

    @Test
    public void testValidateObjectStorageWithNoStorageAccount() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        when(azureStorage.findStorageAccountIdInVisibleSubscriptions(any(), anyString(), any())).thenReturn(Optional.empty());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase("")
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Storage account with name %s not found in the given Azure subscription. " +
                "Please check if you've used the correct Storage Location when setting up Data Access.", ABFS_STORAGE_ACCOUNT_NAME));
    }

    static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of(true, 2),
                Arguments.of(false, 3)
        );
    }

    @ParameterizedTest(name = "skipLogRoleValidationforBackup = {0}, calls = {1}")
    @MethodSource("parameterScenarios")
    public void testValidateObjectStorageSkipLogRoleValidationforBackup(boolean skipLogRoleValidationforBackup, int calls) {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        List<Identity> identityPagedList = new ArrayList<>();
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(identityPagedList);
        when(client.listIdentities()).thenReturn(azureListResult);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .withBackupLocationBase(BACKUP_LOCATION)
                        .withSkipLogRoleValidationforBackup(skipLogRoleValidationforBackup)
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        verify(adlsGen2ConfigGenerator, times(calls)).generateStorageConfig(BACKUP_LOCATION);
    }

    private List<CloudFileSystemView> getCloudFileSystemViews(boolean addMapping) {
        CloudAdlsGen2View idBrokerCloudFileSystem = new CloudAdlsGen2View(CloudIdentityType.ID_BROKER);
        idBrokerCloudFileSystem.setManagedIdentity(ASSUMER_IDENTITY);
        idBrokerCloudFileSystem.setLocations(getStorageLocation());
        if (addMapping) {
            AccountMappingBase accountMapping = new AccountMappingBase();
            accountMapping.setGroupMappings(Map.ofEntries(Map.entry(GROUP_1, GROUP_IDENTITY_1)));
            accountMapping.setUserMappings(Map.ofEntries(Map.entry(USER_1, USER_IDENTITY_1_CASE)));
            idBrokerCloudFileSystem.setAccountMapping(accountMapping);
        }
        CloudAdlsGen2View loggerCloudFileSystem = new CloudAdlsGen2View(CloudIdentityType.LOG);
        loggerCloudFileSystem.setManagedIdentity(LOG_IDENTITY);
        loggerCloudFileSystem.setLocations(getStorageLocation());
        return List.of(idBrokerCloudFileSystem, loggerCloudFileSystem);
    }

    private List<StorageLocationBase> getStorageLocation() {
        CloudStorageCdpService eStorageLocationType = CloudStorageCdpService.RANGER_AUDIT;
        StorageLocationBase storageLocationBase = new StorageLocationBase();
        storageLocationBase.setType(eStorageLocationType);
        storageLocationBase.setValue(STORAGE_LOCATION_RANGER);
        return List.of(storageLocationBase);
    }

    private static class RoleAssignmentBuilder {

        private final List<RoleAssignmentInner> roleAssignments;

        RoleAssignmentBuilder(AzureClient client) {
            roleAssignments = new ArrayList<>();
            AzureListResult<RoleAssignmentInner> azureListResult = mock(AzureListResult.class);
            lenient().when(azureListResult.getAll()).thenReturn(roleAssignments);
            lenient().when(client.listRoleAssignments()).thenReturn(azureListResult);
            lenient().when(client.listRoleAssignmentsByScopeInner(any())).thenReturn(roleAssignments);
        }

        RoleAssignmentBuilder withAssignment(String principalId, String scope) {
            RoleAssignmentInner roleAssignmentInner = spy(new RoleAssignmentInner()
                    .withPrincipalId(principalId));
            lenient().when(roleAssignmentInner.scope()).thenReturn(scope);
            roleAssignments.add(roleAssignmentInner);
            return this;
        }
    }
}
