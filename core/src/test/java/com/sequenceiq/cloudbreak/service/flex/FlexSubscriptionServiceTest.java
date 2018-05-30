package com.sequenceiq.cloudbreak.service.flex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionCallback;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

public class FlexSubscriptionServiceTest {

    @Mock
    private FlexSubscriptionRepository flexRepo;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private FlexSubscriptionService underTest;

    @Before
    public void setUp() throws TransactionExecutionException {
        initMocks(this);
        doAnswer(invocation -> ((TransactionCallback) invocation.getArgument(0)).get()).when(transactionService).required(any());
    }

    @Test(expected = BadRequestException.class)
    public void testCreateShouldThrowBadRequestWhenSubscriptionExistsWithTheSameName() throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(1L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000");

        underTest.create(subscription);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateShouldThrowBadRequestWhenSubscriptionExistsWithTheSameSubscriptionIdentifier() throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(0L);
        when(flexRepo.countBySubscriptionId(anyString())).thenReturn(1L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription1", "FLEX-000000001");

        underTest.create(subscription);
    }

    @Test
    public void testCreateShouldSetDefaultFlagsForTheFirstSavedSubscription() throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(0L);
        when(flexRepo.countBySubscriptionId(anyString())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", false, false);
        when(flexRepo.save(subscription)).thenReturn(subscription);
        when(flexRepo.findAllByAccount(anyString())).thenReturn(Collections.singletonList(subscription));

        FlexSubscription result = underTest.create(subscription);

        verify(flexRepo, times(1)).save(subscription);
        verify(flexRepo, times(1)).save(Arrays.asList(result));
        assertTrue(result.isDefault());
        assertTrue(result.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateDefaultFlagsOfOldSubscriptionsWhenNewSubscriptionRequiresDefaultFlags() throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(0L);
        when(flexRepo.countBySubscriptionId(anyString())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", true, true);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexRepo.save(subscription)).thenReturn(subscription);
        when(flexRepo.findAllByAccount(anyString())).thenReturn(Arrays.asList(subscription1, subscription));

        FlexSubscription result = underTest.create(subscription);

        verify(flexRepo, times(1)).save(subscription);

        verify(flexRepo, times(1)).save(Arrays.asList(subscription1, result));
        assertTrue(result.isDefault());
        assertTrue(result.isUsedForController());
        assertFalse(subscription1.isDefault());
        assertFalse(subscription1.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateUsedForControllerFlagOfOldSubscriptionsWhenNewSubscriptionCreatedAsUsedForController()
            throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(0L);
        when(flexRepo.countBySubscriptionId(anyString())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", true, false);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexRepo.save(subscription)).thenReturn(subscription);
        when(flexRepo.findAllByAccount(anyString())).thenReturn(Arrays.asList(subscription1, subscription));

        FlexSubscription result = underTest.create(subscription);

        verify(flexRepo, times(1)).save(subscription);
        verify(flexRepo, times(1)).save(Arrays.asList(subscription1, result));
        assertFalse(result.isDefault());
        assertTrue(result.isUsedForController());
        assertTrue(subscription1.isDefault());
        assertFalse(subscription1.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateDefaultFlagOfOldSubscriptionsWhenNewSubscriptionCreatedAsDefault() throws TransactionExecutionException {
        when(flexRepo.countByNameAndAccount(anyString(), anyString())).thenReturn(0L);
        when(flexRepo.countBySubscriptionId(anyString())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", false, true);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexRepo.save(subscription)).thenReturn(subscription);
        when(flexRepo.findAllByAccount(anyString())).thenReturn(Arrays.asList(subscription1, subscription));

        FlexSubscription result = underTest.create(subscription);

        verify(flexRepo, times(1)).save(subscription);
        verify(flexRepo, times(1)).save(Arrays.asList(subscription1, result));
        assertTrue(result.isDefault());
        assertFalse(result.isUsedForController());
        assertFalse(subscription1.isDefault());
        assertTrue(subscription1.isUsedForController());
    }

    private FlexSubscription getFlexSubscription(String name, String subscriptionId) {
        return getFlexSubscription(name, subscriptionId, false, false);
    }

    private FlexSubscription getFlexSubscription(String name, String subscriptionId, boolean usedForController, boolean usedAsDefault) {
        FlexSubscription subscription = new FlexSubscription();
        subscription.setName(name);
        subscription.setSubscriptionId(subscriptionId);
        subscription.setAccount("BIGACCOUNT");
        subscription.setOwner("MINE");
        subscription.setUsedForController(usedForController);
        subscription.setDefault(usedAsDefault);
        SmartSenseSubscription sSSubscription = new SmartSenseSubscription();
        subscription.setSmartSenseSubscription(sSSubscription);
        return subscription;
    }
}