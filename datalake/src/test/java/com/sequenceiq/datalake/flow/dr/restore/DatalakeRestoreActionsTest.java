package com.sequenceiq.datalake.flow.dr.restore;


import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.chain.DatalakeResizeFlowEventChainFactory;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLogWithoutPayload;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
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

    @Test
    public void testRestoreWithNoFlowChain() throws Exception {
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(OLD_SDX_ID), any(), any(), eq(USER_CRN), any(DatalakeDrSkipOptions.class), eq(0),  eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), OLD_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
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

        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(skipOptions), eq(0), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, skipOptions, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
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
        int fullDrMaxDurationInMin = 80;
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), any(DatalakeDrSkipOptions.class),
                eq(fullDrMaxDurationInMin), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE,
                fullDrMaxDurationInMin, false);
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
    public void testGetNewSdxIdForResizeDoExecute() throws Exception {
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(SKIP_OPTIONS), eq(0), eq(false)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, SKIP_OPTIONS, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, false);
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

        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN), eq(skipOptions), eq(0), eq(true)))
            .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, DatalakeOperationStatus.State.STARTED, null, null));
        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), NEW_SDX_ID, DATALAKE_NAME,
            USER_CRN, null, BACKUP_LOCATION, null, skipOptions, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, 0, true);
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap());

        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);

        verify(sdxBackupRestoreService, times(1)).triggerDatalakeRestore(eq(2L), any(), any(), any(), any(), eq(0),
            captor.capture());
        boolean captorValue = captor.getValue();
        assertTrue(captorValue);
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
