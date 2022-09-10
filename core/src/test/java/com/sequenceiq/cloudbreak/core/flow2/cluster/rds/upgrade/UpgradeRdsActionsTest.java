package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class UpgradeRdsActionsTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flow_id_1";

    @Mock
    private UpgradeRdsService upgradeRdsService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private UpgradeRdsActions upgradeRdsActions;

    @Test
    public void testShouldBackupDataFromRds() throws Exception {
        AbstractAction action = (AbstractAction) upgradeRdsActions.backupDataFromRds();
        UpgradeRdsStopServicesResult triggerEvent = new UpgradeRdsStopServicesResult(STACK_ID, TargetMajorVersion.VERSION_11);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verify(upgradeRdsService).backupRdsState(STACK_ID);
        verifyBackupRestoreAction(UpgradeRdsDataBackupRequest.class);
    }

    @Test
    public void testShouldNotBackupDataFromRds() throws Exception {
        AbstractAction action = (AbstractAction) upgradeRdsActions.backupDataFromRds();
        UpgradeRdsStopServicesResult triggerEvent = new UpgradeRdsStopServicesResult(STACK_ID, TargetMajorVersion.VERSION_11);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, false);

        verify(upgradeRdsService, never()).backupRdsState(STACK_ID);
        verifyBackupRestoreAction(UpgradeRdsDataBackupResult.class);
    }

    @Test
    public void testShouldRestoreDataFromRds() throws Exception {
        AbstractAction action = (AbstractAction) upgradeRdsActions.restoreDataToRds();
        UpgradeRdsUpgradeDatabaseServerResult triggerEvent = new UpgradeRdsUpgradeDatabaseServerResult(STACK_ID, TargetMajorVersion.VERSION_11);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verify(upgradeRdsService).restoreRdsState(STACK_ID);
        verifyBackupRestoreAction(UpgradeRdsDataRestoreRequest.class);
    }

    @Test
    public void testShouldNotRestoreDataFromRds() throws Exception {
        AbstractAction action = (AbstractAction) upgradeRdsActions.restoreDataToRds();
        UpgradeRdsUpgradeDatabaseServerResult triggerEvent = new UpgradeRdsUpgradeDatabaseServerResult(STACK_ID, TargetMajorVersion.VERSION_11);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, false);

        verify(upgradeRdsService, never()).restoreRdsState(STACK_ID);
        verifyBackupRestoreAction(UpgradeRdsDataRestoreResult.class);
    }

    private void mockAndTriggerRdsUpgradeAction(AbstractAction action, AbstractUpgradeRdsEvent triggerEvent,
            boolean shouldRunDataBackupRestore) throws Exception {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);

        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        ClusterView cluster = mock(ClusterView.class);
        when(upgradeRdsService.shouldRunDataBackupRestore(stack, cluster)).thenReturn(shouldRunDataBackupRestore);
        UpgradeRdsContext context =  new UpgradeRdsContext(new FlowParameters(FLOW_ID, FLOW_ID, null), stack, cluster, TargetMajorVersion.VERSION_11);

        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        testSupport.doExecute(context, triggerEvent, new HashMap<>());
    }

    private void verifyBackupRestoreAction(Object expectedEvent) {
        ArgumentCaptor<AbstractUpgradeRdsEvent> captor = ArgumentCaptor.forClass(AbstractUpgradeRdsEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        AbstractUpgradeRdsEvent captorValue = captor.getValue();
        assertEquals(expectedEvent, captorValue.getClass());
        assertEquals(STACK_ID, captorValue.getResourceId());
        assertEquals(TargetMajorVersion.VERSION_11, captorValue.getVersion());
    }
}