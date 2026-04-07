package com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifytags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsStackHandlerTest {
    private static final long STACK_ID = 1L;

    private static final String STACK_CRN = "crn";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("newKey", "newValue", "custom", "value2");

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    private HandlerEvent<ModifyUserDefinedTagsStackHandlerEvent> event;

    @InjectMocks
    private ModifyUserDefinedTagsStackHandler underTest;

    @BeforeEach
    void setUp() {
        ModifyUserDefinedTagsStackHandlerEvent request = new ModifyUserDefinedTagsStackHandlerEvent(STACK_ID, USER_DEFINED_TAGS);
        event = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void testDoAcceptSuccess() {
        Stack stack = new Stack();
        stack.setResourceCrn(STACK_CRN);

        when(stackService.getById(STACK_ID)).thenReturn(stack);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verify(stackUpdater).updateUserDefinedTags(stack, USER_DEFINED_TAGS);
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(stackService).getById(STACK_ID);
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsFailedEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
    }
}