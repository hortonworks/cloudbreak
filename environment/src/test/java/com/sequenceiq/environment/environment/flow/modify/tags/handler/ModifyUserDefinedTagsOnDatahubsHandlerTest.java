package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsOnDatahubsHandlerTest {
    private static final long ENV_ID = 1L;

    private static final String ENV_NAME = "envName";

    private static final String ENV_CRN = "crn";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private StackPollerService stackPollerService;

    @InjectMocks
    private ModifyUserDefinedTagsOnDatahubsHandler underTest;

    private HandlerEvent<EnvTagsModificationEvent> event;

    @BeforeEach
    void setUp() {
        EnvTagsModificationEvent request = EnvTagsModificationEvent.builder()
                .withSelector(MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT.selector())
                .withResourceId(ENV_ID)
                .withResourceName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .withUserDefinedTags(USER_DEFINED_TAGS)
                .build();
        event = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void testDoAcceptSuccess() {
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verify(stackPollerService).updateUserDefinedTagsOnStacks(ENV_ID, ENV_CRN, USER_DEFINED_TAGS, StackType.WORKLOAD);
    }

    @Test
    void testDoAcceptFailure() {
        when(stackPollerService.updateUserDefinedTagsOnStacks(ENV_ID, ENV_CRN, USER_DEFINED_TAGS, StackType.WORKLOAD))
                .thenThrow(new RuntimeException("error"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationFailureEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
    }
}