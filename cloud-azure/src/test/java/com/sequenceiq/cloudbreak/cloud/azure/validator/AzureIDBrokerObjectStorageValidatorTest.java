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
import java.util.Optional;
import java.util.Set;
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
import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
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
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
class AzureIDBrokerObjectStorageValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String SUBSCRIPTION_ID = "a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa";

    private static final String LOG_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/logger";

    private static final String ASSUMER_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/assumer";

    private static final String DATA_ACCESS_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/dataAccess";

    private static final String RANGER_IDENTITY
            = "/subscriptions/" + SUBSCRIPTION_ID + "/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/ranger";

    private static final String LOG_IDENTITY_PRINCIPAL_ID = "e2589f51-53e9-4ad0-998a-1e801618e070";

    private static final String ASSUMER_IDENTITY_PRINCIPAL_ID = "a2589f51-53e9-4ad0-998a-1e801618e071";

    private static final String DATA_ACCESS_IDENTITY_PRINCIPAL_ID = "a2589f51-53e9-4ad0-998a-1e801618e072";

    private static final String RANGER_IDENTITY_PRINCIPAL_ID = "a2589f51-53e9-4ad0-998a-1e801618e073";

    private static final String STORAGE_LOCATION_RANGER = "abfs://fs@storageaccount.dfs.core.windows.net/ranger/audit";

    private static final String LOG_LOCATION = "abfs://logs@storageaccount.dfs.core.windows.net/";

    private static final String BACKUP_LOCATION = "abfs://backup@storageaccount.dfs.core.windows.net/";

    private static final String ABFS_STORAGE_ACCOUNT_NAME = "anAccount";

    private static final String STORAGE_NAME = "rg-storage";

    private static final String SUBSCRIPTION_FULL_ID = "/subscriptions/" + SUBSCRIPTION_ID;

    private static final String STORAGE_RESOURCE_GROUP_ID = SUBSCRIPTION_FULL_ID + "/resourceGroups/" + STORAGE_NAME;

    private static final String ABFS_STORAGE_ACCOUNT_ID = STORAGE_RESOURCE_GROUP_ID
            + "/providers/Microsoft.Storage/storageAccounts/" + ABFS_STORAGE_ACCOUNT_NAME;

    private static final String MANAGEMENT_GROUP_SCOPE = "/providers/Microsoft.Management/managementGroups/";

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
    private AzureUtils azureUtils;

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

    @Spy
    private Identity user;

    @Spy
    private Identity group;

    @Spy
    private Identity ranger;

    @Spy
    private Identity dataAccess;

    static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of(true, 2),
                Arguments.of(false, 3)
        );
    }

    @BeforeEach
    void setup() {
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
        lenient().when(ranger.id()).thenReturn(RANGER_IDENTITY);
        lenient().when(ranger.principalId()).thenReturn(RANGER_IDENTITY_PRINCIPAL_ID);
        lenient().when(dataAccess.id()).thenReturn(DATA_ACCESS_IDENTITY);
        lenient().when(dataAccess.principalId()).thenReturn(DATA_ACCESS_IDENTITY_PRINCIPAL_ID);
        lenient().when(user.id()).thenReturn(USER_IDENTITY_1);
        lenient().when(group.id()).thenReturn(GROUP_IDENTITY_1);
        lenient().when(azureUtils.getSupportedAzureStorageKinds()).thenReturn(Set.of(Kind.STORAGE_V2, Kind.BLOCK_BLOB_STORAGE));
        lenient().when(azureStorage.findStorageAccountIdInVisibleSubscriptions(any(), anyString(), any())).thenReturn(Optional.of(ABFS_STORAGE_ACCOUNT_ID));
        lenient().when(entitlementService.isDatalakeBackupRestorePrechecksEnabled(any())).thenReturn(true);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        lenient().when(azureListResult.getAll()).thenReturn(List.of(assumer, logger, dataAccess, ranger, user, group));
        lenient().when(client.listIdentities()).thenReturn(azureListResult);
    }

    @Test
    void testValidateObjectStorageHnsNotEnabled() {
        when(storageAccount.isHnsEnabled()).thenReturn(Boolean.FALSE);
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, ABFS_STORAGE_ACCOUNT_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .build();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("Hierarchical namespace is mandatory for Storage Account 'storageaccount'. " +
                "Please create an ADLS Gen2 storage account with hierarchical namespace enabled. " +
                "The storage account must be in the same region as the environment.", validationResult.getErrors().get(0));

    }

    @Test
    void testValidateObjectStorageWithoutFileSystems() {
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
    void testValidateObjectStorageWhenLoggerStorageAccountScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, ABFS_STORAGE_ACCOUNT_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
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
    void testValidateObjectStorageWhenLoggerResourceGroupScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateObjectStorageWhenLoggerSubscriptionScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .build();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, RESOURCE_GROUP_NAME, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateObjectStorageNonExistingAssumerIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
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
    void testValidateObjectStorageNonExistingLoggerIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID);
        when(client.getIdentityById(LOG_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
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
    void testValidateObjectStorageNonExistingCustomMapperIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        AzureListResult<Identity> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(List.of(assumer, logger, dataAccess, ranger));
        when(client.listIdentities()).thenReturn(azureListResult);
        new RoleAssignmentBuilder(client)
                .withAssignment(DATA_ACCESS_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(RANGER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
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
    void testValidateObjectStorageMappingCaseSensitivity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        fileSystem.getCloudFileSystems().stream()
                .filter(cloudFileSystemView -> CloudIdentityType.ID_BROKER.equals(cloudFileSystemView.getCloudIdentityType()))
                .forEach(cloudFileSystemView -> cloudFileSystemView.getAccountMapping().getUserMappings().put(USER_1, USER_IDENTITY_1_CASE));
        new RoleAssignmentBuilder(client)
                .withAssignment(DATA_ACCESS_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(RANGER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateObjectStorageLogLocation() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        new RoleAssignmentBuilder(client)
                .withAssignment(DATA_ACCESS_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(RANGER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
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
    void testValidateObjectStorageNoMappedRoles() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        final String wrongAssumerIdentityPrincipalid = "489e3729-aed1-4d54-a95b-b231b70d383f";
        final String wrongLoggerIdentityPrincipalid = "61a70b9b-7331-4fa3-8717-2652fc70434e";

        new RoleAssignmentBuilder(client)
                .withAssignment(wrongAssumerIdentityPrincipalid, SUBSCRIPTION_FULL_ID)
                .withAssignment(wrongLoggerIdentityPrincipalid, STORAGE_NAME);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(STORAGE_LOCATION_RANGER)
                        .build();

        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(7, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment.", ASSUMER_IDENTITY))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment on scope", ASSUMER_IDENTITY))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment on scope", RANGER_IDENTITY))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment on scope", DATA_ACCESS_IDENTITY))));
    }

    @Test
    void testValidateObjectStorageWithNoRoleAssignments() {
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
    void testValidateObjectStorageWithNoSubscriptionScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
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
    void testValidateObjectStorageWithSingleResourceGroupAndNoResourceGroupRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
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
    void testValidateObjectStorageWithManagementGroupScopeRoleAssignment() {
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
    void testValidateObjectStorageWithNoStorageAccountScopeRoleAssignment() {
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
                ABFS_FILESYSTEM_NAME, ABFS_STORAGE_ACCOUNT_NAME, STORAGE_NAME, SUBSCRIPTION_ID));
    }

    @Test
    void testValidateObjectStorageWithNoStorageAccount() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
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

    @ParameterizedTest(name = "skipLogRoleValidationforBackup = {0}, calls = {1}")
    @MethodSource("parameterScenarios")
    void testValidateObjectStorageSkipLogRoleValidationForBackup(boolean skipLogRoleValidationforBackup, int calls) {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .withBackupLocationBase(BACKUP_LOCATION)
                        .withSkipLogRoleValidationforBackup(skipLogRoleValidationforBackup)
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        verify(adlsGen2ConfigGenerator, times(1)).generateStorageConfig(LOG_LOCATION);
        verify(adlsGen2ConfigGenerator, times(calls)).generateStorageConfig(BACKUP_LOCATION);
    }

    @Test
    void testValidateObjectStorageSkipBackupRoleValidationWhenBackupLocationIsSameAsLog() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        new RoleAssignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_NAME);
        ObjectStorageValidateRequest objectStorageValidateRequest =
                ObjectStorageValidateRequest
                        .builder()
                        .withLogsLocationBase(LOG_LOCATION)
                        .withBackupLocationBase(LOG_LOCATION)
                        .withSkipLogRoleValidationforBackup(false)
                        .build();

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, ACCOUNT_ID, fileSystem, objectStorageValidateRequest, null, resultBuilder);

        verify(adlsGen2ConfigGenerator, times(3)).generateStorageConfig(LOG_LOCATION);
    }

    private List<CloudFileSystemView> getCloudFileSystemViews(boolean addMapping) {
        CloudAdlsGen2View idBrokerCloudFileSystem = new CloudAdlsGen2View(CloudIdentityType.ID_BROKER);
        idBrokerCloudFileSystem.setManagedIdentity(ASSUMER_IDENTITY);
        idBrokerCloudFileSystem.setLocations(getStorageLocation());
        if (addMapping) {
            AccountMappingBase accountMapping = new AccountMappingBase();
            AccountMappingSubject.DATA_ACCESS_USERS.stream().forEach(service -> accountMapping.getUserMappings().put(service, DATA_ACCESS_IDENTITY));
            AccountMappingSubject.RANGER_AUDIT_USERS.stream().forEach(service -> accountMapping.getUserMappings().put(service, RANGER_IDENTITY));
            accountMapping.getGroupMappings().put(GROUP_1, GROUP_IDENTITY_1);
            accountMapping.getUserMappings().put(USER_1, USER_IDENTITY_1);
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
