package com.sequenceiq.cloudbreak.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.structuredevent.LegacyDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.notification.WebSocketNotificationService;

@ExtendWith(MockitoExtension.class)
class CloudbreakEventHandlerTest {

    private static final ResourceEvent RESOURCE_EVENT = ResourceEvent.STACK_DELETE_COMPLETED;

    @Mock
    private LegacyDefaultStructuredEventClient legacyStructuredEventAsyncNotifier;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private CloudbreakEventHandler underTest;

    @Test
    void dropsWebSocketNotificationWhenStackResponseIsMissing() {
        StructuredNotificationEvent structuredEvent = new StructuredNotificationEvent();
        CloudbreakCompositeEvent compositeEvent = new CloudbreakCompositeEvent(RESOURCE_EVENT, List.of(), structuredEvent);
        assertThat(compositeEvent.getStackResponse()).isNull();

        underTest.accept(new Event<>(compositeEvent));

        verify(legacyStructuredEventAsyncNotifier).sendStructuredEvent(structuredEvent);
        verify(webSocketNotificationService, never()).send(any(), any(), any(), any(), any());
    }

    @Test
    void sendsWebSocketNotificationWhenStackResponsePresent() {
        StructuredNotificationEvent structuredEvent = new StructuredNotificationEvent();
        StackV4Response stackResponse = new StackV4Response();
        CloudbreakCompositeEvent compositeEvent =
                new CloudbreakCompositeEvent(RESOURCE_EVENT, List.of(), structuredEvent, stackResponse, "owner");

        underTest.accept(new Event<>(compositeEvent));

        verify(webSocketNotificationService).send(eq(RESOURCE_EVENT), eq(List.of()), eq(stackResponse), eq("owner"), any());
    }
}
