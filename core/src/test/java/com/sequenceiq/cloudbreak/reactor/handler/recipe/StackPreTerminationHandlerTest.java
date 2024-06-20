package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.flow.PreTerminationStateExecutor;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.cluster.flow.telemetry.TelemetryAgentService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
public class StackPreTerminationHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RecipeEngine recipeEngine;

    @Mock
    private TelemetryAgentService telemetryAgentService;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private PreTerminationStateExecutor preTerminationStateExecutor;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private StackPreTerminationHandler underTest;

    @Captor
    private ArgumentCaptor<Event<StackPreTerminationSuccess>> successEventCaptor;

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

    @ParameterizedTest
    @EnumSource(value = TerminationType.class, names = {"REGULAR", "FORCED"})
    void testAccept(TerminationType terminationType) throws CloudbreakException {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getSecurityConfig()).thenReturn(new SecurityConfig());
        when(stackDtoService.getById(any())).thenReturn(stackDto);
        Set<HostGroup> hostGroups = Set.of(new HostGroup());
        when(hostGroupService.getByClusterWithRecipes(1L)).thenReturn(hostGroups);

        underTest.accept(new Event<>(new StackPreTerminationRequest(1L, terminationType.isForced())));

        verify(telemetryAgentService).stopTelemetryAgent(stackDto);
        verify(recipeEngine).executePreTerminationRecipes(stackDto, hostGroups, terminationType.isForced());
        verify(preTerminationStateExecutor).runPreTerminationTasks(stackDto);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(StackPreTerminationSuccess.class)), successEventCaptor.capture());
        StackPreTerminationSuccess result = successEventCaptor.getValue().getData();
        assertEquals(1L, result.getResourceId());
        assertEquals(terminationType, result.getTerminationType());
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(StackPreTerminationRequest.class), underTest.selector());
    }
}
