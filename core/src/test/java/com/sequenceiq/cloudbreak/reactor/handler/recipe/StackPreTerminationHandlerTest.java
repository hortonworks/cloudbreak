package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.service.cluster.flow.telemetry.TelemetryAgentService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class StackPreTerminationHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private TelemetryAgentService telemetryAgentService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StackPreTerminationHandler underTest;

    @Test
    void testIfSecurityConfigIsNull() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        StackView stackView = mock(StackView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getSecurityConfig()).thenReturn(null);
        when(stackDtoService.getById(any())).thenReturn(stackDto);

        underTest.accept(new Event<>(new StackPreTerminationRequest(1L, false)));

        verify(eventBus).notify(any(), any());
        verifyNoInteractions(telemetryAgentService);
    }

}
