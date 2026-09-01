package com.sequenceiq.freeipa.flow.freeipa.migration;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.cloud.model.Variant.variant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.migration.action.AbstractMultiAzMigrationInitAction;
import com.sequenceiq.freeipa.flow.freeipa.migration.action.MultiAzMigrationInitActions;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbDnsUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbMetadataCollectionResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbUpdateResult;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationLbWaitResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationInitActionsTest {

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
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

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
    private MultiAzMigrationInitActions underTest;

    private FlowParameters flowParameters;

    private StackContext context;

    @Mock
    private StateContext<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent> stateContext;

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
        MultiAzMigrationInitTriggerEvent payload =
                new MultiAzMigrationInitTriggerEvent(MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_EVENT.event(), STACK_ID, OPERATION_ID);
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

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitTriggerEvent> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitTriggerEvent>) underTest.multiAzMigrationInitAction();
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
    void testMultiAzMigrationInitAction() throws Exception {
        MultiAzMigrationInitTriggerEvent payload =
                new MultiAzMigrationInitTriggerEvent(MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_EVENT.event(), STACK_ID, OPERATION_ID);
        Map<Object, Object> variables = mock();
        ArgumentCaptor<MultiAzMigrationInitHandlerRequest> requestCaptor = ArgumentCaptor.forClass(MultiAzMigrationInitHandlerRequest.class);
        Event<MultiAzMigrationInitHandlerRequest> event = mock();
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationInitHandlerRequest.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitTriggerEvent> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitTriggerEvent>) underTest.multiAzMigrationInitAction();
        initActionPrivateFields(action);

        AbstractActionTestSupport<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent, StackContext, MultiAzMigrationInitTriggerEvent>
                abstractActionTestSupport = new AbstractActionTestSupport<>(action);
        abstractActionTestSupport.prepareExecution(payload, variables);
        abstractActionTestSupport.doExecute(context, payload, variables);

        verify(variables).put(OperationAwareAction.OPERATION_ID, OPERATION_ID);
        verify(stackUpdater).updateStackStatus(stack, DetailedStackStatus.MULTI_AZ_MIGRATION_IN_PROGRESS, "Starting FreeIPA multi-AZ migration.");
        verify(eventSenderService).sendEventAndNotification(stack, USER_CRN, ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_STARTED);
        verify(reactorEventFactory).createEvent(any(), requestCaptor.capture());
        MultiAzMigrationInitHandlerRequest request = requestCaptor.getValue();
        assertEquals(STACK_ID, request.getResourceId());
        assertEquals(OPERATION_ID, request.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONINITHANDLERREQUEST", event);
    }

    @Test
    void testMultiAzMigrationLbUpdateActionWhenAwsAndLoadBalancerExists() throws Exception {
        MultiAzMigrationInitResult payload = new MultiAzMigrationInitResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        Map<Object, Object> variables = mock();
        when(variables.getOrDefault(AbstractMultiAzMigrationInitAction.HAS_LOAD_BALANCER, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        Event<MultiAzMigrationLbUpdateHandlerRequest> event = mock();
        ArgumentCaptor<MultiAzMigrationLbUpdateHandlerRequest> requestCaptor =
                ArgumentCaptor.forClass(MultiAzMigrationLbUpdateHandlerRequest.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbUpdateHandlerRequest.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult>) underTest.multiAzMigrationLbUpdateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), requestCaptor.capture());
        MultiAzMigrationLbUpdateHandlerRequest request = requestCaptor.getValue();
        assertEquals(STACK_ID, request.getResourceId());
        assertEquals(OPERATION_ID, request.getOperationId());
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        verify(eventBus).notify("MULTIAZMIGRATIONLBUPDATEHANDLERREQUEST", event);
    }

    @Test
    void testMultiAzMigrationLbUpdateActionWhenNotAwsSkipsUpdate() throws Exception {
        MultiAzMigrationInitResult payload = new MultiAzMigrationInitResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        Map<Object, Object> variables = mock();
        Event<MultiAzMigrationLbUpdateResult> event = mock();
        ArgumentCaptor<MultiAzMigrationLbUpdateResult> resultCaptor = ArgumentCaptor.forClass(MultiAzMigrationLbUpdateResult.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbUpdateResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult>) underTest.multiAzMigrationLbUpdateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), resultCaptor.capture());
        MultiAzMigrationLbUpdateResult result = resultCaptor.getValue();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBUPDATERESULT", event);
    }

    @Test
    void testMultiAzMigrationLbUpdateActionWhenAwsButNoLoadBalancerSkipsUpdate() throws Exception {
        MultiAzMigrationInitResult payload = new MultiAzMigrationInitResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        Map<Object, Object> variables = mock();
        Event<MultiAzMigrationLbUpdateResult> event = mock();
        ArgumentCaptor<MultiAzMigrationLbUpdateResult> resultCaptor = ArgumentCaptor.forClass(MultiAzMigrationLbUpdateResult.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbUpdateResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitResult>) underTest.multiAzMigrationLbUpdateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), resultCaptor.capture());
        MultiAzMigrationLbUpdateResult result = resultCaptor.getValue();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBUPDATERESULT", event);
    }

    @Test
    void testMultiAzMigrationLbWaitActionWhenAwsAndLoadBalancerExists() throws Exception {
        MultiAzMigrationLbUpdateResult payload = new MultiAzMigrationLbUpdateResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        Map<Object, Object> variables = mock();
        when(variables.getOrDefault(AbstractMultiAzMigrationInitAction.HAS_LOAD_BALANCER, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        Event<MultiAzMigrationLbWaitHandlerRequest> event = mock();
        ArgumentCaptor<MultiAzMigrationLbWaitHandlerRequest> requestCaptor =
                ArgumentCaptor.forClass(MultiAzMigrationLbWaitHandlerRequest.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbWaitHandlerRequest.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbUpdateResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbUpdateResult>) underTest.multiAzMigrationLbWaitAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), requestCaptor.capture());
        MultiAzMigrationLbWaitHandlerRequest request = requestCaptor.getValue();
        assertEquals(STACK_ID, request.getResourceId());
        assertEquals(OPERATION_ID, request.getOperationId());
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        verify(eventBus).notify("MULTIAZMIGRATIONLBWAITHANDLERREQUEST", event);
    }

    @Test
    void testMultiAzMigrationLbWaitActionWhenNotAwsSkipsWait() throws Exception {
        MultiAzMigrationLbUpdateResult payload = new MultiAzMigrationLbUpdateResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        Map<Object, Object> variables = mock();
        Event<MultiAzMigrationLbWaitResult> event = mock();
        ArgumentCaptor<MultiAzMigrationLbWaitResult> resultCaptor = ArgumentCaptor.forClass(MultiAzMigrationLbWaitResult.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbWaitResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbUpdateResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbUpdateResult>) underTest.multiAzMigrationLbWaitAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), resultCaptor.capture());
        MultiAzMigrationLbWaitResult result = resultCaptor.getValue();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBWAITRESULT", event);
    }

    @Test
    void testMultiAzMigrationLbMetadataCollectionActionWhenAwsAndLoadBalancerExists() throws Exception {
        MultiAzMigrationLbWaitResult payload = new MultiAzMigrationLbWaitResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        Map<Object, Object> variables = mock();
        when(variables.getOrDefault(AbstractMultiAzMigrationInitAction.HAS_LOAD_BALANCER, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        Event<MultiAzMigrationLbMetadataCollectionHandlerRequest> event = mock();
        ArgumentCaptor<MultiAzMigrationLbMetadataCollectionHandlerRequest> requestCaptor =
                ArgumentCaptor.forClass(MultiAzMigrationLbMetadataCollectionHandlerRequest.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbMetadataCollectionHandlerRequest.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbWaitResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbWaitResult>) underTest.multiAzMigrationLbMetadataCollectionAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), requestCaptor.capture());
        MultiAzMigrationLbMetadataCollectionHandlerRequest request = requestCaptor.getValue();
        assertEquals(STACK_ID, request.getResourceId());
        assertEquals(OPERATION_ID, request.getOperationId());
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        verify(eventBus).notify("MULTIAZMIGRATIONLBMETADATACOLLECTIONHANDLERREQUEST", event);
    }

    @Test
    void testMultiAzMigrationLbMetadataCollectionActionWhenNotAwsSkipsCollection() throws Exception {
        MultiAzMigrationLbWaitResult payload = new MultiAzMigrationLbWaitResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        Map<Object, Object> variables = mock();
        Event<MultiAzMigrationLbMetadataCollectionResult> event = mock();
        ArgumentCaptor<MultiAzMigrationLbMetadataCollectionResult> resultCaptor =
                ArgumentCaptor.forClass(MultiAzMigrationLbMetadataCollectionResult.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbMetadataCollectionResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbWaitResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbWaitResult>) underTest.multiAzMigrationLbMetadataCollectionAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), resultCaptor.capture());
        MultiAzMigrationLbMetadataCollectionResult result = resultCaptor.getValue();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBMETADATACOLLECTIONRESULT", event);
    }

    @Test
    void testMultiAzMigrationLbDnsUpdateActionWhenAwsAndLoadBalancerExists() throws Exception {
        MultiAzMigrationLbMetadataCollectionResult payload = new MultiAzMigrationLbMetadataCollectionResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        Map<Object, Object> variables = mock();
        when(variables.getOrDefault(AbstractMultiAzMigrationInitAction.HAS_LOAD_BALANCER, Boolean.FALSE)).thenReturn(Boolean.TRUE);
        Event<MultiAzMigrationLbDnsUpdateHandlerRequest> event = mock();
        ArgumentCaptor<MultiAzMigrationLbDnsUpdateHandlerRequest> requestCaptor = ArgumentCaptor.forClass(MultiAzMigrationLbDnsUpdateHandlerRequest.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbDnsUpdateHandlerRequest.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbMetadataCollectionResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbMetadataCollectionResult>) underTest.multiAzMigrationLbDnsUpdateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), requestCaptor.capture());
        MultiAzMigrationLbDnsUpdateHandlerRequest request = requestCaptor.getValue();
        assertEquals(STACK_ID, request.getResourceId());
        assertEquals(OPERATION_ID, request.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBDNSUPDATEHANDLERREQUEST", event);
    }

    @Test
    void testMultiAzMigrationLbDnsUpdateActionWhenNotAwsSkipsDnsUpdate() throws Exception {
        MultiAzMigrationLbMetadataCollectionResult payload = new MultiAzMigrationLbMetadataCollectionResult(STACK_ID, OPERATION_ID);
        when(stack.getCloudPlatform()).thenReturn("AZURE");
        Map<Object, Object> variables = mock();
        Event<MultiAzMigrationLbDnsUpdateResult> event = mock();
        ArgumentCaptor<MultiAzMigrationLbDnsUpdateResult> resultCaptor = ArgumentCaptor.forClass(MultiAzMigrationLbDnsUpdateResult.class);
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbDnsUpdateResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbMetadataCollectionResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbMetadataCollectionResult>) underTest.multiAzMigrationLbDnsUpdateAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(reactorEventFactory).createEvent(any(), resultCaptor.capture());
        MultiAzMigrationLbDnsUpdateResult result = resultCaptor.getValue();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
        verify(eventBus).notify("MULTIAZMIGRATIONLBDNSUPDATERESULT", event);
    }

    @Test
    void testMultiAzMigrationInitFinishedAction() throws Exception {
        MultiAzMigrationLbDnsUpdateResult payload = new MultiAzMigrationLbDnsUpdateResult(STACK_ID, OPERATION_ID);
        Event<MultiAzMigrationLbDnsUpdateResult> event = mock();
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationLbDnsUpdateResult.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationLbDnsUpdateResult> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationLbDnsUpdateResult>) underTest.multiAzMigrationInitFinishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of());

        verify(reactorEventFactory).createEvent(any(), eq(payload));
        verify(eventBus).notify(MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FINISHED_EVENT.event(), event);
    }

    @Test
    void testMultiAzMigrationInitFailedAction() throws Exception {
        MultiAzMigrationInitFailedEvent payload = new MultiAzMigrationInitFailedEvent(STACK_ID, new RuntimeException("something went wrong"));
        Event<MultiAzMigrationInitFailedEvent> event = mock();
        when(reactorEventFactory.createEvent(any(), any(MultiAzMigrationInitFailedEvent.class))).thenReturn(event);

        AbstractMultiAzMigrationInitAction<MultiAzMigrationInitFailedEvent> action =
                (AbstractMultiAzMigrationInitAction<MultiAzMigrationInitFailedEvent>) underTest.multiAzMigrationInitFailedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, Map.of(OperationAwareAction.OPERATION_ID, OPERATION_ID));

        verify(operationService).failOperation(ACCOUNT_ID, OPERATION_ID, "FreeIPA multi-AZ migration initialization failed: something went wrong");
        verify(reactorEventFactory).createEvent(any(), eq(payload));
        verify(eventBus).notify(MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FAIL_HANDLED_EVENT.event(), event);
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
