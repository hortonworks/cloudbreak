package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_S3GUARD_TABLE_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class IdBrokerMappingsDeleteHandlerTest {

    private static final String MESSAGE = "Houston, we have a problem.";

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private HandlerExceptionProcessor mockExceptionProcessor;

    @Mock
    private GrpcIdbmmsClient idbmmsClient;

    @Mock
    private Event.Headers headers;

    @InjectMocks
    private IdBrokerMappingsDeleteHandler underTest;

    private ArgumentCaptor<BaseNamedFlowEvent> eventArgumentCaptor;

    private ArgumentCaptor<Event.Headers> headersArgumentCaptor;

    private EnvironmentDto environmentDto;

    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @BeforeEach
    void setUp() {
        environmentDto = createEnvironmentDto();
        EnvironmentDeletionDto build = EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(environmentDto)
                .build();
        environmentDtoEvent = new Event<>(headers, build);
        eventArgumentCaptor = ArgumentCaptor.forClass(BaseNamedFlowEvent.class);
        headersArgumentCaptor = ArgumentCaptor.forClass(Event.Headers.class);
    }

    @Test
    void acceptTestNoEnvironment() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        //verify(idbmmsClient, never()).deleteMappings(eq(INTERNAL_ACTOR_CRN), anyString(), eq(Optional.empty()));
        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

//    @Test
//    void acceptTestEnvironmentAndNoIdbmms() {
//        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(IdBrokerMappingSource.NONE)));
//
//        underTest.accept(environmentDtoEvent);
//
//        verify(idbmmsClient, never()).deleteMappings(eq(INTERNAL_ACTOR_CRN), anyString(), eq(Optional.empty()));
//        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
//        verifyEnvDeleteEvent();
//    }

//    @Test
//    void acceptTestEnvironmentAndIdbmmsAndSuccess() {
//        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(IdBrokerMappingSource.IDBMMS)));
//
//        underTest.accept(environmentDtoEvent);
//
//        verify(idbmmsClient).deleteMappings(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty());
//        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
//        verifyEnvDeleteEvent();
//    }

//    @Test
//    void acceptTestEnvironmentAndIdbmmsAndNoMappings() {
//        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(IdBrokerMappingSource.IDBMMS)));
//        doThrow(new IdbmmsOperationException(MESSAGE, createStatusRuntimeException(Status.Code.NOT_FOUND)))
//                .when(idbmmsClient)
//                .deleteMappings(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty());
//
//        underTest.accept(environmentDtoEvent);
//
//        verify(idbmmsClient).deleteMappings(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty());
//        verify(eventSender).sendEvent(eventArgumentCaptor.capture(), headersArgumentCaptor.capture());
//        verifyEnvDeleteEvent();
//    }

//    @Test
//    void acceptTestEnvironmentAndIdbmmsAndFailure() {
//        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(createEnvironment(IdBrokerMappingSource.IDBMMS)));
//        Exception exception = new IdbmmsOperationException(MESSAGE, createStatusRuntimeException(Status.Code.ABORTED));
//        doThrow(exception).when(idbmmsClient).deleteMappings(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty());
//
//        underTest.accept(environmentDtoEvent);
//
//        verify(idbmmsClient).deleteMappings(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty());
//        verify(mockExceptionProcessor, times(1)).handle(any(), any(), any(), any());
//        verify(mockExceptionProcessor, times(1))
//                .handle(any(HandlerFailureConjoiner.class), any(Logger.class), eq(eventSender), eq(DELETE_IDBROKER_MAPPINGS_EVENT.selector()));
//    }

    // Lowering strictness to avoid UnnecessaryStubbingException for the stubs in setUp()
    @MockitoSettings(strictness = Strictness.WARN)
    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo(DELETE_IDBROKER_MAPPINGS_EVENT.selector());
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = eventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_S3GUARD_TABLE_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = eventArgumentCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteFailedEvent.class);

        EnvDeleteFailedEvent envDeleteFailedEvent = (EnvDeleteFailedEvent) event;
        assertThat(envDeleteFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteFailedEvent.selector()).isEqualTo(FAILED_ENV_DELETE_EVENT.selector());
        assertThat(envDeleteFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private EnvironmentDto createEnvironmentDto() {
        return EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withResourceCrn(ENVIRONMENT_CRN)
                .build();
    }

    private Environment createEnvironment(IdBrokerMappingSource mappingSource) {
        Environment env = new Environment();
        env.setId(ENVIRONMENT_ID);
        env.setResourceCrn(ENVIRONMENT_CRN);
        ExperimentalFeatures experimentalFeaturesJson = env.getExperimentalFeaturesJson();
        experimentalFeaturesJson.setIdBrokerMappingSource(mappingSource);
        env.setExperimentalFeaturesJson(experimentalFeaturesJson);
        return env;
    }

    private StatusRuntimeException createStatusRuntimeException(Status.Code code) {
        return new StatusRuntimeException(code.toStatus());
    }

}
