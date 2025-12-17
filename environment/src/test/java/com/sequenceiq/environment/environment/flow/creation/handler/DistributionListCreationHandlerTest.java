package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_DISTRIBUTION_LIST_CREATION_FAILED_WITH_REASON;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_DISTRIBUTION_LISTS_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.sender.DistributionListManagementService;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;

@ExtendWith(MockitoExtension.class)
class DistributionListCreationHandlerTest {

    private static final Long ENV_ID = 123L;

    private static final String ENV_NAME = "test-env";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:resource-123";

    @Mock
    private ParametersService parametersService;

    @Mock
    private DistributionListManagementService distributionListManagementService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private DistributionListCreationHandler underTest;

    private EnvironmentDto environmentDto;

    @BeforeEach
    void setUp() {
        environmentDto = EnvironmentDto.builder()
                .withId(ENV_ID)
                .withName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .build();
    }

    @Test
    void selectorReturnsCorrectValue() {
        assertEquals(CREATE_DISTRIBUTION_LISTS_EVENT.selector(), underTest.selector());
    }

    @Test
    void doAcceptCreatesDistributionListSuccessfully() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-123")
                .resourceCrn(ENV_CRN)
                .build();

        when(distributionListManagementService.createOrUpdateList(any(CreateDistributionListRequest.class)))
                .thenReturn(Optional.of(distributionList));

        HandlerEvent<EnvironmentDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService).createOrUpdateList(any(CreateDistributionListRequest.class));
        verify(parametersService).updateDistributionListDetails(ENV_ID, distributionList);
        verify(eventSenderService, never()).sendEventAndNotification(any(), anyString(), any(), anySet());
    }

    @Test
    void doAcceptWhenDistributionListNotCreatedDoesNotUpdateParameters() {
        when(distributionListManagementService.createOrUpdateList(any(CreateDistributionListRequest.class)))
                .thenReturn(Optional.empty());

        HandlerEvent<EnvironmentDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), result.getSelector());
        verify(distributionListManagementService).createOrUpdateList(any(CreateDistributionListRequest.class));
        verify(parametersService, never()).updateDistributionListDetails(anyLong(), any());
        verify(eventSenderService, never()).sendEventAndNotification(any(), anyString(), any(), anySet());
    }

    @Test
    void doAcceptHandlesExceptionGracefully() {
        RuntimeException exception = new RuntimeException("Distribution list creation failed");
        when(distributionListManagementService.createOrUpdateList(any(CreateDistributionListRequest.class)))
                .thenThrow(exception);

        HandlerEvent<EnvironmentDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), result.getSelector());
        verify(eventSenderService).sendEventAndNotification(
                environmentDto,
                null,
                ENVIRONMENT_DISTRIBUTION_LIST_CREATION_FAILED_WITH_REASON,
                Set.of("Distribution list creation failed")
        );
        verify(parametersService, never()).updateDistributionListDetails(anyLong(), any());
    }

    @Test
    void defaultFailureEventReturnsCorrectEvent() {
        Event<EnvironmentDto> event = new Event<>(environmentDto);
        Exception exception = new Exception("Test exception");

        Selectable result = underTest.defaultFailureEvent(ENV_ID, exception, event);

        assertNotNull(result);
        assertNotNull(result);
        EnvCreationEvent envCreationEvent = (EnvCreationEvent) result;
        assertEquals(ENV_ID, envCreationEvent.getResourceId());
        assertEquals(ENV_NAME, envCreationEvent.getResourceName());
        assertEquals(ENV_CRN, envCreationEvent.getResourceCrn());
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), envCreationEvent.getSelector());
    }

    @Test
    void doAcceptWithNullDistributionListDoesNotThrowException() {
        when(distributionListManagementService.createOrUpdateList(any(CreateDistributionListRequest.class)))
                .thenReturn(null);

        HandlerEvent<EnvironmentDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), result.getSelector());
    }

    @Test
    void doAcceptLogsExceptionAndContinues() {
        RuntimeException exception = new RuntimeException("Service unavailable");
        when(distributionListManagementService.createOrUpdateList(any(CreateDistributionListRequest.class)))
                .thenThrow(exception);

        HandlerEvent<EnvironmentDto> handlerEvent = new HandlerEvent<>(new Event<>(environmentDto));
        Selectable result = underTest.doAccept(handlerEvent);

        assertNotNull(result);
        assertEquals(FINISH_ENV_CREATION_EVENT.selector(), result.getSelector());
        verify(eventSenderService, times(1)).sendEventAndNotification(any(), any(), any(), anySet());
    }
}