package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.model.OsType;
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
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

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
    void initActionDispatchesPropertiesResolutionWithoutCallingResolver() throws Exception {
        AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent> action =
                (AbstractClusterUpgradeValidationAction<ClusterUpgradeValidationTriggerEvent>) underTest.initClusterUpgradeValidation();
        initActionPrivateFields(action);
        ClusterUpgradeValidationTriggerEvent triggerEvent = new ClusterUpgradeValidationTriggerEvent(STACK_ID, null, IMAGE_ID, true, false, true, null, null);
        when(messagesService.getMessage(ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED.getMessage())).thenReturn("started");

        Event event = mock(Event.class);
        ArgumentCaptor<ClusterUpgradeValidationTriggerEvent> emittedPayload = ArgumentCaptor.forClass(ClusterUpgradeValidationTriggerEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), emittedPayload.capture())).thenReturn(event);

        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(context, triggerEvent, variables);

        verify(stackUpdater).updateStackStatus(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_VALIDATION_STARTED, "started");
        verify(cloudbreakEventService).fireCloudbreakEvent(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_UPGRADE_VALIDATION_STARTED);
        verify(eventBus).notify(eq(RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT.event()), eq(event));

        assertThat(emittedPayload.getValue()).isSameAs(triggerEvent);
        verifyNoInteractions(clusterUpgradePropertiesResolver);
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, "clusterUpgradePropertiesResolver", clusterUpgradePropertiesResolver);
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    @Test
    void imageValidationActionPassesPropertiesWithoutReconstructingCatalogImage() throws Exception {
        AbstractClusterUpgradeValidationAction<ClusterUpgradeS3guardValidationFinishedEvent> action =
                (AbstractClusterUpgradeValidationAction<ClusterUpgradeS3guardValidationFinishedEvent>) underTest.clusterUpgradeImageValidation();
        initActionPrivateFields(action);
        ClusterUpgradePropertiesResolver resolver = mock(ClusterUpgradePropertiesResolver.class);
        ReflectionTestUtils.setField(action, "clusterUpgradePropertiesResolver", resolver);
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                TARGET_RUNTIME_VERSION, "base-image", OsType.RHEL8, "x86_64", null, Set.of(), null);
        ClusterUpgradeS3guardValidationFinishedEvent payload =
                new ClusterUpgradeS3guardValidationFinishedEvent(STACK_ID, properties.getTargetImageId(), properties);
        when(resolver.resolve(payload)).thenReturn(properties);
        context = new StackContext(flowParameters, stackDto, null, null, CloudStack.builder().build());
        ArgumentCaptor<ClusterUpgradeImageValidationEvent> emittedPayload = ArgumentCaptor.forClass(ClusterUpgradeImageValidationEvent.class);
        Event event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), emittedPayload.capture())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, new HashMap<>());

        assertThat(emittedPayload.getValue().getTargetImage()).isNull();
        assertThat(emittedPayload.getValue().getClusterUpgradeProperties()).isSameAs(properties);
        assertThat(emittedPayload.getValue().getCloudStack().getImage().getImageId()).isEqualTo(properties.getTargetImageId());
    }
}
