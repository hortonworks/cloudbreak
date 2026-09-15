package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_FALLBACK;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_FALLBACK_EXHAUSTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.notification.model.InstanceTypeFallbackEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class InstanceTypeFallbackNotificationHandlerTest {

    private static final Long STACK_ID = 123L;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private StackView stackView;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private InstanceTypeFallbackNotificationHandler underTest;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
    }

    @Test
    void firesInstanceGroupEventWithCreateInProgressStatus() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        Event<InstanceTypeFallbackEvent> event = new Event<>(new InstanceTypeFallbackEvent(cloudContext, "master", "m5.xlarge", "m5.large",
                "InsufficientInstanceCapacity", false));

        underTest.accept(event);

        verify(cloudbreakEventService).fireCloudbreakInstanceGroupEvent(eq(STACK_ID), eq(Status.CREATE_IN_PROGRESS.name()), eq("master"),
                eq(STACK_INSTANCE_TYPE_FALLBACK), eq(List.of("master", "m5.xlarge", "InsufficientInstanceCapacity", "m5.large")));
    }

    @Test
    void firesInstanceGroupEventWithUpdateInProgressStatus() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getStatus()).thenReturn(Status.UPDATE_IN_PROGRESS);
        Event<InstanceTypeFallbackEvent> event = new Event<>(new InstanceTypeFallbackEvent(cloudContext, "worker", "m5.xlarge", "m5.large",
                "SkuNotAvailable", false));

        underTest.accept(event);

        verify(cloudbreakEventService).fireCloudbreakInstanceGroupEvent(eq(STACK_ID), eq(Status.UPDATE_IN_PROGRESS.name()), eq("worker"),
                eq(STACK_INSTANCE_TYPE_FALLBACK), any());
    }

    @Test
    void firesExhaustedEventWithFewerArgs() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenReturn(stackView);
        when(stackView.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);
        Event<InstanceTypeFallbackEvent> event = new Event<>(new InstanceTypeFallbackEvent(cloudContext, "master", "m5.xlarge", null,
                "InsufficientInstanceCapacity", true));

        underTest.accept(event);

        verify(cloudbreakEventService).fireCloudbreakInstanceGroupEvent(eq(STACK_ID), eq(Status.CREATE_IN_PROGRESS.name()), eq("master"),
                eq(STACK_INSTANCE_TYPE_FALLBACK_EXHAUSTED), eq(List.of("master", "m5.xlarge", "InsufficientInstanceCapacity")));
    }

    @Test
    void fallsBackToUnknownStatusWhenLookupFails() {
        when(stackDtoService.getStackViewById(STACK_ID)).thenThrow(new RuntimeException("boom"));
        Event<InstanceTypeFallbackEvent> event = new Event<>(new InstanceTypeFallbackEvent(cloudContext, "master", "m5.xlarge", "m5.large",
                "InsufficientInstanceCapacity", false));

        underTest.accept(event);

        verify(cloudbreakEventService).fireCloudbreakInstanceGroupEvent(eq(STACK_ID), eq("UNKNOWN"), eq("master"),
                eq(STACK_INSTANCE_TYPE_FALLBACK), any());
    }

    @Test
    void skipsWhenStackIdMissing() {
        CloudContext noId = org.mockito.Mockito.mock(CloudContext.class);
        when(noId.getId()).thenReturn(null);
        Event<InstanceTypeFallbackEvent> event = new Event<>(new InstanceTypeFallbackEvent(noId, "master", "m5.xlarge", "m5.large",
                "InsufficientInstanceCapacity", false));

        underTest.accept(event);

        verify(cloudbreakEventService, never()).fireCloudbreakInstanceGroupEvent(anyLong(), anyString(), anyString(), any(), any());
    }
}
