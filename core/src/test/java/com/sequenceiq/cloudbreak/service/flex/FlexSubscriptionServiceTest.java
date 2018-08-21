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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

public class FlexSubscriptionServiceTest {

    @Mock
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private FlexSubscriptionService underTest;

    @Mock
    private Organization organization;

    @Mock
    private User user;

    @Before
    public void setUp() throws TransactionExecutionException {
        initMocks(this);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(organizationService.getDefaultOrganizationForUser(user)).thenReturn(organization);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateShouldThrowBadRequestWhenSubscriptionExistsWithTheSameName() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(1L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000");

        underTest.create(subscription, organization, user);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateShouldThrowBadRequestWhenSubscriptionExistsWithTheSameSubscriptionIdentifier() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(0L);
        when(flexSubscriptionRepository.countBySubscriptionIdAndOrganization(anyString(), any())).thenReturn(1L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription1", "FLEX-000000001");

        underTest.create(subscription, organization, user);
    }

    @Test
    public void testCreateShouldSetDefaultFlagsForTheFirstSavedSubscription() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(0L);
        when(flexSubscriptionRepository.countBySubscriptionIdAndOrganization(anyString(), any())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", false, false);
        when(flexSubscriptionRepository.save(subscription)).thenReturn(subscription);
        Set<FlexSubscription> allByOrganization = new HashSet<>(Collections.singletonList(subscription));
        when(flexSubscriptionRepository.findAllByOrganization(any())).thenReturn(allByOrganization);
        when(organizationService.retrieveForUser(any())).thenReturn(Stream.of(organization).collect(Collectors.toSet()));

        FlexSubscription result = underTest.create(subscription, organization, user);

        verify(flexSubscriptionRepository, times(1)).save(subscription);
        verify(flexSubscriptionRepository, times(1)).saveAll(allByOrganization);
        assertTrue(result.isDefault());
        assertTrue(result.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateDefaultFlagsOfOldSubscriptionsWhenNewSubscriptionRequiresDefaultFlags() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(0L);
        when(flexSubscriptionRepository.countBySubscriptionIdAndOrganization(anyString(), any())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", true, true);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexSubscriptionRepository.save(subscription)).thenReturn(subscription);
        Set<FlexSubscription> allByOrganization = new HashSet<>(Arrays.asList(subscription1, subscription));
        when(flexSubscriptionRepository.findAllByOrganization(any())).thenReturn(allByOrganization);
        when(organizationService.retrieveForUser(any())).thenReturn(Stream.of(organization).collect(Collectors.toSet()));

        FlexSubscription result = underTest.create(subscription, organization, user);

        verify(flexSubscriptionRepository, times(1)).save(subscription);
        verify(flexSubscriptionRepository, times(1)).saveAll(allByOrganization);
        assertTrue(result.isDefault());
        assertTrue(result.isUsedForController());
        assertFalse(subscription1.isDefault());
        assertFalse(subscription1.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateUsedForControllerFlagOfOldSubscriptionsWhenNewSubscriptionCreatedAsUsedForController() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(0L);
        when(flexSubscriptionRepository.countBySubscriptionIdAndOrganization(anyString(), any())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", true, false);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexSubscriptionRepository.save(subscription)).thenReturn(subscription);
        Set<FlexSubscription> allByOrg = new HashSet<>(Arrays.asList(subscription1, subscription));
        when(flexSubscriptionRepository.findAllByOrganization(any())).thenReturn(allByOrg);
        when(organizationService.retrieveForUser(any())).thenReturn(Stream.of(organization).collect(Collectors.toSet()));

        FlexSubscription result = underTest.create(subscription, organization, user);

        verify(flexSubscriptionRepository, times(1)).save(subscription);
        verify(flexSubscriptionRepository, times(1)).saveAll(allByOrg);
        assertFalse(result.isDefault());
        assertTrue(result.isUsedForController());
        assertTrue(subscription1.isDefault());
        assertFalse(subscription1.isUsedForController());
    }

    @Test
    public void testCreateShouldUpdateDefaultFlagOfOldSubscriptionsWhenNewSubscriptionCreatedAsDefault() {
        when(flexSubscriptionRepository.countByNameAndOrganization(anyString(), any())).thenReturn(0L);
        when(flexSubscriptionRepository.countBySubscriptionIdAndOrganization(anyString(), any())).thenReturn(0L);
        FlexSubscription subscription = getFlexSubscription("testFlexSubscription", "FLEX-000000000", false, true);
        FlexSubscription subscription1 = getFlexSubscription("testFlexSubscription1", "FLEX-000000001", true, true);
        when(flexSubscriptionRepository.save(subscription)).thenReturn(subscription);
        Set<FlexSubscription> allByOrg = new HashSet<>(Arrays.asList(subscription1, subscription));
        when(flexSubscriptionRepository.findAllByOrganization(any())).thenReturn(allByOrg);
        when(organizationService.retrieveForUser(any())).thenReturn(Stream.of(organization).collect(Collectors.toSet()));

        FlexSubscription result = underTest.create(subscription, organization, user);

        verify(flexSubscriptionRepository, times(1)).save(subscription);
        verify(flexSubscriptionRepository, times(1)).saveAll(allByOrg);
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
        subscription.setAccount("userName");
        subscription.setOwner("userId");
        subscription.setUsedForController(usedForController);
        subscription.setDefault(usedAsDefault);
        SmartSenseSubscription sSSubscription = new SmartSenseSubscription();
        subscription.setSmartSenseSubscription(sSSubscription);
        return subscription;
    }

}