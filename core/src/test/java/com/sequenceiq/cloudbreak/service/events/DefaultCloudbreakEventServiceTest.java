package com.sequenceiq.cloudbreak.service.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.BaseLegacyStructuredFlowEventFactory;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class DefaultCloudbreakEventServiceTest {

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private BaseLegacyStructuredFlowEventFactory baseLegacyStructuredFlowEventFactory;

    @Mock
    private StackToStackV4ResponseConverter stackV4ResponseConverter;

    @Mock
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Mock
    private EventBus reactor;

    @InjectMocks
    private DefaultCloudbreakEventService underTest;

    @Test
    void emittedEventHasNoStackResponseButIsStillNotifiedWhenStackConversionFails() {
        when(stack.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(stackV4ResponseConverter.convert(stack)).thenThrow(new RuntimeException("simulated conversion failure"));
        Event<CloudbreakCompositeEvent> event = new Event<>(null);
        doReturn(event).when(eventFactory).createEvent(any());

        underTest.fireCloudbreakEvent(stack, ResourceEvent.STACK_DELETE_COMPLETED, List.of());

        ArgumentCaptor<CloudbreakCompositeEvent> emitted = ArgumentCaptor.forClass(CloudbreakCompositeEvent.class);
        verify(eventFactory).createEvent(emitted.capture());
        assertThat(emitted.getValue().getStackResponse()).isNull();
        assertThat(emitted.getValue().getResourceEvent()).isEqualTo(ResourceEvent.STACK_DELETE_COMPLETED);
        verify(reactor).notify("CLOUDBREAK_EVENT", event);
    }
}
