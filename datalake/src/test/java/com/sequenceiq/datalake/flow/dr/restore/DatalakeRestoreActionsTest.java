package com.sequenceiq.datalake.flow.dr.restore;


import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse.State;
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
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class DatalakeRestoreActionsTest {

    private static final Long OLD_SDX_ID = 1L;

    private static final Long NEW_SDX_ID = 2L;

    private static final String FLOW_ID = "flow_id";

    private static final String DATALAKE_NAME = "test_dl";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3://cloudbreak-bucket/backup-location";

    private static final String BACKUP_ID = "backup_id";

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
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(OLD_SDX_ID), any(), any(), eq(USER_CRN)))
                .thenReturn(new DatalakeDrStatusResponse(BACKUP_ID, State.STARTED, Optional.empty()));
        when(flowLogService.getLastFlowLog(anyString())).thenReturn(Optional.of(new FlowLog()));
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(any())).thenReturn(Optional.empty());

        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), OLD_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE);
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID, null), event);
        testSupport.doExecute(context, event, new HashMap());

        verify(reactorEventFactory, times(1)).createEvent(any(), any(DatalakeDatabaseRestoreStartEvent.class));
    }

    @Test
    public void testGetNewSdxIdForResize() throws Exception {

        SdxCluster sdxCluster = genCluster();
        when(sdxService.getByNameInAccount(eq(USER_CRN), eq(DATALAKE_NAME))).thenReturn(sdxCluster);
        when(sdxBackupRestoreService.triggerDatalakeRestore(eq(NEW_SDX_ID), any(), any(), eq(USER_CRN)))
                .thenReturn(new DatalakeDrStatusResponse(BACKUP_ID, State.STARTED, Optional.empty()));
        when(flowLogService.getLastFlowLog(anyString())).thenReturn(Optional.of(new FlowLog()));
        FlowChainLog flowChainLog = new FlowChainLog();
        flowChainLog.setFlowChainType(DatalakeResizeFlowEventChainFactory.class.getName());
        when(flowChainLogService.findFirstByFlowChainIdOrderByCreatedDesc(any())).thenReturn(Optional.of(flowChainLog));

        DatalakeTriggerRestoreEvent event = new DatalakeTriggerRestoreEvent(DATALAKE_TRIGGER_RESTORE_EVENT.event(), OLD_SDX_ID, DATALAKE_NAME,
                USER_CRN, null, BACKUP_LOCATION, null, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE);
        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestore();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID, null), event);
        testSupport.doExecute(context, event, new HashMap());

        verify(reactorEventFactory, times(1)).createEvent(any(), any(DatalakeDatabaseRestoreStartEvent.class));

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
