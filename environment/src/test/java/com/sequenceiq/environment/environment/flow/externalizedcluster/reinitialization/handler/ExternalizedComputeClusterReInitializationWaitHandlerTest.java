package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.handler;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterReInitializationWaitHandlerTest {

    @InjectMocks
    private ExternalizedComputeClusterReInitializationWaitHandler underTest;

    @Mock
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventBus eventBus;

    @Test
    public void testDoAccept() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        environmentDto.setName("env");
        Environment environment = new Environment();
        environment.setName("env");
        when(environmentService.findEnvironmentByIdOrThrow(environmentDto.getId())).thenReturn(environment);
        String defaultClusterName = "default";
        when(externalizedComputeService.getDefaultComputeClusterName(environment.getName())).thenReturn(defaultClusterName);
        underTest.accept(new Event<>(environmentDto));

        verify(environmentService, times(1)).findEnvironmentByIdOrThrow(environmentDto.getId());
        verify(externalizedComputeService, times(1)).awaitComputeClusterCreation(environment, defaultClusterName);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eq(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_EVENT.selector()), eventArgumentCaptor.capture());
        assertEquals(environmentDto.getName(), ((BaseNamedFlowEvent) eventArgumentCaptor.getValue().getData()).getResourceName());
        assertEquals(environmentDto.getResourceCrn(), ((ResourceCrnPayload) eventArgumentCaptor.getValue().getData()).getResourceCrn());
        assertEquals(environmentDto.getResourceId(), ((Payload) eventArgumentCaptor.getValue().getData()).getResourceId());
    }

    @Test
    public void testDoAcceptButAwaitFailed() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        Environment environment = new Environment();
        environment.setName("env");
        when(environmentService.findEnvironmentByIdOrThrow(environmentDto.getId())).thenReturn(environment);
        String defaultClusterName = "default";
        when(externalizedComputeService.getDefaultComputeClusterName(environment.getName())).thenReturn(defaultClusterName);
        doThrow(new ExternalizedComputeOperationFailedException("error"))
                .when(externalizedComputeService).awaitComputeClusterCreation(environment, defaultClusterName);
        underTest.accept(new Event<>(environmentDto));

        verify(environmentService, times(1)).findEnvironmentByIdOrThrow(environmentDto.getId());
        verify(externalizedComputeService, times(1)).awaitComputeClusterCreation(environment, defaultClusterName);
        ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eq(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_EVENT.selector()), eventArgumentCaptor.capture());
        assertEquals(environmentDto.getName(), ((BaseNamedFlowEvent) eventArgumentCaptor.getValue().getData()).getResourceName());
        assertEquals(environmentDto.getResourceCrn(), ((ResourceCrnPayload) eventArgumentCaptor.getValue().getData()).getResourceCrn());
        assertEquals(environmentDto.getResourceId(), ((Payload) eventArgumentCaptor.getValue().getData()).getResourceId());
        assertEquals("error", ((Payload) eventArgumentCaptor.getValue().getData()).getException()
                .getMessage());
    }
}