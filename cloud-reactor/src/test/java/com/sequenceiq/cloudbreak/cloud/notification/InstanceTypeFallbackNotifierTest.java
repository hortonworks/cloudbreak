package com.sequenceiq.cloudbreak.cloud.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class InstanceTypeFallbackNotifierTest {

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private CloudContext cloudContext;

    @InjectMocks
    private InstanceTypeFallbackNotifier underTest;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(cloudContext.getId()).thenReturn(42L);
        org.mockito.Mockito.lenient().when(eventFactory.createEvent(any())).thenAnswer(inv -> new Event<>(inv.getArgument(0)));
    }

    @Test
    void firesReactorEventOnFirstReport() {
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        verify(eventBus, times(1)).notify(eq("instance-type-fallback"), any());
    }

    @Test
    void dedupsRepeatedReportsForSameTuple() {
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        verify(eventBus, times(1)).notify(eq("instance-type-fallback"), any());
    }

    @Test
    void distinctFallbackTuplesEachFireOwnEvent() {
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m4.large", "InsufficientInstanceCapacity");
        verify(eventBus, times(2)).notify(eq("instance-type-fallback"), any());
    }

    @Test
    void fallbackAndExhaustedForSameGroupBothFire() {
        underTest.reportFallback(cloudContext, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        underTest.reportFallbackExhausted(cloudContext, "master", "m5.xlarge", "InsufficientInstanceCapacity");
        verify(eventBus, times(2)).notify(eq("instance-type-fallback"), any());
    }

    @Test
    void skipsWhenCloudContextIsNull() {
        underTest.reportFallback(null, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        verify(eventBus, never()).notify(any(), any());
    }

    @Test
    void skipsWhenStackIdIsNull() {
        CloudContext noStackId = org.mockito.Mockito.mock(CloudContext.class);
        when(noStackId.getId()).thenReturn(null);
        underTest.reportFallback(noStackId, "master", "m5.xlarge", "m5.large", "InsufficientInstanceCapacity");
        verify(eventBus, never()).notify(any(), any());
    }
}
