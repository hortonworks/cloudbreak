package com.sequenceiq.datalake.flow.dr.restore;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreAwaitServicesStoppedRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.sdx.api.model.SdxBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class DatalakeRestoreActionsTest {

    private static final Long OLD_SDX_ID = 1L;

    private static final Long NEW_SDX_ID = 2L;

    private static final String FLOW_ID = "flow_id";

    private static final String DATALAKE_NAME = "test_dl";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3://cloudbreak-bucket/backup-location";

    private static final String BACKUP_ID = "backup_id";

    private static final String RESTORE_ID = "restore_id";

    private static final String START_TIMESTAMP = "2024-10-02T19:09:31.000653+00:00";

    private static final String END_TIMESTAMP  = "2024-10-02T19:15:12.000060+00:00";

    private static final DatalakeDrSkipOptions SKIP_OPTIONS = new DatalakeDrSkipOptions(false, false, false, false);

    @InjectMocks
    private final DatalakeRestoreActions underTest = new DatalakeRestoreActions();

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxMetricService metricService;

    @Test
    public void testRestoreWithNoFlowChain() throws Exception {
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(OLD_SDX_ID), any(), any(), eq(USER_CRN), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), OLD_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
        when(sdxBackupRestoreService.getDatalakeBackupStatus(any(), eq(BACKUP_ID), any(), eq(USER_CRN)))
                .thenReturn(new SdxBackupStatusResponse(RESTORE_ID, "", "", Collections.singletonList(""), START_TIMESTAMP, END_TIMESTAMP));
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<DatalakeDatabaseRestoreStartEvent> captor = ArgumentCaptor.forClass(DatalakeDatabaseRestoreStartEvent.class);

        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeDatabaseRestoreStartEvent captorValue = captor.getValue();
        assertEquals(OLD_SDX_ID, captorValue.getResourceId());
    }

    @Test
    public void testGetNewSdxIdForResizeCreateFlowContext() throws Exception {

        SdxCluster sdxCluster = genCluster();
        FlowParameters flowParameters = new FlowParameters(FLOW_ID, null);
        when(sdxService.getByNameInAccount(eq(USER_CRN), eq(DATALAKE_NAME))).thenReturn(sdxCluster);
        when(flowLogService.getLastFlowLog(anyString())).thenReturn(Optional.of(mock(FlowLogWithoutPayload.class)));
        when(flowChainLogService.isFlowTriggeredByFlowChain(eq(DatalakeResizeFlowEventChainFactory.class.getSimpleName()), any())).thenReturn(true);

        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), OLD_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = (SdxContext) testSupport.createFlowContext(flowParameters, null, event);

        assertEquals(NEW_SDX_ID, context.getSdxId());
        assertEquals(USER_CRN, context.getUserId());
        assertEquals(FLOW_ID, context.getFlowId());
        assertEquals(NEW_SDX_ID, event.getDrStatus().getSdxClusterId());

        when(flowChainLogService.isFlowTriggeredByFlowChain(eq(DatalakeResizeFlowEventChainFactory.class.getSimpleName()), any())).thenReturn(false);
        context = (SdxContext) testSupport.createFlowContext(flowParameters, null, event);
        assertEquals(OLD_SDX_ID, context.getSdxId());
    }

    @Test
    public void testSkipOptionsPassedIntoRestoreCall() throws Exception {
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(true, true, true, true);

        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(skipOptions), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, skipOptions, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
        when(sdxBackupRestoreService.getDatalakeBackupStatus(any(), eq(BACKUP_ID), any(), eq(USER_CRN)))
                .thenReturn(new SdxBackupStatusResponse(RESTORE_ID, "", "", Collections.singletonList(""), START_TIMESTAMP, END_TIMESTAMP));
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<DatalakeDatabaseRestoreStartEvent> captor = ArgumentCaptor.forClass(DatalakeDatabaseRestoreStartEvent.class);

        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeDatabaseRestoreStartEvent captorValue = captor.getValue();
        assertEquals(NEW_SDX_ID, captorValue.getResourceId());
    }

    @Test
    public void testFullDrMaxDurationPassedIntoRestoreCall() throws Exception {
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), any(DatalakeDrSkipOptions.class), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE,
                0, false);
        when(sdxBackupRestoreService.getDatalakeBackupStatus(any(), eq(BACKUP_ID), any(), eq(USER_CRN)))
                .thenReturn(new SdxBackupStatusResponse(RESTORE_ID, "", "", Collections.singletonList(""), START_TIMESTAMP, END_TIMESTAMP));
        when(sdxBackupRestoreService.getTotalDurationInMin(START_TIMESTAMP, END_TIMESTAMP)).thenReturn(100L);
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        Map<Object, Object> variables = spy(new HashMap<>());
        testSupport.doExecute(context, event, variables);
        verify(variables).put("MAX_DURATION_IN_MIN", 150);
    }

    @Test
    public void testGetNewSdxIdForResizeDoExecute() throws Exception {
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(SKIP_OPTIONS), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
        when(sdxBackupRestoreService.getDatalakeBackupStatus(any(), eq(BACKUP_ID), any(), eq(USER_CRN)))
                .thenReturn(new SdxBackupStatusResponse(RESTORE_ID, "", "", Collections.singletonList(""), START_TIMESTAMP, END_TIMESTAMP));
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<DatalakeDatabaseRestoreStartEvent> captor = ArgumentCaptor.forClass(DatalakeDatabaseRestoreStartEvent.class);

        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeDatabaseRestoreStartEvent captorValue = captor.getValue();
        assertEquals(NEW_SDX_ID, captorValue.getResourceId());
    }

    @Test
    public void testValidationOnlyPassedRestoreCall() throws Exception {
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(true, true, true, true);

        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(skipOptions), eq(true)))
            .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
            USER_CRN, null, BACKUP_LOCATION, null, skipOptions, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, true);
        when(sdxBackupRestoreService.getDatalakeBackupStatus(any(), eq(BACKUP_ID), any(), eq(USER_CRN)))
                .thenReturn(new SdxBackupStatusResponse(RESTORE_ID, "", "", Collections.singletonList(""), START_TIMESTAMP, END_TIMESTAMP));
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);

        verify(sdxBackupRestoreService, times(1)).triggerDatalakeRestore(eq(2L), any(), any(), any(), any(),
            captor.capture());
        boolean captorValue = captor.getValue();
        assertTrue(captorValue);
    }

    static Object[][] testValidationOnly() {
        return new Object[][]{
                {true, DatalakeStatusEnum.DATALAKE_RESTORE_VALIDATION_INPROGRESS, ResourceEvent.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS},
                {false, DatalakeStatusEnum.DATALAKE_RESTORE_INPROGRESS, ResourceEvent.DATALAKE_RESTORE_IN_PROGRESS},
                {null, DatalakeStatusEnum.DATALAKE_RESTORE_INPROGRESS, ResourceEvent.DATALAKE_RESTORE_IN_PROGRESS}
        };
    }

    @ParameterizedTest(name = "validationOnly={0}, datalakeStatusEnum={1}, event={2}")
    @MethodSource("testValidationOnly")
    public void testDatalakeRestoreInProgressEvents(Boolean validationOnly, DatalakeStatusEnum datalakeStatusEnum, ResourceEvent event)
            throws Exception {
        AbstractAction action = (AbstractAction) underTest.datalakeRestoreInProgress();

        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), mock(SdxEvent.class));


        Map<Object, Object> variables = new HashMap<>();
        variables.put("OPERATION-ID", "123-123");
        if (validationOnly != null) {
            variables.put("VALIDATION_ONLY", validationOnly);
        }

        testSupport.doExecute(context, mock(SdxEvent.class), variables);

        verify(sdxStatusService, times(1)).setStatusForDatalakeAndNotify(eq(datalakeStatusEnum), eq(event), anyString(), anyLong());
    }

    @Test
    public void testAwaitServicesStoppedEmitsRequest() throws Exception {
        AbstractAction action = (AbstractAction) underTest.datalakeRestoreAwaitingServicesStopped();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);

        SdxOperation dr = mock(SdxOperation.class);
        DatalakeDatabaseRestoreStartEvent payload = new DatalakeDatabaseRestoreStartEvent(
                DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT.event(),
                OLD_SDX_ID,
                dr,
                USER_CRN,
                BACKUP_ID,
                RESTORE_ID,
                BACKUP_LOCATION,
                90,
                true
        );

        SdxContext ctx = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), payload);
        testSupport.doExecute(ctx, payload, new HashMap<>());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(any(), captor.capture());

        Object emitted = captor.getValue();
        assertTrue(emitted instanceof DatalakeRestoreAwaitServicesStoppedRequest);
        DatalakeRestoreAwaitServicesStoppedRequest req = (DatalakeRestoreAwaitServicesStoppedRequest) emitted;
        assertEquals(OLD_SDX_ID, req.getResourceId());
        assertEquals(USER_CRN, req.getUserId());
        assertEquals(RESTORE_ID, req.getOperationId());
        assertEquals(BACKUP_ID, req.getBackupId());
        assertEquals(BACKUP_LOCATION, req.getBackupLocation());
        assertEquals(90, req.getDatabaseMaxDurationInMin());
        assertTrue(req.isValidationOnly());
        assertTrue(req.getDatalakeRestoreParams() != null);
        assertSame(dr, req.getDrStatus());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private SdxCluster genCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(NEW_SDX_ID);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn("crn");
        sdxCluster.setClusterName(DATALAKE_NAME);
        return sdxCluster;
    }

}
