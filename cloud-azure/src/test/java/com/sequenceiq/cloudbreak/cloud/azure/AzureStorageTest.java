package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.implementation.StorageAccountInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
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

    @InjectMocks
    private AzureStorage underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindStorageAccountIdInVisibleSubscriptionsWhenPresentInCurrentSubscription() {
        AzureClient client = mock(AzureClient.class);
        StorageAccount storageAccount = mock(StorageAccount.class);
        when(storageAccount.id()).thenReturn("storageAccountOneId");
        when(client.getStorageAccount(STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION, Kind.STORAGE_V2)).thenReturn(Optional.of(storageAccount));

        Optional<String> storageAccountId = underTest.findStorageAccountIdInVisibleSubscriptions(client, "storageAccountOne");

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

        Optional<String> storageAccountId = underTest.findStorageAccountIdInVisibleSubscriptions(client, "storageAccountAnother");

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
        PagedList<Subscription> subscriptionPagedList = Mockito.spy(PagedList.class);

        Subscription subscriptionOne = mock(Subscription.class);
        when(subscriptionOne.subscriptionId()).thenReturn(SUBSCRIPTION_ONE);
        subscriptionPagedList.add(subscriptionOne);

        Subscription subscriptionTwo = mock(Subscription.class);
        when(subscriptionTwo.subscriptionId()).thenReturn(SUBSCRIPTION_ANOTHER);
        subscriptionPagedList.add(subscriptionTwo);

        when(client.listSubscriptions()).thenReturn(subscriptionPagedList);
    }

    private CloudCredential createCloudCredential() {
        Map<String, String> azureParameters = new HashMap<>();
        azureParameters.put("subscriptionId", SUBSCRIPTION_ONE);
        CloudCredential cloudCredential = new CloudCredential("id", "name");
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
