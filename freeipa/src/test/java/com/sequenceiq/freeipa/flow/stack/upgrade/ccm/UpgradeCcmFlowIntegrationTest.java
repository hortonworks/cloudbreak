package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmActions;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmChangeTunnelHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmCheckPrerequisitesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmHealthCheckHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmPushSaltStatesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmReconfigureHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRegisterCcmHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRemoveMinaHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmUpgradeHandler;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpgradeCcmFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final int ALL_CALLED_ONCE = 16;

    private static final int CALLED_ONCE_TILL_PREPARATION = 2;

    private static final int CALLED_ONCE_TILL_CHANGE_TUNNEL = 4;

    private static final int CALLED_ONCE_TILL_PUSH = 6;

    private static final int CALLED_ONCE_TILL_UPGRADE = 8;

    private static final int CALLED_ONCE_TILL_RECONFIGURE = 10;

    private static final int CALLED_ONCE_TILL_REGISTER = 12;

    private static final int CALLED_ONCE_TILL_HEALTH_CHECK = 14;

    private static final int CALLED_ONCE_TILL_REMOVE_MINA = 16;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockBean(reset = MockReset.NONE)
    private StackService stackService;

    @MockBean
    private UpgradeCcmService upgradeCcmService;

    @MockBean
    private OperationService operationService;

    @BeforeEach
    public void setup() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    @Test
    public void testCcmUpgradeWhenSuccessful() {
        testFlow(ALL_CALLED_ONCE, true);
    }

    @Test
    public void testCcmUpgradeWhenPreparationFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).checkPrerequsities(1L);
        testFlow(CALLED_ONCE_TILL_PREPARATION, false);
    }

    @Test
    public void testCcmUpgradeWhenChangeTunnelFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).changeTunnel(1L);
        testFlow(CALLED_ONCE_TILL_CHANGE_TUNNEL, false);
    }

    @Test
    public void testCcmUpgradeWhenPushSaltStatesFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).pushSaltStates(1L);
        testFlow(CALLED_ONCE_TILL_PUSH, false);
    }

    @Test
    public void testCcmUpgradeWhenUpgradeFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).upgrade(1L);
        testFlow(CALLED_ONCE_TILL_UPGRADE, false);
    }

    @Test
    public void testCcmUpgradeWhenReconfigureFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).reconfigure(1L);
        testFlow(CALLED_ONCE_TILL_RECONFIGURE, false);
    }

    @Test
    public void testCcmUpgradeWhenRegisterCcmFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).registerCcm(1L);
        testFlow(CALLED_ONCE_TILL_REGISTER, false);
    }

    @Test
    public void testCcmUpgradeWhenHealthCheckFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).healthCheck(1L);
        testFlow(CALLED_ONCE_TILL_HEALTH_CHECK, false);
    }

    @Test
    public void testCcmUpgradeWhenRemoveMinaFails() {
        doThrow(new BadRequestException()).when(upgradeCcmService).removeMina(1L);
        testFlow(CALLED_ONCE_TILL_REMOVE_MINA, false);
    }

    private void testFlow(int calledOnceCount, boolean success) {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
        verifyServiceCalls(calledOnceCount);
        verifyFinishingStatCalls(success);
    }

    private void verifyFinishingStatCalls(boolean success) {
        verify(upgradeCcmService, times(success ? 1 : 0)).finishedState(STACK_ID);
        verify(operationService, times(success ? 1 : 0)).completeOperation(any(), any(), any(), any());
        verify(upgradeCcmService, times(success ? 0 : 1)).failedState(STACK_ID);
        verify(operationService, times(success ? 0 : 1)).failOperation(any(), any(), any());

    }

    private void verifyServiceCalls(int calledOnceCount) {
        final int[] expected = new int[ALL_CALLED_ONCE];
        Arrays.fill(expected, 0, calledOnceCount, 1);
        int i = 0;
        verify(upgradeCcmService, times(expected[i++])).checkPrerequisitesState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).checkPrerequsities(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).changeTunnelState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).changeTunnel(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).pushSaltStatesState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).pushSaltStates(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).upgradeState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).upgrade(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).reconfigureState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).reconfigure(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).registerCcmState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).registerCcm(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).healthCheckState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).healthCheck(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).removeMinaState(STACK_ID);
        verify(upgradeCcmService, times(expected[i++])).removeMina(STACK_ID);
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_TRIGGER_EVENT.event();
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new UpgradeCcmTriggerEvent(selector, "opi", STACK_ID)));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 10);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import({
            UpgradeCcmOperationAcceptor.class,
            UpgradeCcmFlowConfig.class,
            UpgradeCcmActions.class,
            UpgradeCcmCheckPrerequisitesHandler.class,
            UpgradeCcmChangeTunnelHandler.class,
            UpgradeCcmPushSaltStatesHandler.class,
            UpgradeCcmUpgradeHandler.class,
            UpgradeCcmReconfigureHandler.class,
            UpgradeCcmRegisterCcmHandler.class,
            UpgradeCcmHealthCheckHandler.class,
            UpgradeCcmRemoveMinaHandler.class,
            UpgradeCcmService.class,
            FlowIntegrationTestConfig.class
    })
    static class Config {
    }
}