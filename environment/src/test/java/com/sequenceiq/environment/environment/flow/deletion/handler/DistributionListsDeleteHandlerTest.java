package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DISTRIBUTION_LIST;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.notification.sender.DistributionListManagementService;

@ExtendWith(MockitoExtension.class)
class DistributionListsDeleteHandlerTest {

    private static final Long ENV_ID = 123L;

    private static final String ENV_NAME = "test-env";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:resource-123";

    private static final boolean FORCE_DELETE = false;

    @Mock
    private DistributionListManagementService distributionListManagementService;

    @InjectMocks
    private DistributionListsDeleteHandler underTest;

    private EnvironmentDeletionDto environmentDeletionDto;

    private EnvironmentDto environmentDto;

    @BeforeEach
    void setUp() {
        environmentDto = EnvironmentDto.builder()
                .withId(ENV_ID)
                .withName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .build();

        environmentDeletionDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withForceDelete(FORCE_DELETE)
                .build();
    }

    @Test
    void selectorReturnsCorrectValue() {
        assertEquals(DELETE_DISTRIBUTION_LIST.selector(), underTest.selector());
    }

    @Test
    void doAcceptDeletesDistributionListSuccessfully() {
        HandlerEvent<EnvironmentDeletionDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDeletionDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService).deleteDistributionList(ENV_CRN);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) result;
        assertEquals(ENV_ID, envDeleteEvent.getResourceId());
        assertEquals(ENV_NAME, envDeleteEvent.getResourceName());
        assertEquals(ENV_CRN, envDeleteEvent.getResourceCrn());
        assertEquals(FORCE_DELETE, envDeleteEvent.isForceDelete());
    }

    @Test
    void doAcceptHandlesExceptionGracefully() {
        RuntimeException exception = new RuntimeException("Distribution list deletion failed");
        doThrow(exception).when(distributionListManagementService).deleteDistributionList(ENV_CRN);

        HandlerEvent<EnvironmentDeletionDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDeletionDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService).deleteDistributionList(ENV_CRN);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) result;
        assertEquals(ENV_ID, envDeleteEvent.getResourceId());
        assertEquals(ENV_NAME, envDeleteEvent.getResourceName());
        assertEquals(ENV_CRN, envDeleteEvent.getResourceCrn());
        assertEquals(FORCE_DELETE, envDeleteEvent.isForceDelete());
    }

    @Test
    void doAcceptWithForceDeleteTrue() {
        EnvironmentDeletionDto forceDeleteDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withForceDelete(true)
                .build();

        HandlerEvent<EnvironmentDeletionDto> handlerEvent = new HandlerEvent<>(new Event<>(forceDeleteDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService).deleteDistributionList(ENV_CRN);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) result;
        assertEquals(true, envDeleteEvent.isForceDelete());
    }

    @Test
    void defaultFailureEventReturnsCorrectEvent() {
        Event<EnvironmentDeletionDto> event = new Event<>(environmentDeletionDto);
        Exception exception = new Exception("Test exception");

        Selectable result = underTest.defaultFailureEvent(ENV_ID, exception, event);

        assertNotNull(result);
        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) result;
        assertEquals(ENV_ID, envDeleteEvent.getResourceId());
        assertEquals(ENV_NAME, envDeleteEvent.getResourceName());
        assertEquals(ENV_CRN, envDeleteEvent.getResourceCrn());
        assertEquals(FORCE_DELETE, envDeleteEvent.isForceDelete());
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), envDeleteEvent.getSelector());
    }

    @Test
    void defaultFailureEventWithForceDeleteTrue() {
        EnvironmentDeletionDto forceDeleteDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(environmentDto)
                .withForceDelete(true)
                .build();

        Event<EnvironmentDeletionDto> event = new Event<>(forceDeleteDto);
        Exception exception = new Exception("Test exception");

        Selectable result = underTest.defaultFailureEvent(ENV_ID, exception, event);

        assertNotNull(result);
        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) result;
        assertEquals(true, envDeleteEvent.isForceDelete());
    }

    @Test
    void doAcceptLogsExceptionAndContinuesWithDeletion() {
        RuntimeException exception = new RuntimeException("Service unavailable");
        doThrow(exception).when(distributionListManagementService).deleteDistributionList(ENV_CRN);

        HandlerEvent<EnvironmentDeletionDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDeletionDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService, times(1)).deleteDistributionList(anyString());
    }

    @Test
    void doAcceptDeletesDistributionListWithoutException() {
        HandlerEvent<EnvironmentDeletionDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDeletionDto));

        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        verify(distributionListManagementService, times(1)).deleteDistributionList(ENV_CRN);
        assertEquals(FINISH_ENV_DELETE_EVENT.selector(), result.getSelector());
    }
}
