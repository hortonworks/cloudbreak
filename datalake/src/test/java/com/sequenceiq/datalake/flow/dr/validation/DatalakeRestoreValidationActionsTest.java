package com.sequenceiq.datalake.flow.dr.validation;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus.State;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerRestoreValidationEvent;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@ExtendWith(MockitoExtension.class)
public class DatalakeRestoreValidationActionsTest {

    private static final Long SDX_ID = 1L;

    private static final String FLOW_ID = "flow_id";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3://cloudbreak-bucket/backup-location";

    private static final String BACKUP_ID = "backup_id";

    private static final String RESTORE_ID = "restore_id";

    @InjectMocks
    private final DatalakeRestoreValidationActions underTest = new DatalakeRestoreValidationActions();

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
    public void testRestoreValidationAction() throws Exception {
        DatalakeTriggerRestoreValidationEvent event = new DatalakeTriggerRestoreValidationEvent(DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT.event(),
                SDX_ID, USER_CRN, BACKUP_LOCATION, DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, null);
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxBackupRestoreService.triggerDatalakeRestoreValidation(eq(sdxCluster), eq(USER_CRN)))
                .thenReturn(new DatalakeRestoreStatusResponse(BACKUP_ID, RESTORE_ID, State.STARTED, null, null));
        when(sdxService.getById(eq(SDX_ID))).thenReturn(sdxCluster);

        AbstractAction action = (AbstractAction) underTest.triggerDatalakeRestoreValidationAction();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        SdxContext context = SdxContext.from(new FlowParameters(FLOW_ID, FLOW_ID), event);
        testSupport.doExecute(context, event, new HashMap<>());

        ArgumentCaptor<DatalakeTriggerRestoreValidationEvent> captor = ArgumentCaptor.forClass(DatalakeTriggerRestoreValidationEvent.class);

        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        DatalakeTriggerRestoreValidationEvent captorValue = captor.getValue();
        assertEquals(SDX_ID, captorValue.getResourceId());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
