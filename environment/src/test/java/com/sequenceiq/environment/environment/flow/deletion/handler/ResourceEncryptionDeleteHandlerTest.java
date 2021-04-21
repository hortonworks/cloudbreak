package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_PUBLICKEY_DELETE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.encryption.EnvironmentEncryptionService;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.Event.Headers;

@ExtendWith(MockitoExtension.class)
class ResourceEncryptionDeleteHandlerTest {

    private static final Long ENVIRONMENT_ID = 1234L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentEncryptionService environmentEncryptionService;

    @Mock
    private Event<EnvironmentDeletionDto> environmentDtoEvent;

    @Mock
    private Headers headers;

    @InjectMocks
    private ResourceEncryptionDeleteHandler underTest;

    @Captor
    private ArgumentCaptor<BaseNamedFlowEvent> baseNamedFlowEventCaptor;

    @Captor
    private ArgumentCaptor<Headers> headersArgumentCaptor;

    @BeforeEach
    void setUp() {
        EnvironmentDto eventDto = EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AZURE")
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withDiskEncryptionSetId("/subscriptions/dummySubscriptionId/resourceGroups/dummyResourceGroup/" +
                                                "providers/Microsoft.Compute/diskEncryptionSets/dummyDesId")
                                        .build())
                                .build())
                        .build())
                .build();
        EnvironmentDeletionDto environmentDeletionDto = EnvironmentDeletionDto
                .builder()
                .withId(ENVIRONMENT_ID)
                .withForceDelete(false)
                .withEnvironmentDto(eventDto)
                .build();
        when(environmentDtoEvent.getData()).thenReturn(environmentDeletionDto);
        when(environmentDtoEvent.getHeaders()).thenReturn(headers);
        doAnswer(i -> null).when(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), any(Headers.class));
    }

    @Test
    void acceptEnvironmentNotFound() {
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.empty());

        underTest.accept(environmentDtoEvent);

        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void acceptTestEnvironmentFailure() {
        IllegalStateException error = new IllegalStateException("error");
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenThrow(error);

        underTest.accept(environmentDtoEvent);
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteFailedEvent(error);
    }

    @Test
    void testEnvironmentStatusShouldBeUpdatedWhenDesIsDeleted() {
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        underTest.accept(environmentDtoEvent);
        verify(environmentEncryptionService).deleteEncryptionResources(environmentDtoEvent.getData().getEnvironmentDto());
        assertEquals(environment.getStatus(), EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED);
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void testDeletionContinuesIfEnvironmentEncryptionResourcesAreAlreadyDeleted() {
        Environment environment = new Environment();
        environment.setStatus(EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        underTest.accept(environmentDtoEvent);
        assertEquals(environment.getStatus(), EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED);
        verify(environmentEncryptionService, never()).deleteEncryptionResources(environmentDtoEvent.getData().getEnvironmentDto());
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    void testDeletionContinuesIfDiskEncryptionSetIdIsNotPresent() {
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        environmentDtoEvent.getData().setEnvironmentDto(EnvironmentDto.builder()
                .withId(ENVIRONMENT_ID)
                .withResourceCrn(ENVIRONMENT_CRN)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform("AZURE")
                .build());
        underTest.accept(environmentDtoEvent);
        assertEquals(environment.getStatus(), null);
        verify(environmentEncryptionService, never()).deleteEncryptionResources(environmentDtoEvent.getData().getEnvironmentDto());
        verify(eventSender).sendEvent(baseNamedFlowEventCaptor.capture(), headersArgumentCaptor.capture());
        verifyEnvDeleteEvent();
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void selector() {
        assertThat(underTest.selector()).isEqualTo("DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT");
    }

    private void verifyEnvDeleteEvent() {
        BaseNamedFlowEvent event = baseNamedFlowEventCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteEvent.class);

        EnvDeleteEvent envDeleteEvent = (EnvDeleteEvent) event;
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.selector()).isEqualTo(START_PUBLICKEY_DELETE_EVENT.selector());

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }

    private void verifyEnvDeleteFailedEvent(Exception exceptionExpected) {
        BaseNamedFlowEvent event = baseNamedFlowEventCaptor.getValue();
        assertThat(event).isInstanceOf(EnvDeleteFailedEvent.class);

        EnvDeleteFailedEvent envDeleteFailedEvent = (EnvDeleteFailedEvent) event;
        assertThat(envDeleteFailedEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteFailedEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envDeleteFailedEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteFailedEvent.selector()).isEqualTo(FAILED_ENV_DELETE_EVENT.selector());
        assertThat(envDeleteFailedEvent.getException()).isSameAs(exceptionExpected);

        assertThat(headersArgumentCaptor.getValue()).isSameAs(headers);
    }
}
