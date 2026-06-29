package com.sequenceiq.freeipa.flow.freeipa.migration;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.migration.action.AbstractMultiAzMigrationFinalizeAction;
import com.sequenceiq.freeipa.flow.freeipa.migration.action.MultiAzMigrationFinalizeActions;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationFinalizeActionsTest {

    private static final String FLOW_ID = "flowId";

    private static final String USER_CRN = "userCrn";

    private static final String ACCOUNT_ID = "accountId";

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "operationId";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private OperationService operationService;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FreeipaJobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @InjectMocks
    private MultiAzMigrationFinalizeActions underTest;

    private FlowParameters flowParameters;

    private StackContext context;

    @Mock
    private StateContext<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent> stateContext;

    @Mock
    private Stack stack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @BeforeEach
    void setUp() {
        flowParameters = new FlowParameters(FLOW_ID, USER_CRN);
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);

        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void testAbstractMultiAzMigrationFinalizeActionCreateFlowContext() {
        MultiAzMigrationFinalizeTriggerEvent payload =
                new MultiAzMigrationFinalizeTriggerEvent(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_EVENT.event(),
                        STACK_ID, OPERATION_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        ExtendedState extendedState = mock();
        when(extendedState.getVariables()).thenReturn(new HashMap<>(Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID)));
        when(stateContext.getExtendedState()).thenReturn(extendedState);
        Credential credential = mock();
        when(credentialService.getCredentialByEnvCrn(ENV_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(cloudStackConverter.convert(stack)).thenReturn(cloudStack);
        when(stack.getRegion()).thenReturn("region");
        when(stack.getAvailabilityZone()).thenReturn("availabilityZone");
        when(stack.getName()).thenReturn("name");
        when(stack.getResourceCrn()).thenReturn("crn");
        when(stack.getCloudPlatform()).thenReturn("platform");
        when(stack.getPlatformvariant()).thenReturn("variant");
        when(stack.getOwner()).thenReturn("owner");

        AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeTriggerEvent> action =
                (AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeTriggerEvent>) underTest.multiAzMigrationFinalizeAction();
        initActionPrivateFields(action);

        StackContext result = new AbstractActionTestSupport<>(action).createFlowContext(flowParameters, stateContext, payload);

        assertEquals(flowParameters, result.getFlowParameters());
        assertEquals(stack, result.getStack());
        assertEquals(cloudCredential, result.getCloudCredential());
        assertEquals(cloudStack, result.getCloudStack());
        CloudContext resultCloudContext = result.getCloudContext();
        assertEquals(STACK_ID, resultCloudContext.getId());
        assertEquals("name", resultCloudContext.getName());
        assertEquals("crn", resultCloudContext.getCrn());
        assertEquals(platform("platform"), resultCloudContext.getPlatform());
        assertEquals(variant("variant"), resultCloudContext.getVariant());
        assertEquals(location(region("region"), availabilityZone("availabilityZone")), resultCloudContext.getLocation());
        assertEquals("owner", resultCloudContext.getUserName());
        assertEquals(ACCOUNT_ID, resultCloudContext.getAccountId());
    }

    @Test
    void testMultiAzMigrationFinalizeAction() throws Exception {
        MultiAzMigrationFinalizeTriggerEvent payload =
                new MultiAzMigrationFinalizeTriggerEvent(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_EVENT.event(),
                        STACK_ID, OPERATION_ID);
        Map<Object, Object> variables = mock();
        ArgumentCaptor<Set<SuccessDetails>> successDetailsSetCaptor = ArgumentCaptor.forClass(Set.class);
        Event<MultiAzMigrationFinalizeTriggerEvent> event = mock();
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationFinalizeTriggerEvent.class))).thenReturn(event);

        AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeTriggerEvent> action =
                (AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeTriggerEvent>) underTest.multiAzMigrationFinalizeAction();
        initActionPrivateFields(action);

        AbstractActionTestSupport<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent, StackContext, MultiAzMigrationFinalizeTriggerEvent>
                abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        abstractActionTestSupport.prepareExecution(payload, variables);
        abstractActionTestSupport.doExecute(context, payload, variables);

        verify(variables).put(OperationAwareAction.OPERATION_ID, OPERATION_ID);
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.UPDATE_COMPLETE, "FreeIPA multi-AZ migration completed successfully.");
        verify(operationService).completeOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), successDetailsSetCaptor.capture(), eq(Set.of()));
        Set<SuccessDetails> successDetails = successDetailsSetCaptor.getValue();
        assertThat(successDetails)
                .hasSize(1)
                .extracting(SuccessDetails::getEnvironment).containsExactly(ENV_CRN);
        verify(eventSenderService).sendEventAndNotification(stack, USER_CRN, ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FINISHED);
        verify(jobService).schedule(STACK_ID);
        verify(reactorEventFactory).createEvent(any(), eq(payload));
        verify(eventBus).notify(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FINISHED_EVENT.event(), event);
    }

    @Test
    void testMultiAzMigrationFinalizeFailedAction() throws Exception {
        MultiAzMigrationFinalizeFailedEvent payload = new MultiAzMigrationFinalizeFailedEvent(STACK_ID, new RuntimeException("something went wrong"));
        Event<MultiAzMigrationFinalizeFailedEvent> event = mock();
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationFinalizeFailedEvent.class))).thenReturn(event);

        AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeFailedEvent> action =
                (AbstractMultiAzMigrationFinalizeAction<MultiAzMigrationFinalizeFailedEvent>) underTest.multiAzMigrationFinalizeFailedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID));

        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID, "FreeIPA multi-AZ migration finalization failed: something went wrong");
        verify(reactorEventFactory).createEvent(any(), eq(payload));
        verify(eventBus).notify(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FAIL_HANDLED_EVENT.event(), event);
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, stackUpdater, StackUpdater.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, null, jobService, FreeipaJobService.class);
        ReflectionTestUtils.setField(action, null, stackService, StackService.class);
        ReflectionTestUtils.setField(action, null, credentialConverter, CredentialToCloudCredentialConverter.class);
        ReflectionTestUtils.setField(action, null, credentialService, CredentialService.class);
        ReflectionTestUtils.setField(action, null, cloudStackConverter, StackToCloudStackConverter.class);
    }
}
