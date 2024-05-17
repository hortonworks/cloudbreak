package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterReInitializationActionsTest {

    public static final String USER_CRN = "userCrn";

    @InjectMocks
    private ExternalizedComputeClusterReInitializationActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Mock
    private ExternalizedComputeService externalizedComputeService;

    @Mock
    private EnvironmentService environmentService;

    private CommonContext context;

    @BeforeEach
    void setUp() {
        context = new CommonContext(flowParameters);
    }

    @Test
    public void testDefaultComputeClusterCreation() throws Exception {
        AbstractExternalizedComputeReInitializationAction<ExternalizedComputeClusterReInitializationEvent> externalizedComputeClusterDelete =
                (AbstractExternalizedComputeReInitializationAction<ExternalizedComputeClusterReInitializationEvent>)
                        underTest.defaultComputeClusterReinitialization();
        initActionPrivateFields(externalizedComputeClusterDelete);
        ExternalizedComputeClusterReInitializationEvent createEvent = ExternalizedComputeClusterReInitializationEvent.builder().withForce(true).build();

        EnvironmentDto envDto = new EnvironmentDto();
        envDto.setId(1L);
        when(environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, createEvent,
                EnvironmentStatus.COMPUTE_CLUSTER_REINITIALIZATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_STARTED,
                ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE))
                .thenReturn(envDto);
        Environment environment = new Environment();
        when(environmentService.findEnvironmentByIdOrThrow(envDto.getId())).thenReturn(environment);
        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), eq(envDto))).thenReturn(event);

        new AbstractActionTestSupport<>(externalizedComputeClusterDelete).doExecute(context, createEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(
                context, createEvent, EnvironmentStatus.COMPUTE_CLUSTER_REINITIALIZATION_IN_PROGRESS,
                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_STARTED,
                ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE
        );
        verify(environmentService, times(1)).findEnvironmentByIdOrThrow(1L);
        verify(externalizedComputeService, times(1)).reInitializeComputeCluster(environment, true);
        verify(eventBus)
                .notify(eq(ExternalizedComputeClusterReInitializationHandlerSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_WAIT_HANDLER_EVENT.selector()),
                        eq(event));
    }

    @Test
    public void testDefaultComputeClusterCreationFinished() throws Exception {
        AbstractExternalizedComputeReInitializationAction<ExternalizedComputeClusterReInitializationEvent> externalizedComputeClusterDelete =
                (AbstractExternalizedComputeReInitializationAction<ExternalizedComputeClusterReInitializationEvent>)
                        underTest.defaultComputeClusterReinitializationFinished();
        initActionPrivateFields(externalizedComputeClusterDelete);
        ExternalizedComputeClusterReInitializationEvent createEvent = ExternalizedComputeClusterReInitializationEvent.builder().build();

        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), eq(createEvent))).thenReturn(event);

        new AbstractActionTestSupport<>(externalizedComputeClusterDelete).doExecute(context, createEvent, Map.of());

        verify(environmentStatusUpdateService, times(1)).updateEnvironmentStatusAndNotify(context, createEvent, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED,
                ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE);
        verify(eventBus)
                .notify(eq(DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT.event()), eq(event));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}