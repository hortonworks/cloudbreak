package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils.withRuntimeVersionAndFlags;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesFactory;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeValidationActionsTest {

    private static final Long STACK_ID = 1L;

    private static final String IMAGE_ID = "target-image-uuid";

    private static final String TARGET_RUNTIME_VERSION = "7.3.1";

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ClusterUpgradePropertiesFactory clusterUpgradePropertiesFactory;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private FlowParameters flowParameters;

    @InjectMocks
    private ClusterUpgradeValidationActions underTest;

    private StackContext context;

    private StackDto stackDto;

    @BeforeEach
    void setUp() {
        stackDto = mock(StackDto.class);
        context = new StackContext(flowParameters, stackDto, null, null, null);
    }

    @Test
    void initActionCreatesUpgradePropertiesAndFiresImageValidationEvent() throws Exception {
        AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent> action =
                (AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent>) underTest.initClusterUpgradeValidation();
        initActionPrivateFields(action);
        ClusterUpgradeValidationTriggerEvent triggerEvent = new ClusterUpgradeValidationTriggerEvent(STACK_ID, null, IMAGE_ID, true, false, true);
        when(messagesService.getMessage(ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED.getMessage())).thenReturn("started");

        ClusterUpgradeProperties clusterUpgradeProperties = withRuntimeVersionAndFlags(TARGET_RUNTIME_VERSION, true, false, true);
        when(clusterUpgradePropertiesFactory.create(STACK_ID, IMAGE_ID, true, false, true)).thenReturn(clusterUpgradeProperties);

        Event event = mock(Event.class);
        ArgumentCaptor<ClusterUpgradeS3guardValidationFinishedEvent> emittedPayload =
                ArgumentCaptor.forClass(ClusterUpgradeS3guardValidationFinishedEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), emittedPayload.capture())).thenReturn(event);

        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context, triggerEvent, variables);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_STARTED, "started");
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED);
        verify(eventBus).notify(eq(ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_IMAGE_VALIDATION_EVENT.name()), eq(event));

        assertThat(emittedPayload.getValue().getImageId()).isEqualTo(clusterUpgradeProperties.getTargetImageId());
        assertThat(emittedPayload.getValue().getResourceId()).isEqualTo(STACK_ID);
        assertThat(emittedPayload.getValue().getClusterUpgradeProperties()).isEqualTo(clusterUpgradeProperties);
    }

    @Test
    void initActionPropagatesCloudbreakImageNotFoundExceptionFromPropertiesFactory() throws Exception {
        AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent> action =
                (AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent>) underTest.initClusterUpgradeValidation();
        initActionPrivateFields(action);
        ClusterUpgradeValidationTriggerEvent triggerEvent = new ClusterUpgradeValidationTriggerEvent(STACK_ID, null, IMAGE_ID, false, false, false);

        CloudbreakImageNotFoundException expected = new CloudbreakImageNotFoundException("image gone");
        when(clusterUpgradePropertiesFactory.create(STACK_ID, IMAGE_ID, false, false, false)).thenThrow(expected);

        assertThatThrownBy(() -> new AbstractActionTestSupport<>(action).doExecute(context, triggerEvent, new HashMap<>()))
                .isSameAs(expected);
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
