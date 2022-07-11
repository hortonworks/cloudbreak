package com.sequenceiq.periscope.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final String DEFAULT_CLIENT_ID = "default";

    private static final String DEFAULT_NOTIFICATION_ENDPOINT = "http://localhost:3000/notifications";

    @InjectMocks
    private SubscriptionService underTest;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Test
    void testCreateDefaultSubscription() {
        Subscription aSubscription = getASubscription();

        when(subscriptionRepository.findByClientId(DEFAULT_CLIENT_ID)).thenReturn(Optional.empty());
        underTest.subscribe(aSubscription);

        verify(subscriptionRepository, times(1)).save(aSubscription);
    }

    @Test
    void testUpdateSubscriptionWhenEndpointIsDifferent() {
        Subscription aSubscription = getASubscription();
        Subscription dbSubscription = getASubscription();
        dbSubscription.setEndpoint("http://differenturl");

        when(subscriptionRepository.findByClientId(DEFAULT_CLIENT_ID))
                .thenReturn(Optional.of(dbSubscription));
        underTest.subscribe(aSubscription);

        assertEquals(DEFAULT_NOTIFICATION_ENDPOINT,
                dbSubscription.getEndpoint(), "Updated endpoint address should match");
        verify(subscriptionRepository, times(1)).save(dbSubscription);
    }

    @Test
    void testUpdateSubscriptionWhenEndpointIsSame() {
        Subscription aSubscription = getASubscription();
        Subscription dbSubscription = getASubscription();

        when(subscriptionRepository.findByClientId(DEFAULT_CLIENT_ID))
                .thenReturn(Optional.of(dbSubscription));
        underTest.subscribe(aSubscription);

        verify(subscriptionRepository, never()).save(aSubscription);
    }

    private Subscription getASubscription() {
        Subscription subscription = new Subscription();
        subscription.setClientId(DEFAULT_CLIENT_ID);
        subscription.setEndpoint(DEFAULT_NOTIFICATION_ENDPOINT);
        return subscription;
    }
}
