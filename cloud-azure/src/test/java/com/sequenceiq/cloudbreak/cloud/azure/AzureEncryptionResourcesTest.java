package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK_ENCRYPTION_SET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_RESOURCE_GROUP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.keyvault.models.KeyAttributes;
import com.microsoft.azure.keyvault.models.KeyBundle;
import com.microsoft.azure.keyvault.webkey.JsonWebKey;
import com.microsoft.azure.management.compute.DiskEncryptionSetIdentityType;
import com.microsoft.azure.management.compute.DiskEncryptionSetType;
import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.KeyVaultAndKeyReference;
import com.microsoft.azure.management.compute.SourceVault;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.microsoft.azure.management.resources.ResourceGroup;
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
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

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

    private static Object[][] testResourceGroupNameExtractionDataProvider() {
        return new Object[][]{
                //testCaseName     resourceId     expectedResourceGroupName}
                {"testResourceGroupName - RG name has only alphabets",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyAlphaResourceGroup/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyAlphaResourceGroup"},
                {"testResourceGroupName - RG name is combination of alphabets and numbers",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyAlphaNumResourceGroup0910/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyAlphaNumResourceGroup0910"},
                {"testResourceGroupName - RG name is combination of alphabets, numbers and hyphens",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyAlphaNumHyphenResourceGroup---0910/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyAlphaNumHyphenResourceGroup---0910"},
                {"testResourceGroupName - RG name is combination of alphabets, numbers, hyphens and periods",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyAlphaNumHyphenPeriodResourceGroup---....0910/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyAlphaNumHyphenPeriodResourceGroup---....0910"},
                {"testResourceGroupName - RG name is combination of alphabets, numbers, hyphens, periods and Parentheses",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyAlphaParenthesesPeriodResourceGroup---....)()(0910/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyAlphaParenthesesPeriodResourceGroup---....)()(0910"},
                {"testResourceGroupName - RG name is combination of alphabets, numbers, hyphens, periods, Parentheses and underscores",
                        "/subscriptions/dummySubscriptionId/resourceGroups/" +
                                "dummyUnderscoreResourceGroup---___....)()(0910/providers/Microsoft.Compute/dummyResourceObject/dummyResourceObjectId",
                        "dummyUnderscoreResourceGroup---___....)()(0910"}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testResourceGroupNameExtractionDataProvider")
    public void testResourceGroupNameExtraction(String testName, String resourceId, String expectedResourceGroupName) {
        Matcher matcher = underTest.RESOURCE_GROUP_NAME.matcher(resourceId);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(1), expectedResourceGroupName);
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
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
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
    public void testCreateDiskEncryptionSetShouldCheckEncryptionKeyExistenceOnCloudAndThrowExistenceError() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/wrongKey")
                .build();
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet),
                "Key specified with keyUrl 'https://dummyVaultName.vault.azure.net/wrongKey' - either does not exist or insufficient" +
                        " permissions to access it. Please ensure that the key exists and user has 'List' permissions on the keyVault dummyVaultName");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldCheckEncryptionKeyExistenceOnCloudAndThrowEnabledError() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle()
                        .withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.FALSE))
                        .withKey(new JsonWebKey().withKid("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")));
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet),
                "keyName dummyKeyName is not enabled to be used for encryption. Please 'enable' the key using Azure portal.");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldMakeCloudCallAndThrowException() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        Subscription subscription = mock(Subscription.class);
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId("envName-DES-", "uniqueId")).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
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
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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

    private void verifyPersistedResourceGroupAndDiskEncryptionSetCloudResource(String resourceGroupId) {
        ArgumentCaptor<CloudResource> cloudResourceCaptor = ArgumentCaptor.forClass(CloudResource.class);
        verify(persistenceNotifier, times(2)).notifyAllocation(cloudResourceCaptor.capture(), eq(cloudContext));
        verifyCloudResource(cloudResourceCaptor.getAllValues(), AZURE_DISK_ENCRYPTION_SET, DES_RESOURCE_ID, CREATED);
        verifyCloudResource(cloudResourceCaptor.getAllValues(), AZURE_RESOURCE_GROUP, resourceGroupId, CREATED);
    }

    private void verifyCloudResource(List<CloudResource> updatedCloudResources, ResourceType type, String expectedReference, CommonStatus expectedStatus) {
        updatedCloudResources
                .stream()
                .filter(cloudResource -> cloudResource.getType().equals(type))
                .findFirst()
                .ifPresent(cloudResource -> {
                            assertThat(cloudResource.getReference()).isEqualTo(expectedReference);
                            assertThat(cloudResource.getStatus()).isEqualTo(expectedStatus);
                        });
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
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
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
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
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
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
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
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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

    @Test
    public void testCreateDiskEncryptionSetShouldReturnNewlyCreatedDiskEncryptionSetWhenDesAndVaultResourceGroupAreDifferentAndDesNotAlreadyExists() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withDiskEncryptionSetResourceGroupName("dummyResourceGroup")
                .withEncryptionKeyResourceGroupName("dummyVaultResourceGroup")
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
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyVaultResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion")
                .withTags(new HashMap<>());
        ReflectionTestUtils.setField(des, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
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
        verify(azureClient).grantKeyVaultAccessPolicyToServicePrincipal("dummyVaultResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);

        verifyPersistedCloudResource();
    }

    @Test
    public void testCreateDiskEncryptionSetThrowsExceptionWhenNotSingleResourceGroupAndVaultResourceGroupNameIsNotPresent() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withDiskEncryptionSetResourceGroupName(null)
                .withTags(new HashMap<>())
                .withEncryptionKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                .build();
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        initExceptionConversion();

        verifyException(IllegalArgumentException.class, () -> underTest.createDiskEncryptionSet(requestedSet),
                "Encryption key resource group name should be present if resource group is not provided during environment creation. " +
                        "At least one of --resource-group-name or --encryption-key-resource-group-name should be specified.");
    }

    @Test
    public void testCreateDiskEncryptionSetShouldReturnNewlyCreatedDiskEncryptionSetIfNotAlreadyExistsAndCreateNewResourceGroupWhenIsNotSingleResourceGroup() {
        DiskEncryptionSetCreationRequest requestedSet = new DiskEncryptionSetCreationRequest.Builder()
                .withId("uniqueId")
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withEncryptionKeyResourceGroupName("dummyResourceGroup")
                .withDiskEncryptionSetResourceGroupName(null)
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
        ResourceGroup resourceGroup = mock(ResourceGroup.class);
        ReflectionTestUtils.setField(des, "id", DES_RESOURCE_ID);
        Subscription subscription = mock(Subscription.class);
        when(azureClient.checkEncryptionKeyExistenceOnCloud(any(String.class), any(String.class), any(String.class)))
                .thenReturn(new KeyBundle().withAttributes((KeyAttributes) new KeyAttributes().withEnabled(Boolean.TRUE)));
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(new ResourcePersisted());
        when(subscription.subscriptionId()).thenReturn("dummySubscriptionId");
        when(azureUtils.generateDesNameByNameAndId(any(String.class), any(String.class))).thenReturn("dummyEnvName-DES-uniqueId");
        when(azureClientService.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        when(azureClient.getCurrentSubscription()).thenReturn(subscription);
        when(azureClient.getDiskEncryptionSetByName(any(String.class), any(String.class))).thenReturn(null);
        when(azureUtils.generateResourceGroupNameByNameAndId(any(String.class), any(String.class))).thenReturn("envName-CDP_DES-uniqueId");
        when(azureClient.resourceGroupExists(eq("envName-CDP_DES-uniqueId"))).thenReturn(Boolean.FALSE);
        when(azureClient.createResourceGroup(eq("envName-CDP_DES-uniqueId"), eq("dummyRegion"), any(HashMap.class))).thenReturn(resourceGroup);
        when(azureClient.createDiskEncryptionSet(any(String.class), any(String.class), any(String.class),
                any(String.class), any(String.class), any(Map.class))).thenReturn(des);
        initRetry();
        // Return the same DES instance to simulate that the poller checker task instantly completed
        when(diskEncryptionSetCreationPoller.startPolling(eq(authenticatedContext), any(DiskEncryptionSetCreationCheckerContext.class), eq(des)))
                .thenReturn(des);

        CreatedDiskEncryptionSet createdDes = underTest.createDiskEncryptionSet(requestedSet);

        assertEquals(createdDes.getDiskEncryptionSetLocation(), "dummyRegion");
        assertEquals(createdDes.getDiskEncryptionSetResourceGroupName(), "envName-CDP_DES-uniqueId");
        verify(azureClient).grantKeyVaultAccessPolicyToServicePrincipal("dummyResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        verify(azureClient).createResourceGroup(eq("envName-CDP_DES-uniqueId"), eq("dummyRegion"), any(HashMap.class));
        verifyPersistedResourceGroupAndDiskEncryptionSetCloudResource(resourceGroup.id());
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
    public void testDeleteDiskEncryptionSetShouldDeduceValidResourceGroupAndDiskEncryptionSetNameWhenDesAndVaultHaveSameResourceGroup() {
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
    public void testDeleteDiskEncryptionSetShouldDeduceValidDiskEncryptionSetNameAndResourceGroupWhenDesAndVaultDoNotHaveSameResourceGroup() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyDesResourceGroup/providers/" +
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
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyVaultResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion");
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(des);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClient).deleteDiskEncryptionSet("dummyDesResourceGroup", "dummyDesId");
        verify(azureClient).removeKeyVaultAccessPolicyFromServicePrincipal("dummyVaultResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().iterator().next(), deletionRequest.getCloudContext());
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldThrowExceptionWhenVaultResourceGroupIsNotFound() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyDesResourceGroup/providers/" +
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
                                .withId("invaildSourceVault")))
                .withIdentity(identity)
                .withLocation("dummyRegion");
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(des);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();
        initExceptionConversion();
        initActionFailedExceptionConversion();

        verifyActionFailedException(IllegalArgumentException.class, () -> underTest.deleteDiskEncryptionSet(deletionRequest),
                "Failed to deduce vault resource group name from source vault ID " +
                        "\"invaildSourceVault\"");
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

    @Test
    public void testDeleteDiskEncryptionSetShouldDeduceValidDiskEncryptionSetNameAndCheckAndDeleteResourceGroupWhenDesResourceGroupIsCreatedByCDP() {
        CloudResource desCloudResource = new CloudResource.Builder()
                .name("Des")
                .type(AZURE_DISK_ENCRYPTION_SET)
                .reference("/subscriptions/dummySubscriptionId/resourceGroups/dummy-CDP_DES-ResourceGroup/providers/" +
                        "Microsoft.Compute/diskEncryptionSets/dummyDesId")
                .status(CREATED)
                .build();
        CloudResource rgCloudResource = new CloudResource.Builder()
                .name("dummy-CDP_DES-ResourceGroup")
                .type(AZURE_RESOURCE_GROUP)
                .reference("uniqueDummyId")
                .status(CREATED)
                .build();
        List<CloudResource> resources = List.of(desCloudResource, rgCloudResource);
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyVaultResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion");
        when(cloudResourceHelper.getResourceTypeFromList(AZURE_DISK_ENCRYPTION_SET, resources))
                .thenReturn(resources.isEmpty() ? Optional.empty() : Optional.of(resources.get(0)));
        when(cloudResourceHelper.getResourceTypeFromList(AZURE_RESOURCE_GROUP, resources))
                .thenReturn(resources.isEmpty() ? Optional.empty() : Optional.of(resources.get(1)));
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(des);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        when(azureClient.resourceGroupExists(eq("dummy-CDP_DES-ResourceGroup"))).thenReturn(Boolean.TRUE);
        initRetry();

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClient).deleteDiskEncryptionSet("dummy-CDP_DES-ResourceGroup", "dummyDesId");
        verify(azureClient).removeKeyVaultAccessPolicyFromServicePrincipal("dummyVaultResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        verify(azureClient).deleteResourceGroup("dummy-CDP_DES-ResourceGroup");
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().get(0), deletionRequest.getCloudContext());
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().get(1), deletionRequest.getCloudContext());
    }

    @Test
    public void testDeleteDiskEncryptionSetShouldDeduceValidDiskEncryptionSetNameAndShouldNotDeleteResourceGroupWhenNotCreatedByCDP() {
        List<CloudResource> resources = getResources("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/providers/" +
                "Microsoft.Compute/diskEncryptionSets/dummyDesId");
        DiskEncryptionSetDeletionRequest deletionRequest = new DiskEncryptionSetDeletionRequest.Builder()
                .withCloudCredential(cloudCredential)
                .withCloudContext(cloudContext)
                .withCloudResources(resources)
                .build();
        EncryptionSetIdentity identity = new EncryptionSetIdentity().withType(DiskEncryptionSetIdentityType.SYSTEM_ASSIGNED);
        ReflectionTestUtils.setField(identity, "principalId", DES_PRINCIPAL_ID);
        DiskEncryptionSetInner des = (DiskEncryptionSetInner) new DiskEncryptionSetInner()
                .withEncryptionType(DiskEncryptionSetType.ENCRYPTION_AT_REST_WITH_CUSTOMER_KEY)
                .withActiveKey(new KeyVaultAndKeyReference()
                        .withKeyUrl("https://dummyVaultName.vault.azure.net/keys/dummyKeyName/dummyKeyVersion")
                        .withSourceVault(new SourceVault()
                                .withId("/subscriptions/dummySubs/resourceGroups/dummyVaultResourceGroup/providers/Microsoft.KeyVault/vaults/dummyVaultName")))
                .withIdentity(identity)
                .withLocation("dummyRegion");
        initCloudResourceHelper(resources);
        when(azureClient.getDiskEncryptionSetByName(any(), any())).thenReturn(des);
        when(azureClientService.getClient(cloudCredential)).thenReturn(azureClient);
        initRetry();

        underTest.deleteDiskEncryptionSet(deletionRequest);

        verify(azureClient).deleteDiskEncryptionSet("dummyResourceGroup", "dummyDesId");
        verify(azureClient, never()).deleteResourceGroup("dummyResourceGroup");
        verify(azureClient).removeKeyVaultAccessPolicyFromServicePrincipal("dummyVaultResourceGroup", "dummyVaultName", DES_PRINCIPAL_ID);
        verify(persistenceNotifier).notifyDeletion(deletionRequest.getCloudResources().get(0), deletionRequest.getCloudContext());
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