package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsOnDatalakeHandlerTest {
    private static final long ENV_ID = 1L;

    private static final String ENV_NAME = "envName";

    private static final String ENV_CRN = "crn";

    private static final String SDX_CRN = "sdxCrn";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("owner", "john doe");

    @Mock
    private SdxService sdxService;

    @Mock
    private StackService stackService;

    @InjectMocks
    private ModifyUserDefinedTagsOnDatalakeHandler underTest;

    private HandlerEvent<EnvTagsModificationEvent> event;

    private SdxClusterResponse sdxClusterResponse;

    @BeforeEach
    void setUp() {
        EnvTagsModificationEvent request = EnvTagsModificationEvent.builder()
                .withSelector(MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT.selector())
                .withResourceId(ENV_ID)
                .withResourceName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .withUserDefinedTags(USER_DEFINED_TAGS)
                .build();
        event = new HandlerEvent<>(new Event<>(request));
        SdxClusterResponse sdxClusterResponse = new SdxClusterResponse();
        sdxClusterResponse.setCrn(SDX_CRN);

        when(sdxService.list(ENV_NAME)).thenReturn(List.of(sdxClusterResponse));
    }

    @Test
    void testDoAcceptSuccess() {
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT.name(), result.getSelector());
        verify(stackService).modifyUserDefinedTags(SDX_CRN, USER_DEFINED_TAGS);
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(stackService).modifyUserDefinedTags(SDX_CRN, USER_DEFINED_TAGS);
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationFailureEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verify(stackService).modifyUserDefinedTags(SDX_CRN, USER_DEFINED_TAGS);
    }
}