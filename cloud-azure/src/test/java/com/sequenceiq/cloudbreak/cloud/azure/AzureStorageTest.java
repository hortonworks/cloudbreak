package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.implementation.StorageAccountInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.storage.SkuTypeResolver;

public class AzureStorageTest {

    private static final String STORAGE_ACCOUNT_IN_CURRENT_SUBSCRIPTION = "storageAccountOne";

    private static final String STORAGE_ACCOUNT_IN_ANOTHER_SUBSCRIPTION = "storageAccountAnother";

    private static final String SUBSCRIPTION_ONE = "subscriptionOne";

    private static final String SUBSCRIPTION_ANOTHER = "subscriptionAnother";

    @Mock
    private AzureUtils armUtils;

    @Mock
    private SkuTypeResolver skuTypeResolver;

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
}
