package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import static com.sequenceiq.cloudbreak.common.database.TargetMajorVersion.VERSION_11;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation.ValidateRdsUpgradeEvent.VALIDATE_RDS_UPGRADE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.AbstractValidateRdsUpgradeEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ValidateRdsUpgradeActionsTest {

    private static final Long STACK_ID = 1L;

    private static final String FLOW_ID = "flow_id_1";

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = VERSION_11;

    @Mock
    private ValidateRdsUpgradeService validateRdsUpgradeService;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private ValidateRdsUpgradeActions underTest;

    @Test
    public void testShouldPushSaltStatesNoOperation() throws Exception {
        AbstractAction action = (AbstractAction) underTest.pushSaltStates();
        ValidateRdsUpgradeTriggerRequest triggerEvent =
                new ValidateRdsUpgradeTriggerRequest(VALIDATE_RDS_UPGRADE_EVENT.event(), STACK_ID, TARGET_MAJOR_VERSION, new Promise<>());
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, false);

        verify(validateRdsUpgradeService).rdsUpgradeStarted(STACK_ID, TARGET_MAJOR_VERSION);
        verify(validateRdsUpgradeService, never()).pushSaltStates(STACK_ID);
        verifyBackupRestoreAction(ValidateRdsUpgradePushSaltStatesResult.class);
    }

    @Test
    public void testShouldValidateBackup() throws Exception {
        AbstractAction action = (AbstractAction) underTest.validateBackup();
        ValidateRdsUpgradePushSaltStatesResult triggerEvent = new ValidateRdsUpgradePushSaltStatesResult(STACK_ID);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verify(validateRdsUpgradeService).validateBackup(STACK_ID);
        verifyBackupRestoreAction(ValidateRdsUpgradeBackupValidationRequest.class);
    }

    @Test
    public void testShouldNotValidateBackup() throws Exception {
        AbstractAction action = (AbstractAction) underTest.validateBackup();
        ValidateRdsUpgradePushSaltStatesResult triggerEvent = new ValidateRdsUpgradePushSaltStatesResult(STACK_ID);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, false);

        verify(validateRdsUpgradeService, never()).validateBackup(STACK_ID);
        verifyBackupRestoreAction(ValidateRdsUpgradeBackupValidationResult.class);
    }

    @Test
    public void testShouldValidateOnCloudProvider() throws Exception {
        AbstractAction action = (AbstractAction) underTest.validateUpgradeOnCloudProvider();
        ValidateRdsUpgradeBackupValidationResult triggerEvent = new ValidateRdsUpgradeBackupValidationResult(STACK_ID);
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verify(validateRdsUpgradeService).validateOnCloudProvider(STACK_ID);
        verifyBackupRestoreAction(ValidateRdsUpgradeOnCloudProviderRequest.class);
    }

    @Test
    public void testShouldWaitForValidateOnCloudProvider() throws Exception {
        AbstractAction action = (AbstractAction) underTest.waitForValidateUpgradeOnCloudProvider();
        ValidateRdsUpgradeOnCloudProviderResult triggerEvent = new ValidateRdsUpgradeOnCloudProviderResult(STACK_ID,
                TARGET_MAJOR_VERSION,
                "",
                new FlowIdentifier(FlowType.FLOW, FLOW_ID),
                new DatabaseConnectionProperties());
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verifyNoInteractions(validateRdsUpgradeService);
        verifyBackupRestoreAction(WaitForValidateRdsUpgradeOnCloudProviderRequest.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidateConnection(boolean shouldRunDataBackupRestore) throws Exception {
        AbstractAction action = (AbstractAction) underTest.validateConnection();
        WaitForValidateRdsUpgradeOnCloudProviderResult triggerEvent = new WaitForValidateRdsUpgradeOnCloudProviderResult(STACK_ID,
                "",
                new DatabaseConnectionProperties());
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, shouldRunDataBackupRestore);

        verify(validateRdsUpgradeService, times(2)).shouldRunDataBackupRestore(any(), any(), any());
        if (shouldRunDataBackupRestore) {
            verify(validateRdsUpgradeService).validateConnection(STACK_ID);
            verifyBackupRestoreAction(ValidateRdsUpgradeConnectionRequest.class);
        } else {
            verify(validateRdsUpgradeService, never()).validateConnection(STACK_ID);
            verifyBackupRestoreAction(ValidateRdsUpgradeConnectionResult.class);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testValidateCleanup(boolean shouldRunDataBackupRestore) throws Exception {
        AbstractAction action = (AbstractAction) underTest.cleanupValidateResources();
        ValidateRdsUpgradeConnectionResult triggerEvent = new ValidateRdsUpgradeConnectionResult(STACK_ID,
                "");
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, shouldRunDataBackupRestore);

        verify(validateRdsUpgradeService, times(2)).shouldRunDataBackupRestore(any(), any(), any());
        if (shouldRunDataBackupRestore) {
            verify(validateRdsUpgradeService).validateCleanup(STACK_ID);
            verifyBackupRestoreAction(ValidateRdsUpgradeCleanupRequest.class);
        } else {
            verify(validateRdsUpgradeService, never()).validateConnection(STACK_ID);
            verifyBackupRestoreAction(ValidateRdsUpgradeCleanupResult.class);
        }
    }

    @Test
    public void testShouldWaitForValidateCleanup() throws Exception {
        AbstractAction action = (AbstractAction) underTest.waitForCleanupValidateResources();
        ValidateRdsUpgradeCleanupResult triggerEvent = new ValidateRdsUpgradeCleanupResult(STACK_ID,
                new FlowIdentifier(FlowType.FLOW, FLOW_ID));
        mockAndTriggerRdsUpgradeAction(action, triggerEvent, true);

        verifyNoInteractions(validateRdsUpgradeService);
        verifyBackupRestoreAction(WaitForValidateRdsUpgradeCleanupRequest.class);
    }

    private void mockAndTriggerRdsUpgradeAction(AbstractAction action, AbstractValidateRdsUpgradeEvent triggerEvent,
            boolean shouldRunDataBackupRestore) throws Exception {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        when(runningFlows.getFlowChainId(anyString())).thenReturn(FLOW_ID);

        StackView stack = mock(StackView.class);
        when(stack.getId()).thenReturn(STACK_ID);
        ClusterView cluster = mock(ClusterView.class);
        Database database = new Database();

        lenient().when(validateRdsUpgradeService.shouldRunDataBackupRestore(stack, cluster, database)).thenReturn(shouldRunDataBackupRestore);
        ValidateRdsUpgradeContext context = new ValidateRdsUpgradeContext(new FlowParameters(FLOW_ID, FLOW_ID), stack, cluster, database,
                TARGET_MAJOR_VERSION, null);

        AbstractActionTestSupport testSupport = new AbstractActionTestSupport(action);
        Map<Object, Object> variables = new HashMap<>();
        variables.put("TARGET_MAJOR_VERSION", TARGET_MAJOR_VERSION);
        testSupport.doExecute(context, triggerEvent, variables);
    }

    private void verifyBackupRestoreAction(Object expectedEvent) {
        ArgumentCaptor<AbstractValidateRdsUpgradeEvent> captor = ArgumentCaptor.forClass(AbstractValidateRdsUpgradeEvent.class);
        verify(reactorEventFactory, times(1)).createEvent(any(), captor.capture());
        AbstractValidateRdsUpgradeEvent captorValue = captor.getValue();
        assertEquals(expectedEvent, captorValue.getClass());
        assertEquals(STACK_ID, captorValue.getResourceId());
    }
}