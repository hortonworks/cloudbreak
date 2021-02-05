package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.graphrbac.implementation.RoleAssignmentInner;
import com.microsoft.azure.management.msi.Identity;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@RunWith(MockitoJUnitRunner.class)
public class AzureIDBrokerObjectStorageValidatorTest {

    private static final CloudStorageCdpService SERVICE_1 = CloudStorageCdpService.ZEPPELIN_NOTEBOOK;

    private static final String PATH_1 = "path1";

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

    private static final String ABFS_STORAGE_ACCOUNT_NAME = "anAccount";

    private static final String SUBSCRIPTION_FULL_ID = "/subscriptions/" + SUBSCRIPTION_ID;

    private static final String STORAGE_RESOURCE_GROUP_NAME = "rg-storage";

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

    @Mock
    private AzureClient client;

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

    @Mock
    private AzureStorage azureStorage;

    @InjectMocks
    private AzureIDBrokerObjectStorageValidator underTest;

    @Spy
    private Identity logger;

    @Spy
    private Identity assumer;

    @Before
    public void setup() {
        when(client.getIdentityById(LOG_IDENTITY)).thenReturn(logger);
        when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(assumer);
        when(client.getCurrentSubscription()).thenReturn(mock(Subscription.class));
        when(client.getCurrentSubscription().subscriptionId()).thenReturn(SUBSCRIPTION_ID);
        AdlsGen2Config adlsGen2Config = new AdlsGen2Config("abfs://", ABFS_FILESYSTEM_NAME, ABFS_STORAGE_ACCOUNT_NAME, false);
        when(adlsGen2ConfigGenerator.generateStorageConfig(anyString())).thenReturn(adlsGen2Config);
        when(logger.id()).thenReturn(LOG_IDENTITY);
        when(logger.principalId()).thenReturn(LOG_IDENTITY_PRINCIPAL_ID);
        when(assumer.id()).thenReturn(ASSUMER_IDENTITY);
        when(assumer.principalId()).thenReturn(ASSUMER_IDENTITY_PRINCIPAL_ID);
        when(azureStorage.findStorageAccountIdInVisibleSubscriptions(any(), anyString())).thenReturn(Optional.of(ABFS_STORAGE_ACCOUNT_ID));
    }

    @Test
    public void testValidateObjectStorageWithoutFileSystems() {
        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    public void testValidateObjectStorageWhenLoggerStorageAccountScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, ABFS_STORAGE_ACCOUNT_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

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
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageWhenLoggerSubscriptionScopeThenNoError() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageNonExistingAssumerIdentity() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

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
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID);
        when(client.getIdentityById(LOG_IDENTITY)).thenReturn(null);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

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
        PagedList<Identity> identityPagedList = Mockito.spy(PagedList.class);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        when(client.listIdentities()).thenReturn(identityPagedList);

        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, STORAGE_LOCATION_RANGER, resultBuilder);

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
        PagedList<Identity> identityPagedList = Mockito.spy(PagedList.class);
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        when(client.listIdentities()).thenReturn(identityPagedList);
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);
        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageNoMappedRoles() {
        SpiFileSystem fileSystem = setupSpiFileSystem(true);
        PagedList<Identity> identityPagedList = Mockito.spy(PagedList.class);
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        when(client.listIdentities()).thenReturn(identityPagedList);

        final String wrongAssumerIdentityPrincipalid = "489e3729-aed1-4d54-a95b-b231b70d383f";
        final String wrongLoggerIdentityPrincipalid = "61a70b9b-7331-4fa3-8717-2652fc70434e";

        new RoleASsignmentBuilder(client)
                .withAssignment(wrongAssumerIdentityPrincipalid, SUBSCRIPTION_FULL_ID)
                .withAssignment(wrongLoggerIdentityPrincipalid, STORAGE_RESOURCE_GROUP_NAME);

        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, STORAGE_LOCATION_RANGER, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(6, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment.", USER_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.contains(String.format("Identity with id %s has no role assignment.", GROUP_IDENTITY_1))));

    }

    @Test
    public void testValidateObjectStorageWithNoRoleAssignments() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, "There are no role assignments for the given Azure subscription. " +
                "Please check if you've used the correct Identity when setting up Data Access.");
    }

    @Test
    public void testValidateObjectStorageWithNoSubscriptionScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s has no role assignment on scope(s) [/subscriptions/%s]. " +
                "Please check if you've used the correct Identity when setting up Data Access.", ASSUMER_IDENTITY, SUBSCRIPTION_ID));
    }

    @Test
    public void testValidateObjectStorageWithNoStorageAccountScopeRoleAssignment() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, STORAGE_LOCATION_RANGER, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s has no role assignment on scope(s) [%s, %s, %s]. " +
                        "Please check if you've used the correct Identity when setting up Logs-Storage and Audit.", LOG_IDENTITY,
                ABFS_STORAGE_ACCOUNT_NAME, STORAGE_RESOURCE_GROUP_NAME, SUBSCRIPTION_ID));
    }

    @Test
    public void testValidateObjectStorageWithNoStorageAccount() {
        SpiFileSystem fileSystem = setupSpiFileSystem(false);
        new RoleASsignmentBuilder(client)
                .withAssignment(ASSUMER_IDENTITY_PRINCIPAL_ID, SUBSCRIPTION_FULL_ID)
                .withAssignment(LOG_IDENTITY_PRINCIPAL_ID, STORAGE_RESOURCE_GROUP_NAME);
        when(azureStorage.findStorageAccountIdInVisibleSubscriptions(any(), anyString())).thenReturn(Optional.empty());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateObjectStorage(client, fileSystem, "", resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Storage account with name %s not found in the given Azure subscription. " +
                "Please check if you've used the correct Storage Location when setting up Data Access.", ABFS_STORAGE_ACCOUNT_NAME));
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

    private static class RoleASsignmentBuilder {

        private final PagedList<RoleAssignmentInner> roleAssignmentsPagedList;

        RoleASsignmentBuilder(AzureClient client) {
            roleAssignmentsPagedList = Mockito.spy(PagedList.class);
            when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);
            when(client.listRoleAssignmentsByScopeInner(any())).thenReturn(roleAssignmentsPagedList);
        }

        RoleASsignmentBuilder withAssignment(String principalId, String scope) {
            RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                    .withPrincipalId(principalId)
                    .withScope(scope);
            roleAssignmentsPagedList.add(roleAssignmentInner);
            return this;
        }
    }
}
