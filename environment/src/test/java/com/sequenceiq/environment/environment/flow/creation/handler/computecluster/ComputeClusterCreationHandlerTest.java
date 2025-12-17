package com.sequenceiq.environment.environment.flow.creation.handler.computecluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class ComputeClusterCreationHandlerTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    @Mock
    private EventSender eventSender;

    @Mock
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ComputeClusterCreationHandler underTest;

    @Captor
    private ArgumentCaptor<Event<EnvCreationEvent>> successfulEventCaptor;

    @Captor
    private ArgumentCaptor<Event<EnvCreationFailureEvent>> failureEventCaptor;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        successfulEventCaptor = ArgumentCaptor.forClass(Event.class);
        failureEventCaptor = ArgumentCaptor.forClass(Event.class);
        FieldUtils.writeField(underTest, "eventBus", eventBus, true);
    }

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("CREATE_COMPUTE_CLUSTER_EVENT");
    }

    @Test
    void acceptAndSendNextStateEventWhenComputeClusterCreationSuccessfullyStarted() {
        doNothing().when(eventBus).notify(anyString(), successfulEventCaptor.capture());
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentByIdOrThrow(anyLong())).thenReturn(environment);

        underTest.accept(Event.wrap(environmentDto));

        assertEquals(EnvCreationEvent.class, successfulEventCaptor.getValue().getData().getClass());
        EnvCreationEvent envCreationEvent = successfulEventCaptor.getValue().getData();

        assertThat(envCreationEvent).isNotNull();
        assertThat(envCreationEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreationEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreationEvent.selector()).isEqualTo("START_NETWORK_CREATION_EVENT");
        verify(externalizedComputeService, times(1)).createComputeCluster(any());
    }

    @Test
    void sendEnvCreationFailureEventWhenNoEnvironmentFound() {
        doNothing().when(eventBus).notify(anyString(), failureEventCaptor.capture());
        EnvironmentDto environmentDto = createEnvironmentDto();
        when(environmentService.findEnvironmentByIdOrThrow(anyLong()))
                .thenThrow(NotFoundException.notFound("environment", environmentDto.getId()).get());


        underTest.accept(Event.wrap(environmentDto));

        assertEquals(EnvCreationFailureEvent.class, failureEventCaptor.getValue().getData().getClass());
        EnvCreationFailureEvent envCreationFailureEvent = failureEventCaptor.getValue().getData();
        assertEquals("environment '1' not found.", envCreationFailureEvent.getException().getMessage());
    }

    @Test
    void sendEnvCreationFailureEventWhenComputeClusterCreationFailed() {
        doNothing().when(eventBus).notify(anyString(), failureEventCaptor.capture());
        EnvironmentDto environmentDto = createEnvironmentDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentByIdOrThrow(anyLong())).thenReturn(environment);
        doThrow(new RuntimeException("error")).when(externalizedComputeService).createComputeCluster(any());

        underTest.accept(Event.wrap(environmentDto));

        assertEquals(EnvCreationFailureEvent.class, failureEventCaptor.getValue().getData().getClass());
        EnvCreationFailureEvent envCreationFailureEvent = failureEventCaptor.getValue().getData();
        assertEquals("error", envCreationFailureEvent.getException().getMessage());
    }

    private EnvironmentDto createEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setName(ENVIRONMENT_NAME);
        Credential credential = new Credential();
        credential.setResourceCrn("credential");
        environmentDto.setCredential(credential);
        return environmentDto;
    }
}