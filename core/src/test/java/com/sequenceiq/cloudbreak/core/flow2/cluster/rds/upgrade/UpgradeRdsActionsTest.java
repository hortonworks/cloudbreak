package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;


import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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

    @ParameterizedTest
    @MethodSource("provideTestCombinations")
    public void backupDataFromRds(CloudPlatform cloudPlatform, boolean embeddedDatabaseOnAttachedDisk, boolean runBackup) throws Exception {

        StackView stack = mock(StackView.class);
        ClusterView cluster = mock(ClusterView.class);
        AbstractAction action = (AbstractAction) upgradeRdsActions.backupDataFromRds();
        initGlobalPrivateFields();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        UpgradeRdsStopServicesResult triggerEvent = new UpgradeRdsStopServicesResult(STACK_ID, TargetMajorVersion.VERSION_11);
        UpgradeRdsContext rdsContext = new UpgradeRdsContext(new FlowParameters(FLOW_ID, FLOW_ID, null), stack, cluster, TargetMajorVersion.VERSION_11);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);
        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(embeddedDatabaseOnAttachedDisk);
        testSupport.doExecute(rdsContext, triggerEvent, new HashMap<>());

        if (runBackup) {
            verify(upgradeRdsService).backupRdsState(STACK_ID);
        } else {
            verify(upgradeRdsService, never()).backupRdsState(STACK_ID);
        }

        ArgumentCaptor<AbstractUpgradeRdsEvent> captor = ArgumentCaptor.forClass(AbstractUpgradeRdsEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        AbstractUpgradeRdsEvent captorValue = captor.getValue();
        if (runBackup) {
            assertEquals(UpgradeRdsDataBackupRequest.class, captorValue.getClass());
        } else {
            assertEquals(UpgradeRdsDataBackupResult.class, captorValue.getClass());
        }
        assertEquals(STACK_ID, captorValue.getResourceId());
        assertEquals(TargetMajorVersion.VERSION_11, captorValue.getVersion());
    }

    @ParameterizedTest
    @MethodSource("provideTestCombinations")
    public void restoreDataFromRds(CloudPlatform cloudPlatform, boolean embeddedDatabaseOnAttachedDisk, boolean runRestore) throws Exception {

        StackView stack = mock(StackView.class);
        ClusterView cluster = mock(ClusterView.class);
        AbstractAction action = (AbstractAction) upgradeRdsActions.restoreDataToRds();
        initGlobalPrivateFields();
        initActionPrivateFields(action);
        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        UpgradeRdsUpgradeDatabaseServerResult triggerEvent = new UpgradeRdsUpgradeDatabaseServerResult(STACK_ID, TargetMajorVersion.VERSION_11);
        UpgradeRdsContext rdsContext = new UpgradeRdsContext(new FlowParameters(FLOW_ID, FLOW_ID, null), stack, cluster, TargetMajorVersion.VERSION_11);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);
        when(stack.getCloudPlatform()).thenReturn(cloudPlatform.name());
        when(stack.getId()).thenReturn(STACK_ID);
        when(cluster.getEmbeddedDatabaseOnAttachedDisk()).thenReturn(embeddedDatabaseOnAttachedDisk);
        testSupport.doExecute(rdsContext, triggerEvent, new HashMap<>());

        if (runRestore) {
            verify(upgradeRdsService).restoreRdsState(STACK_ID);
        } else {
            verify(upgradeRdsService, never()).restoreRdsState(STACK_ID);
        }

        ArgumentCaptor<AbstractUpgradeRdsEvent> captor = ArgumentCaptor.forClass(AbstractUpgradeRdsEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        AbstractUpgradeRdsEvent captorValue = captor.getValue();
        if (runRestore) {
            assertEquals(UpgradeRdsDataRestoreRequest.class, captorValue.getClass());
        } else {
            assertEquals(UpgradeRdsDataRestoreResult.class, captorValue.getClass());
        }
        assertEquals(STACK_ID, captorValue.getResourceId());
        assertEquals(TargetMajorVersion.VERSION_11, captorValue.getVersion());
    }

    private void initGlobalPrivateFields() {
        Field cloudPlatformsToRunBackupRestore = ReflectionUtils.findField(UpgradeRdsActions.class, "cloudPlatformsToRunBackupRestore");
        ReflectionUtils.makeAccessible(cloudPlatformsToRunBackupRestore);
        ReflectionUtils.setField(cloudPlatformsToRunBackupRestore, upgradeRdsActions, Set.of(AZURE));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }

    private static Stream<Arguments> provideTestCombinations() {
        return Stream.of(
                //cloudPlatform,embeddedDatabaseOnAttachedDisk,runBackup
                Arguments.of(AZURE, true, false),
                Arguments.of(AZURE, false, true),
                Arguments.of(AWS, true, false),
                Arguments.of(AWS, false, false)
                );
    }
}