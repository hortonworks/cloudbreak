package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.compute.DiskEncryptionSetIdentityType;
import com.microsoft.azure.management.compute.DiskEncryptionSetType;
import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.KeyVaultAndKeyReference;
import com.microsoft.azure.management.compute.SourceVault;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AzureEncryptionResourcesTest {

    @InjectMocks
    private AzureEncryptionResources underTest;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Test
    public void testPlatformShouldReturnAzurePlatform() {
        Platform actual = underTest.platform();

        assertEquals(AzureConstants.PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAzurePlatform() {
        Variant actual = underTest.variant();

        assertEquals(AzureConstants.VARIANT, actual);
    }

    @Test
    public void testExceptionIsThrownWhenNotIsSingleResourceGroup() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withRegion(Region.region("dummyRegion"))
                .withEnvironmentName("dummyEnvName")
                .withEnvironmentId(1L)
                .withSingleResourceGroup(false)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        when(azureClientService.getClient(any(CloudCredential.class))).thenReturn(azureClient);
        assertEquals(assertThrows(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet)).getMessage(),
                "Customer Managed Key Encryption for managed Azure disks is supported only if the CDP resources " +
                        "are in the same resource group as the vault.");
    }

    @Test
    public void testExceptionIsThrownWhenVaultNameIsNotFound() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withRegion(Region.region("dummyRegion"))
                .withEnvironmentName("dummyEnvName")
                .withEnvironmentId(1L)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("wrongKeyUrl")
                .build();
        when(azureClientService.getClient(any(CloudCredential.class))).thenReturn(azureClient);
        assertEquals(assertThrows(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet)).getMessage(),
                "vaultName cannot be fetched from encryptionKeyUrl. encryptionKeyUrl should be of format - " +
                        "'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldMakeCloudCallAndThrowException() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withRegion(Region.region("dummyRegion"))
                .withEnvironmentName("dummyEnvName")
                .withEnvironmentId(1L)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureClientService.getClient(any(CloudCredential.class))).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        //Call to createDiskEncryptionSet is made and exception is thrown because of dummy parameters.
        assertEquals(assertThrows(CloudConnectorException.class, () -> underTest.createDiskEncryptionSet(requestedSet)).getMessage(),
                "Creating Disk Encryption Set resulted in failure from Azure cloud.");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldreturnExistingDiskEncryptionSet() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName").build())
                .withCloudCredential(new CloudCredential())
                .withRegion(Region.region("dummyRegion"))
                .withEnvironmentName("dummyEnvName")
                .withEnvironmentId(1L)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED))
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), any(CloudContext.class))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.getClient(any(CloudCredential.class))).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSet(any(String.class), any(String.class))).thenReturn(des);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);
        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroup(), "dummyResourceGroup");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldreturnNewlyCreatedDiskEncryptionSetIfNotAlreadyExists() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName").build())
                .withCloudCredential(new CloudCredential())
                .withRegion(Region.region("dummyRegion"))
                .withEnvironmentName("dummyEnvName")
                .withEnvironmentId(1L)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED))
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), any(CloudContext.class))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.getClient(any(CloudCredential.class))).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSet(any(String.class), any(String.class))).thenReturn(null);
        when(azureClient.createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class))).thenReturn(des);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);
        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroup(), "dummyResourceGroup");
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldThrowExceptionWhenDiskEncryptionSetNameIsNotFound() {
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName")
                        .withCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                        .withPlatform("AZURE")
                        .withVariant("AZURE")
                        .withLocation(location(Region.region("dummyRegion")))
                        .withUserName("dummyUser")
                        .withAccountId("dummyAccountId")
                        .build())
                .withCloudResources(getResource("dummyDesId"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest));
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldThrowExceptionWhenResourceGroupIsNotFound() {
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName")
                        .withCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                        .withPlatform("AZURE")
                        .withVariant("AZURE")
                        .withLocation(location(Region.region("dummyRegion")))
                        .withUserName("dummyUser")
                        .withAccountId("dummyAccountId")
                        .build())
                .withCloudResources(getResource("/subscriptions/dummySubscriptionId/resourceGroups/wrongValuesFramed/diskEncryptionSets/dummyDesName"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest));
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldDeduceValidResourceGroupAndDiskEncryptionSetName() {
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName")
                        .withCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                        .withPlatform("AZURE")
                        .withVariant("AZURE")
                        .withLocation(location(Region.region("dummyRegion")))
                        .withUserName("dummyUser")
                        .withAccountId("dummyAccountId")
                        .build())
                .withCloudResources(getResource("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                        "Microsoft.Compute/diskEncryptionSets/dummyDesId"))
                .build();
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED))
                .withLocation("dummyRegion");
        when(azureClient.getDiskEncryptionSet(any(), any())).thenReturn(des);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        underTest.deleteDiskEncryptionSet(deletionRequest);
        verify(azureClient).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().stream().findFirst().get(), deletionRequest.getCloudContext());
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldNotMakeCloudCallWhenDiskEncyrptionSetIsNotFound() {
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(new CloudCredential())
                .withCloudContext(CloudContext.Builder.builder()
                        .withId(1L)
                        .withName("envName")
                        .withCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                        .withPlatform("AZURE")
                        .withVariant("AZURE")
                        .withLocation(location(Region.region("dummyRegion")))
                        .withUserName("dummyUser")
                        .withAccountId("dummyAccountId")
                        .build())
                .withCloudResources(getResource("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                        "Microsoft.Compute/diskEncryptionSets/dummyDesId"))
                .build();
        when(azureClient.getDiskEncryptionSet(any(), any())).thenReturn(null);
        when(azureClientService.getClient(any())).thenReturn(azureClient);
        underTest.deleteDiskEncryptionSet(deletionRequest);
        verify(azureClient, never()).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().stream().findFirst().get(), deletionRequest.getCloudContext());
    }

    private List<CloudResource> getResource(String desId) {
        CloudResource desCloudResource = new CloudResource.Builder()
                .name("Des")
                .type(ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .reference(desId)
                .status(CommonStatus.CREATED)
                .build();
        return List.of(desCloudResource);
    }
}