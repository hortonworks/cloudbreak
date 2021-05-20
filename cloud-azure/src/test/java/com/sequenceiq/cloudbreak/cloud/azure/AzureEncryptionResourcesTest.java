package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK_ENCRYPTION_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.management.compute.DiskEncryptionSetIdentityType;
import com.microsoft.azure.management.compute.DiskEncryptionSetType;
import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.KeyVaultAndKeyReference;
import com.microsoft.azure.management.compute.SourceVault;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.microsoft.azure.management.resources.Subscription;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClientService;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationPoller;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.service.Retry;

@ExtendWith(MockitoExtension.class)
public class AzureEncryptionResourcesTest {

    private static final String DES_PRINCIPAL_ID = "desPrincipalId";

    private static final String DES_RESOURCE_ID =
            "/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/Microsoft.Compute/diskEncryptionSets/dummyEnvName-DES-uniqueId";

    @InjectMocks
    private AzureEncryptionResources underTest;

    @Mock
    private AzureClientService azureClientService;

    @Mock
    private AzureClient azureClient;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private DiskEncryptionSetCreationPoller diskEncryptionSetCreationPoller;

    @Mock
    private Retry retryService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AuthenticatedContext authenticatedContext;

    private CloudContext cloudContext;

    @BeforeEach
    void setUp() {
        cloudContext = createCloudContext();
    }

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

    private CloudContext createCloudContext() {
        return CloudContext.Builder.builder()
                .withId(1L)
                .withName("envName")
                .withCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                .withPlatform("AZURE")
                .withVariant("AZURE")
                .withLocation(location(region("dummyRegion")))
                .withUserName("dummyUser")
                .withAccountId("dummyAccountId")
                .build();
    }

    private void initExceptionConversion() {
        when(azureUtils.convertToCloudConnectorException(any(Exception.class), any(String.class)))
                .thenAnswer(invocation -> new CloudConnectorException(invocation.getArgument(0, Exception.class)));
    }

    private void initActionFailedExceptionConversion() {
        when(azureUtils.convertToActionFailedExceptionCausedByCloudConnectorException(any(Exception.class), any(String.class)))
                .thenAnswer(invocation -> new Retry.ActionFailedException(new CloudConnectorException(invocation.getArgument(0, Exception.class))));
    }

    @Test
    public void testExceptionIsThrownWhenNotIsSingleResourceGroup() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withSingleResourceGroup(false)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet),
                "Customer Managed Key Encryption for managed Azure disks is supported only if the CDP resources " +
                        "are in the same resource group as the vault.");
    }

    private <T extends Throwable> void verifyException(Class<T> expectedType, Executable executable, String messageExpected) {
        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, executable);

        assertThat(verifyAndGetCause(expectedType, cloudConnectorException)).hasMessage(messageExpected);
    }

    private <T extends Throwable> T verifyAndGetCause(Class<T> expectedType, Throwable e) {
        assertThat(e).hasCauseInstanceOf(expectedType);
        return expectedType.cast(e.getCause());
    }

    @Test
    public void testExceptionIsThrownWhenVaultNameIsNotFound() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("wrongKeyUrl")
                .build();
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet),
                "vaultName cannot be fetched from encryptionKeyUrl. encryptionKeyUrl should be of format - " +
                        "'https://<vaultName>.vault.azure.net/keys/<keyName>/<keyVersion>'");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldMakeCloudCallAndThrowException() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId("envName-DES-", "uniqueId")).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        initExceptionConversion();
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);

        when(azureClient.getDiskEncryptionSetByName("dummyResourceGroup", "dummyEnvName-DES-uniqueId"))
                .thenThrow(new UnsupportedOperationException("Serious problem"));

        verifyException(UnsupportedOperationException.class, () -> underTest.createDiskEncryptionSet(requestedSet), "Serious problem");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldReturnExistingDiskEncryptionSetWithoutPolling() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(des, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSetByName(any(String.class), any(String.class))).thenReturn(des);
        initRetry();
        // Return the same DES instance to simulate that the poller checker task instantly completed
        when(diskEncryptionSetCreationPoller.startPolling(eq(authenticatedContext), any(DiskEncryptionSetCreationCheckerContext.class), eq(des)))
                .thenReturn(des);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);

        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroupName(), "dummyResourceGroup");
        assertThat(createdDes.getDiskEncryptionSetId()).isEqualTo(DES_RESOURCE_ID);
        verify(azureClient, never()).createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class));
        verify(azureClient).grantKeyVaultAccessPolicyToServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);

        verifyPersistedCloudResource();
    }

    private void verifyPersistedCloudResource() {
        ArgumentCaptor<CloudResource> cloudResourceCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier).notifyAllocation(cloudResourceCaptor.capture(), eq(cloudContext));
        CloudResource cloudResourceCaptured = cloudResourceCaptor.getValue();
        assertThat(cloudResourceCaptured.getName()).isEqualTo("dummyEnvName-DES-uniqueId");
        assertThat(cloudResourceCaptured.getType()).isEqualTo(AZURE_DISK_ENCRYPTION_SET);
        assertThat(cloudResourceCaptured.getReference()).isEqualTo(DES_RESOURCE_ID);
        assertThat(cloudResourceCaptured.getStatus()).isEqualTo(CREATED);
    }

    private void initRetry() {
        when(retryService.testWith2SecDelayMax15Times(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    @Test
    public void testCreateDiskEncryptionSetShouldReturnExistingDiskEncryptionSetWithPolling() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        DiskEncryptionSetInner desInitial = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED))
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(desInitial, "id", DES_RESOURCE_ID);
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner desAfterPolling = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(desAfterPolling, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSetByName(any(String.class), any(String.class))).thenReturn(desInitial);
        initRetry();
        // Return a different DES instance to simulate that the poller checker task initially indicated incomplete, hence the final DES was obtained by the
        // scheduled execution of the poller
        when(diskEncryptionSetCreationPoller.startPolling(eq(authenticatedContext), any(DiskEncryptionSetCreationCheckerContext.class), eq(desInitial)))
                .thenReturn(desAfterPolling);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);

        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroupName(), "dummyResourceGroup");
        assertThat(createdDes.getDiskEncryptionSetId()).isEqualTo(DES_RESOURCE_ID);
        verify(azureClient, never()).createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class));
        verify(azureClient).grantKeyVaultAccessPolicyToServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);

        verifyPersistedCloudResource();
    }

    @Test
    public void testCreateDiskEncryptionSetShouldReturnNewlyCreatedDiskEncryptionSetIfNotAlreadyExists() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(des, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSetByName(any(String.class), any(String.class))).thenReturn(null);
        when(azureClient.createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class))).thenReturn(des);
        initRetry();
        // Return the same DES instance to simulate that the poller checker task instantly completed
        when(diskEncryptionSetCreationPoller.startPolling(eq(authenticatedContext), any(DiskEncryptionSetCreationCheckerContext.class), eq(des)))
                .thenReturn(des);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);

        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroupName(), "dummyResourceGroup");
        verify(azureClient).grantKeyVaultAccessPolicyToServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);

        verifyPersistedCloudResource();
    }

    @Test
    public void testCreateDiskEncryptionSetShouldReturnNewlyCreatedDiskEncryptionSetIfNotAlreadyExistsAndGrantKeyVaultAccessPolicyError() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withSingleResourceGroup(true)
                .withResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(des, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSetByName(any(String.class), any(String.class))).thenReturn(null);
        when(azureClient.createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class))).thenReturn(des);
        initRetry();
        // Return the same DES instance to simulate that the poller checker task instantly completed
        when(diskEncryptionSetCreationPoller.startPolling(eq(authenticatedContext), any(DiskEncryptionSetCreationCheckerContext.class), eq(des)))
                .thenReturn(des);
        doThrow(new UnsupportedOperationException("Serious problem")).when(azureClient)
                .grantKeyVaultAccessPolicyToServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        initExceptionConversion();
        initActionFailedExceptionConversion();

        verifyActionFailedException(UnsupportedOperationException.class, () -> underTest.createDiskEncryptionSet(requestedSet), "Serious problem");

        verifyPersistedCloudResource();
    }

    private void verifyActionFailedException(Class<? extends Throwable> expectedType, Executable executable, String messageExpected) {
        CloudConnectorException cloudConnectorExceptionOuter = assertThrows(CloudConnectorException.class, executable);

        Retry.ActionFailedException actionFailedException = verifyAndGetCause(Retry.ActionFailedException.class, cloudConnectorExceptionOuter);
        CloudConnectorException cloudConnectorExceptionInner = verifyAndGetCause(CloudConnectorException.class, actionFailedException);
        assertThat(verifyAndGetCause(expectedType, cloudConnectorExceptionInner)).hasMessage(messageExpected);
    }

    @Test
    void testDeleteDiskEncryptionSetShouldReturnSilentlyWhenThereIsNoCloudResource() {
        List<CloudResource> resources = List.of();
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClientService, never()).getClient(any(CloudCredential.class));
    }

    @Test
    void testDeleteDiskEncryptionSetExceptionThrownWhenInvalidCredential() {
        List<CloudResource> resources = getResources("dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        when(azureClientService.getClient(cloudCredential)).thenThrow(new UnsupportedOperationException("Serious problem"));
        initExceptionConversion();

        verifyException(UnsupportedOperationException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest), "Serious problem");
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldThrowExceptionWhenDiskEncryptionSetNameIsNotFound() {
        List<CloudResource> resources = getResources("dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest),
                "Failed to deduce Disk Encryption Set name from given resource id \"dummyDesId\"");
    }

    private void initCloudResourceHelper(List<CloudResource> resources) {
        when(cloudResourceHelper.getResourceTypeFromList(AZURE_DISK_ENCRYPTION_SET, resources))
                .thenReturn(resources.isEmpty() ? Optional.empty() : Optional.of(resources.iterator().next()));
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldThrowExceptionWhenResourceGroupIsNotFound() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/wrongValuesFramed/diskEncryptionSets/dummyDesName");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest),
                "Failed to deduce Disk Encryption Set's resource group name from given resource id " +
                        "\"/subscriptions/dummySubscriptionId/resourceGroups/wrongValuesFramed/diskEncryptionSets/dummyDesName\"");
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldDeduceValidResourceGroupAndDiskEncryptionSetName() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                "Microsoft.Compute/diskEncryptionSets/dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion");
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(des);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClient).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(azureClient).removeKeyVaultAccessPolicyFromServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().iterator().next(), deletionRequest.getCloudContext());
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldNotMakeCloudCallWhenDiskEncryptionSetIsNotFound() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                "Microsoft.Compute/diskEncryptionSets/dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(null);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClient, never()).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().iterator().next(), deletionRequest.getCloudContext());
    }

    @Test
    void testDeleteDiskEncryptionSetWhenExceptionDuringDiskEncryptionSetExistenceCheck() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                "Microsoft.Compute/diskEncryptionSets/dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        initCloudResourceHelper(resources);
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenThrow(new UnsupportedOperationException("Serious problem"));
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();
        initExceptionConversion();
        initActionFailedExceptionConversion();

        verifyActionFailedException(UnsupportedOperationException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest), "Serious problem");

        verify(azureClient, never()).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(persistenceNotifier, never()).notifyDeletion(deletionRequest.getCloudResources().iterator().next(), deletionRequest.getCloudContext());
    }

    private List<CloudResource> getResources(String desId) {
        CloudResource desCloudResource = new CloudResource.Builder()
                .name("Des")
                .type(AZURE_DISK_ENCRYPTION_SET)
                .reference(desId)
                .status(CREATED)
                .build();
        return List.of(desCloudResource);
    }

}