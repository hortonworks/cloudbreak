package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2Config;
import com.sequenceiq.cloudbreak.telemetry.fluent.cloud.AdlsGen2ConfigGenerator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;

@RunWith(MockitoJUnitRunner.class)
public class AzureIDBrokerObjectStorageValidatorTest {

    private static final CloudStorageCdpService SERVICE_1 = CloudStorageCdpService.ZEPPELIN_NOTEBOOK;

    private static final String PATH_1 = "path1";

    private static final String LOG_IDENTITY
            = "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/logger";

    private static final String ASSUMER_IDENTITY
            = "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/assumer";

    private static final String LOG_IDENTITY_PRINCIPAL_ID
            = "e2589f51-53e9-4ad0-998a-1e801618e070";

    private static final String ASSUMER_IDENTITY_PRINCIPAL_ID
            = "a2589f51-53e9-4ad0-998a-1e801618e071";

    private static final String STORAGE_LOCATION_RANGER = "abfs://fs@storageaccount.dfs.core.windows.net/ranger/audit";

    private static final String SUBSCRIPTION_ID = "valid-subscription-id";

    private static final String ABFS_STORAGE_ACCOUNT_NAME = "anAccount";

    private static final String ABFS_FILESYSTEM_NAME = "aFileSystem";

    private static final String USER_1 = "user1";

    private static final String GROUP_1 = "group1";

    private static final String USER_IDENTITY_1 =
            "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/user";

    private static final String GROUP_IDENTITY_1 =
            "/subscriptions/a9d4456e-349f-44f6-bc73-aaaaaaaaaaaa/resourcegroups/msi/providers/Microsoft.ManagedIdentity/userAssignedIdentities/group";

    @Mock
    private AzureClient client;

    @Mock
    private AdlsGen2ConfigGenerator adlsGen2ConfigGenerator;

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
    }

    @Test
    public void testValidateObjectStorageWithoutFileSystems() {
        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, null);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    public void testValidateObjectStorageNoError() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(ASSUMER_IDENTITY_PRINCIPAL_ID)
                .withScope("/subscriptions/" + SUBSCRIPTION_ID);
        roleAssignmentsPagedList.add(roleAssignmentInner);
        roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(LOG_IDENTITY_PRINCIPAL_ID)
                .withScope(ABFS_STORAGE_ACCOUNT_NAME);
        roleAssignmentsPagedList.add(roleAssignmentInner);

        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);
        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertFalse(validationResult.hasError());
    }

    @Test
    public void testValidateObjectStorageNonExistingAssumerIdentity() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(LOG_IDENTITY_PRINCIPAL_ID)
                .withScope(ABFS_STORAGE_ACCOUNT_NAME);
        roleAssignmentsPagedList.add(roleAssignmentInner);

        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);
        when(client.getIdentityById(ASSUMER_IDENTITY)).thenReturn(null);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s does not exist in the given Azure subscription.", ASSUMER_IDENTITY));
    }

    @Test
    public void testValidateObjectStorageNonExistingLoggerIdentity() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(ASSUMER_IDENTITY_PRINCIPAL_ID)
                .withScope("/subscriptions/" + SUBSCRIPTION_ID);
        roleAssignmentsPagedList.add(roleAssignmentInner);

        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);
        when(client.getIdentityById(LOG_IDENTITY)).thenReturn(null);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s does not exist in the given Azure subscription.", LOG_IDENTITY));
    }

    @Test
    public void testValidateObjectStorageNonExistingMapperIdentity() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(true);

        PagedList<Identity> identityPagedList = Mockito.spy(PagedList.class);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        when(client.listIdentities()).thenReturn(identityPagedList);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(ASSUMER_IDENTITY_PRINCIPAL_ID)
                .withScope("/subscriptions/" + SUBSCRIPTION_ID);
        roleAssignmentsPagedList.add(roleAssignmentInner);
        roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(LOG_IDENTITY_PRINCIPAL_ID)
                .withScope(ABFS_STORAGE_ACCOUNT_NAME);
        roleAssignmentsPagedList.add(roleAssignmentInner);

        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.equals(String.format("Identity with id %s does not exist in the given Azure subscription.", USER_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.equals(String.format("Identity with id %s does not exist in the given Azure subscription.", GROUP_IDENTITY_1))));
    }

    @Test
    public void testValidateObjectStorageNoMappedRoles() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(true);

        PagedList<Identity> identityPagedList = Mockito.spy(PagedList.class);
        when(assumer.id()).thenReturn(USER_IDENTITY_1);
        when(logger.id()).thenReturn(GROUP_IDENTITY_1);
        identityPagedList.add(assumer);
        identityPagedList.add(logger);
        when(client.listIdentities()).thenReturn(identityPagedList);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(ASSUMER_IDENTITY_PRINCIPAL_ID)
                .withScope("/subscriptions/" + SUBSCRIPTION_ID);
        roleAssignmentsPagedList.add(roleAssignmentInner);
        roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(LOG_IDENTITY_PRINCIPAL_ID)
                .withScope(ABFS_STORAGE_ACCOUNT_NAME);
        roleAssignmentsPagedList.add(roleAssignmentInner);

        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(2, validationResult.getErrors().size());
        List<String> actual = validationResult.getErrors();
        assertTrue(actual.stream().anyMatch(item ->
                item.equals(String.format("Identity with id %s has no role assignment.", USER_IDENTITY_1))));
        assertTrue(actual.stream().anyMatch(item ->
                item.equals(String.format("Identity with id %s has no role assignment.", GROUP_IDENTITY_1))));

    }

    @Test
    public void testValidateObjectStorageWithNoRoleAssignments() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);

        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, "There are no role assignments for the given Azure subscription.");
    }

    @Test
    public void testValidateObjectStorageWithNoSubscriptionScopeRoleAssignment() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);
        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(LOG_IDENTITY_PRINCIPAL_ID)
                .withScope(ABFS_STORAGE_ACCOUNT_NAME);
        roleAssignmentsPagedList.add(roleAssignmentInner);
        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s has no role assignment on scope /subscriptions/%s.", ASSUMER_IDENTITY, SUBSCRIPTION_ID));
    }

    @Test
    public void testValidateObjectStorageWithNoStorageAccountScopeRoleAssignment() {
        List<CloudFileSystemView> cloudFileSystems = getCloudFileSystemViews(false);
        PagedList<RoleAssignmentInner> roleAssignmentsPagedList = Mockito.spy(PagedList.class);
        RoleAssignmentInner roleAssignmentInner = new RoleAssignmentInner()
                .withPrincipalId(ASSUMER_IDENTITY_PRINCIPAL_ID)
                .withScope("/subscriptions/" + SUBSCRIPTION_ID);
        roleAssignmentsPagedList.add(roleAssignmentInner);
        when(client.listRoleAssignments()).thenReturn(roleAssignmentsPagedList);

        SpiFileSystem fileSystem = new SpiFileSystem("test", FileSystemType.ADLS_GEN_2, cloudFileSystems);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        underTest.validateObjectStorage(client, fileSystem, resultBuilder);

        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals(1, validationResult.getErrors().size());
        String actual = validationResult.getErrors().get(0);
        assertEquals(actual, String.format("Identity with id %s has no role assignment on scope %s.", LOG_IDENTITY, ABFS_STORAGE_ACCOUNT_NAME));
    }

    private List<CloudFileSystemView> getCloudFileSystemViews(boolean addMapping) {
        CloudAdlsGen2View idBrokerCloudFileSystem = new CloudAdlsGen2View(CloudIdentityType.ID_BROKER);
        idBrokerCloudFileSystem.setManagedIdentity(ASSUMER_IDENTITY);
        idBrokerCloudFileSystem.setLocations(getStorageLocation());
        if (addMapping) {
            AccountMappingBase accountMapping = new AccountMappingBase();
            accountMapping.setGroupMappings(Map.ofEntries(Map.entry(GROUP_1, GROUP_IDENTITY_1)));
            accountMapping.setUserMappings(Map.ofEntries(Map.entry(USER_1, USER_IDENTITY_1)));
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
}
