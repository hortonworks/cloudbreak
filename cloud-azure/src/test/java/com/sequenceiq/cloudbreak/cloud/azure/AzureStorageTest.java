package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.resourcemanager.resources.models.Subscription;
import com.azure.resourcemanager.storage.fluent.models.StorageAccountInner;
import com.azure.resourcemanager.storage.models.Kind;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.azure.service.AzureClientCachedOperations;
import com.sequenceiq.cloudbreak.cloud.azure.storage.SkuTypeResolver;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

public class AzureStorageTest {

    private static final String STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION = "storageAccountOne";

    private static final String STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION = "storageAccountAnother";

    private static final String SUBSCRIPTION_ONE = "subscriptionOne";

    private static final String SUBSCRIPTION_ANOTHER = "subscriptionAnother";

    private static final String STACK_NAME = "Test Cluster";

    private static final Region REGION = Region.region("westus2");

    private static final String RESOURCE_GROUP = "resource-group";

    @Mock
    private AzureUtils armUtils;

    @Mock
    private SkuTypeResolver skuTypeResolver;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Spy
    private AzureClientCachedOperations azureClientCachedOperations;

    @InjectMocks
    private AzureStorage underTest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindStorageAccountIdInVisibleSubscriptionsWhenPresentInCurrentSubscription() {
        AzureClient client = mock(AzureClient.class);
        StorageAccount storageAccount = mock(StorageAccount.class);
        when(storageAccount.id()).thenReturn("storageAccountOneId");
        when(client.getStorageAccount(STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION, Kind.STORAGE_V2)).thenReturn(Optional.of(storageAccount));

        Optional<String> storageAccountId = underTest.findStorageAccountIdInVisibleSubscriptions(client, "storageAccountOne", "");

        assertTrue(storageAccountId.isPresent());
        assertEquals("storageAccountOneId", storageAccountId.get());
        verify(client).getStorageAccount(STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION, Kind.STORAGE_V2);
        verify(client, never()).listSubscriptions();
    }

    @Test
    public void testFindStorageAccountIdInVisibleSubscriptionsWhenPresentInAnotherSubscription() {
        AzureClient client = mock(AzureClient.class);
        StorageAccountInner storageAccount = mock(StorageAccountInner.class);
        when(storageAccount.id()).thenReturn(STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION);
        when(client.getStorageAccount(STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION, Kind.STORAGE_V2)).thenReturn(Optional.empty());
        setupSubscriptions(client);
        when(client.getStorageAccountBySubscription(STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION, SUBSCRIPTION_ONE, Kind.STORAGE_V2))
                .thenReturn(Optional.empty());
        when(client.getStorageAccountBySubscription(STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION, SUBSCRIPTION_ANOTHER, Kind.STORAGE_V2))
                .thenReturn(Optional.of(storageAccount));

        Optional<String> storageAccountId = underTest.findStorageAccountIdInVisibleSubscriptions(client, "storageAccountAnother", "");

        assertTrue(storageAccountId.isPresent());
        assertEquals(STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION, storageAccountId.get());
        verify(client).getStorageAccount(STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION, Kind.STORAGE_V2);
        verify(client).listSubscriptions();
    }

    @Test
    public void testImageStorageNameForSingleRG() {
        ReflectionTestUtils.setField(underTest, "imageStorePrefix", "cbimg");

        CloudCredential cloudCredential = createCloudCredential();
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        CloudContext cloudContext = createCloudContext();

        when(azureResourceGroupMetadataProvider.useSingleResourceGroup(cloudStack)).thenReturn(true);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack)).thenReturn(RESOURCE_GROUP);
        when(armUtils.encodeString(RESOURCE_GROUP)).thenReturn("2b8305c3");
        when(armUtils.encodeString("subscriptionone")).thenReturn("33890668");

        String imageStorageName = underTest.getImageStorageName(azureCredentialView, cloudContext, cloudStack);
        assertEquals("cbimgwu2338906682b8305c3", imageStorageName);
    }

    @Test
    public void testImageStorageNameForNonSingleRG() {
        ReflectionTestUtils.setField(underTest, "imageStorePrefix", "cbimg");
        CloudCredential cloudCredential = createCloudCredential();
        AzureCredentialView azureCredentialView = new AzureCredentialView(cloudCredential);
        CloudContext cloudContext = createCloudContext();

        when(azureResourceGroupMetadataProvider.useSingleResourceGroup(cloudStack)).thenReturn(false);
        when(armUtils.encodeString("subscriptionone")).thenReturn("33890668");
        when(armUtils.encodeString(null)).thenReturn("");

        String imageStorageName = underTest.getImageStorageName(azureCredentialView, cloudContext, cloudStack);
        assertEquals("cbimgwu2subscriptionone", imageStorageName);
    }

    private void setupSubscriptions(AzureClient client) {
        List<Subscription> subscriptions = new ArrayList<>();

        Subscription subscriptionOne = mock(Subscription.class);
        when(subscriptionOne.subscriptionId()).thenReturn(SUBSCRIPTION_ONE);
        subscriptions.add(subscriptionOne);

        Subscription subscriptionTwo = mock(Subscription.class);
        when(subscriptionTwo.subscriptionId()).thenReturn(SUBSCRIPTION_ANOTHER);
        subscriptions.add(subscriptionTwo);

        AzureListResult<Subscription> azureListResult = mock(AzureListResult.class);
        lenient().when(azureListResult.getAll()).thenReturn(subscriptions);
        lenient().when(azureListResult.getStream()).thenReturn(subscriptions.stream());
        when(client.listSubscriptions()).thenReturn(azureListResult);
    }

    private CloudCredential createCloudCredential() {
        Map<String, String> azureParameters = new HashMap<>();
        azureParameters.put("subscriptionId", SUBSCRIPTION_ONE);
        CloudCredential cloudCredential = new CloudCredential("id", "name", "account");
        cloudCredential.putParameter("azure", azureParameters);
        return cloudCredential;
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(REGION, AvailabilityZone.availabilityZone("westus2"));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withName(STACK_NAME)
                .withLocation(location)
                .build();
        return cloudContext;
    }
}
