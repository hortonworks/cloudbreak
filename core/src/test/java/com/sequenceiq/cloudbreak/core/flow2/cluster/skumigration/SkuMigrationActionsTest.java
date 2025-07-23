package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CHECK_LOAD_BALANCERS_SKU;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.SKU_MIGRATION_FINISHED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check.CheckSkuRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.model.ProviderSyncState;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class SkuMigrationActionsTest {

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private StackService stackService;

    @Mock
    private SkuMigrationService skuMigrationService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private SkuMigrationActions skuMigrationActions;

    private SkuMigrationContext context;

    @Test
    public void checkSkuActionIfBasicSkuMigrationNeeded() throws Exception {
        context = new SkuMigrationContext(flowParameters, mock(StackView.class), "AZURE", mock(CloudContext.class), mock(CloudCredential.class),
                mock(CloudConnector.class), mock(CloudStack.class), Set.of(ProviderSyncState.BASIC_SKU_MIGRATION_NEEDED));
        AbstractSkuMigrationAction<SkuMigrationTriggerEvent> action =
                (AbstractSkuMigrationAction<SkuMigrationTriggerEvent>) skuMigrationActions.checkSkuAction();
        initActionPrivateFields(action);
        SkuMigrationTriggerEvent skuMigrationTriggerEvent = new SkuMigrationTriggerEvent(SKU_MIGRATION_EVENT.event(), 1L, false);
        Event event = mock(Event.class);
        ArgumentCaptor<CheckSkuRequest> checkSkuRequestArgumentCaptor = ArgumentCaptor.forClass(CheckSkuRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), checkSkuRequestArgumentCaptor.capture())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, skuMigrationTriggerEvent, Map.of());

        verify(stackUpdater).updateStackStatus(skuMigrationTriggerEvent.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
        verify(flowMessageService).fireEventAndLog(skuMigrationTriggerEvent.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), CHECK_LOAD_BALANCERS_SKU);

        verify(eventBus).notify(eq("CHECKSKUREQUEST"), eq(event));
        CheckSkuRequest sentCheckSkuRequest = checkSkuRequestArgumentCaptor.getValue();
        assertTrue(sentCheckSkuRequest.isForce());
    }

    @Test
    public void checkSkuActionIfForceTrue() throws Exception {
        context = new SkuMigrationContext(flowParameters, mock(StackView.class), "AZURE", mock(CloudContext.class), mock(CloudCredential.class),
                mock(CloudConnector.class), mock(CloudStack.class), Set.of());
        AbstractSkuMigrationAction<SkuMigrationTriggerEvent> action =
                (AbstractSkuMigrationAction<SkuMigrationTriggerEvent>) skuMigrationActions.checkSkuAction();
        initActionPrivateFields(action);
        SkuMigrationTriggerEvent skuMigrationTriggerEvent = new SkuMigrationTriggerEvent(SKU_MIGRATION_EVENT.event(), 1L, true);
        Event event = mock(Event.class);
        ArgumentCaptor<CheckSkuRequest> checkSkuRequestArgumentCaptor = ArgumentCaptor.forClass(CheckSkuRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), checkSkuRequestArgumentCaptor.capture())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, skuMigrationTriggerEvent, Map.of());

        verify(stackUpdater).updateStackStatus(skuMigrationTriggerEvent.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
        verify(flowMessageService).fireEventAndLog(skuMigrationTriggerEvent.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), CHECK_LOAD_BALANCERS_SKU);

        verify(eventBus).notify(eq("CHECKSKUREQUEST"), eq(event));
        CheckSkuRequest sentCheckSkuRequest = checkSkuRequestArgumentCaptor.getValue();
        assertTrue(sentCheckSkuRequest.isForce());
    }

    @Test
    public void checkSkuActionIfForceFalse() throws Exception {
        context = new SkuMigrationContext(flowParameters, mock(StackView.class), "AZURE", mock(CloudContext.class), mock(CloudCredential.class),
                mock(CloudConnector.class), mock(CloudStack.class), Set.of());
        AbstractSkuMigrationAction<SkuMigrationTriggerEvent> action =
                (AbstractSkuMigrationAction<SkuMigrationTriggerEvent>) skuMigrationActions.checkSkuAction();
        initActionPrivateFields(action);
        SkuMigrationTriggerEvent skuMigrationTriggerEvent = new SkuMigrationTriggerEvent(SKU_MIGRATION_EVENT.event(), 1L, false);
        Event event = mock(Event.class);
        ArgumentCaptor<CheckSkuRequest> checkSkuRequestArgumentCaptor = ArgumentCaptor.forClass(CheckSkuRequest.class);
        when(reactorEventFactory.createEvent(anyMap(), checkSkuRequestArgumentCaptor.capture())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, skuMigrationTriggerEvent, Map.of());

        verify(stackUpdater).updateStackStatus(skuMigrationTriggerEvent.getResourceId(), DetailedStackStatus.MIGRATING_SKU);
        verify(flowMessageService).fireEventAndLog(skuMigrationTriggerEvent.getResourceId(), Status.UPDATE_IN_PROGRESS.name(), CHECK_LOAD_BALANCERS_SKU);

        verify(eventBus).notify(eq("CHECKSKUREQUEST"), eq(event));
        CheckSkuRequest sentCheckSkuRequest = checkSkuRequestArgumentCaptor.getValue();
        assertFalse(sentCheckSkuRequest.isForce());
    }

    @Test
    public void skuMigrationFinishedActionTest() throws Exception {
        context = new SkuMigrationContext(flowParameters, mock(StackView.class), "AZURE", mock(CloudContext.class), mock(CloudCredential.class),
                mock(CloudConnector.class), mock(CloudStack.class), Set.of());
        AbstractSkuMigrationAction<SkuMigrationFinished> action =
                (AbstractSkuMigrationAction<SkuMigrationFinished>) skuMigrationActions.skuMigrationFinishedAction();
        initActionPrivateFields(action);
        SkuMigrationFinished skuMigrationFinished = new SkuMigrationFinished(1L);
        Event event = mock(Event.class);
        ArgumentCaptor<StackEvent> stackEvent = ArgumentCaptor.forClass(StackEvent.class);
        when(reactorEventFactory.createEvent(anyMap(), stackEvent.capture())).thenReturn(event);

        new AbstractActionTestSupport<>(action).doExecute(context, skuMigrationFinished, Map.of());

        verify(stackUpdater).updateStackStatus(skuMigrationFinished.getResourceId(), DetailedStackStatus.AVAILABLE);
        verify(flowMessageService).fireEventAndLog(skuMigrationFinished.getResourceId(), Status.AVAILABLE.name(), SKU_MIGRATION_FINISHED);

        verify(eventBus).notify(eq("SKU_MIGRATION_FINALIZED_EVENT"), eq(event));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}