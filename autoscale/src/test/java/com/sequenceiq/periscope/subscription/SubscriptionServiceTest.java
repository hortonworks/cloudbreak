package com.sequenceiq.periscope.subscription;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceTest {

    @InjectMocks
    SubscriptionService underTest;

    @Mock
    SubscriptionRepository subscriptionRepository;

    String defaultClientId = "default";

    String defaultNotificationEndpoint = "http://localhost:3000/notifications";

    @Test
    public void testCreateDefaultSubscription() {
        Subscription aSubscription = getASubscription();

        when(subscriptionRepository.findByClientId(defaultClientId)).thenReturn(Optional.empty());
        underTest.subscribe(aSubscription);

        verify(subscriptionRepository, times(1)).save(aSubscription);
    }

    @Test
    public void testUpdateSubscriptionWhenEndpointIsDifferent() {
        Subscription aSubscription = getASubscription();
        Subscription dbSubscription = getASubscription();
        dbSubscription.setEndpoint("http://differenturl");

        when(subscriptionRepository.findByClientId(defaultClientId))
                .thenReturn(Optional.of(dbSubscription));
        underTest.subscribe(aSubscription);

        assertEquals("Updated endpoint address should match",
                defaultNotificationEndpoint, dbSubscription.getEndpoint());
        verify(subscriptionRepository, times(1)).save(dbSubscription);
    }

    @Test
    public void testUpdateSubscriptionWhenEndpointIsSame() {
        Subscription aSubscription = getASubscription();
        Subscription dbSubscription = getASubscription();

        when(subscriptionRepository.findByClientId(defaultClientId))
                .thenReturn(Optional.of(dbSubscription));
        underTest.subscribe(aSubscription);

        verify(subscriptionRepository, never()).save(aSubscription);
    }

    private Subscription getASubscription() {
        Subscription subscription = new Subscription();
        subscription.setClientId(defaultClientId);
        subscription.setEndpoint(defaultNotificationEndpoint);
        return subscription;
    }
}