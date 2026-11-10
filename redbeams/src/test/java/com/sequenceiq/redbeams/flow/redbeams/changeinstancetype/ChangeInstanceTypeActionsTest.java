package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_START_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FINALIZE_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerRequest;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeValidationFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@ExtendWith(MockitoExtension.class)
class ChangeInstanceTypeActionsTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String CURRENT_INSTANCE_TYPE = "db.m5.large";

    private static final String TARGET_INSTANCE_TYPE = "db.m5.xlarge";

    private final CloudContext cloudContext = CloudContext.Builder.builder().withId(RESOURCE_ID).withPlatform("AWS").withVariant("AWS").build();

    private final CloudCredential cloudCredential = new CloudCredential("id", "name", "account");

    private final DatabaseStack databaseStack = new DatabaseStack(null, DatabaseServer.builder().withServerId("dbserver").build(), Map.of(), "");

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private DBStackUpdater dbStackUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private DBStack dbStack;

    @Captor
    private ArgumentCaptor<Object> payloadCaptor;

    @InjectMocks
    private ChangeInstanceTypeActions underTest;

    private RedbeamsContext context;

    @BeforeEach
    void setUp() {
        context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    @Test
    void testSyncSetsInProgressStatusAndSyncsTheLiveState() throws Exception {
        execute(underTest.syncInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_START_EVENT, null));

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_IN_PROGRESS);
        assertHandlerRequest(SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER, null);
    }

    @Test
    void testValidationRunsWhenTheTargetIsNotApplied() throws Exception {
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);

        execute(underTest.validateChangeInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT, liveState));

        assertHandlerRequest(VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER, liveState);
    }

    @Test
    void testValidationRunsWhenTheTargetIsReportedButAModificationIsInProgress() throws Exception {
        DatabaseServerInstanceTypeState liveState = state(TARGET_INSTANCE_TYPE, true);

        execute(underTest.validateChangeInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT, liveState));

        assertHandlerRequest(VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER, liveState);
    }

    @Test
    void testAlreadyAppliedTargetSkipsToTheMetadataUpdate() throws Exception {
        DatabaseServerInstanceTypeState liveState = state(TARGET_INSTANCE_TYPE, false);

        execute(underTest.validateChangeInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_SYNC_FINISHED_EVENT, liveState));

        verify(reactorEventFactory).createEvent(anyMap(), payloadCaptor.capture());
        ChangeInstanceTypeEvent sentEvent = assertInstanceOf(ChangeInstanceTypeEvent.class, payloadCaptor.getValue());
        assertEquals(CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT.event(), sentEvent.selector());
        assertEquals(TARGET_INSTANCE_TYPE, sentEvent.getTargetInstanceType());
        assertSame(liveState, sentEvent.getLiveState());
    }

    @Test
    void testModifyIssuesTheModification() throws Exception {
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, false);

        execute(underTest.changeInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_VALIDATION_FINISHED_EVENT, liveState));

        assertHandlerRequest(CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER, liveState);
    }

    @Test
    void testWaitPollsTheModification() throws Exception {
        DatabaseServerInstanceTypeState liveState = state(CURRENT_INSTANCE_TYPE, true);

        execute(underTest.waitForChangeInstanceTypeAction(), event(CHANGE_INSTANCE_TYPE_MODIFY_FINISHED_EVENT, liveState));

        assertHandlerRequest(WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER, liveState);
    }

    @Test
    void testFinishedPersistsTheLiveValuesInsteadOfTheRequestedOnes() throws Exception {
        DatabaseServerInstanceTypeState liveState = new DatabaseServerInstanceTypeState("db.m5.2xlarge", 250L, true, false);
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_COMPLETED)).thenReturn(Optional.of(dbStack));

        execute(underTest.changeInstanceTypeFinishedAction(), event(CHANGE_INSTANCE_TYPE_WAIT_FINISHED_EVENT, liveState));

        verify(dbStackUpdater).updateInstanceTypeAndStorageSize(RESOURCE_ID, "db.m5.2xlarge", 250L);
        verify(metricService).incrementMetricCounter(MetricType.DB_CHANGE_INSTANCE_TYPE_FINISHED, Optional.of(dbStack));
        verify(reactorEventFactory).createEvent(anyMap(), payloadCaptor.capture());
        RedbeamsEvent sentEvent = assertInstanceOf(RedbeamsEvent.class, payloadCaptor.getValue());
        assertEquals(FINALIZE_CHANGE_INSTANCE_TYPE_EVENT.event(), sentEvent.selector());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testFailedSetsTheFailedStatusUnlessCancelled(boolean cancelled) throws Exception {
        RedbeamsFailureEvent payload = new RedbeamsFailureEvent(RESOURCE_ID, cancelled ? new CancellationException("cancelled") : new Exception("failure"));
        AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> action =
                (AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>) underTest.changeInstanceTypeFailedAction();

        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        if (cancelled) {
            verifyNoInteractions(dbStackStatusUpdater, metricService);
        } else {
            verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_FAILED, "failure");
            verify(metricService).incrementMetricCounter(eq(MetricType.DB_CHANGE_INSTANCE_TYPE_FAILED), any(Optional.class));
        }
    }

    @Test
    void testFailedValidationKeepsTheDatabaseServerAvailable() throws Exception {
        RedbeamsFailureEvent payload = new ChangeInstanceTypeValidationFailedEvent(RESOURCE_ID, new BadRequestException("invalid instance type"));
        AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> action =
                (AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>) underTest.changeInstanceTypeFailedAction();

        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED, "invalid instance type");
        verify(metricService).incrementMetricCounter(eq(MetricType.DB_CHANGE_INSTANCE_TYPE_FAILED), any(Optional.class));
    }

    @Test
    void testFailedStatusIsDecidedByTheSelectorNotTheExceptionType() throws Exception {
        RedbeamsFailureEvent payload = new RedbeamsFailureEvent(RESOURCE_ID, new BadRequestException("invalid"));
        AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> action =
                (AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>) underTest.changeInstanceTypeFailedAction();

        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_FAILED, "invalid");
    }

    @Test
    void testValidationFailedStatusIsAvailable() {
        assertEquals(Status.AVAILABLE, DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED.getStatus());
    }

    @Test
    void testFailedSendsTheFailureHandledEvent() throws Exception {
        AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors> action =
                (AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>) underTest.changeInstanceTypeFailedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, new RedbeamsFailureEvent(RESOURCE_ID, new Exception("failure")), new HashMap<>());

        verify(reactorEventFactory).createEvent(anyMap(), payloadCaptor.capture());
        RedbeamsEvent sentEvent = assertInstanceOf(RedbeamsEvent.class, payloadCaptor.getValue());
        assertEquals(HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT.event(), sentEvent.selector());
        assertEquals(RESOURCE_ID, sentEvent.getResourceId());
    }

    private void execute(Action<?, ?> action, ChangeInstanceTypeEvent payload) throws Exception {
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>((AbstractChangeInstanceTypeAction<ChangeInstanceTypeEvent>) action).doExecute(context, payload, new HashMap<>());
    }

    private void assertHandlerRequest(ChangeInstanceTypeHandlerSelectors expectedSelector, DatabaseServerInstanceTypeState expectedLiveState) {
        verify(reactorEventFactory).createEvent(anyMap(), payloadCaptor.capture());
        ChangeInstanceTypeHandlerRequest request = assertInstanceOf(ChangeInstanceTypeHandlerRequest.class, payloadCaptor.getValue());
        assertEquals(expectedSelector.event(), request.selector());
        assertEquals(RESOURCE_ID, request.getResourceId());
        assertEquals(TARGET_INSTANCE_TYPE, request.getTargetInstanceType());
        assertSame(expectedLiveState, request.getLiveState());
        assertSame(cloudContext, request.getCloudContext());
        assertSame(cloudCredential, request.getCloudCredential());
        assertSame(databaseStack, request.getDatabaseStack());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
    }

    private static ChangeInstanceTypeEvent event(ChangeInstanceTypeStateSelectors selector, DatabaseServerInstanceTypeState liveState) {
        return new ChangeInstanceTypeEvent(selector.event(), RESOURCE_ID, TARGET_INSTANCE_TYPE, liveState);
    }

    private static DatabaseServerInstanceTypeState state(String instanceType, boolean modificationInProgress) {
        return new DatabaseServerInstanceTypeState(instanceType, 100L, false, modificationInProgress);
    }
}
