package com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.handler;

import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT;
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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.stack.modify.tags.event.ModifyUserDefinedTagsStackHandlerEvent;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsStackHandlerTest {
    private static final long STACK_ID = 1L;

    private static final String STACK_CRN = "crn";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("newKey", "newValue", "custom", "value2");

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackUpdater dbStackUpdater;

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
        DBStack dbStack = new DBStack();
        dbStack.setResourceCrn(STACK_CRN);

        when(dbStackService.getById(STACK_ID)).thenReturn(dbStack);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.name(), result.getSelector());
        verify(dbStackUpdater).updateUserDefinedTags(dbStack, USER_DEFINED_TAGS);
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(dbStackService).getById(STACK_ID);
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ModifyUserDefinedTagsFailedEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_REDBEAMS_EVENT.name(), result.getSelector());
    }
}