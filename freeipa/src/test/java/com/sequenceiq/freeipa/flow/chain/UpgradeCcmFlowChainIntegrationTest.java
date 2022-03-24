package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.chain.FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.stack.update.UpdateUserDataFlowConfig;
import com.sequenceiq.freeipa.flow.stack.update.action.UserDataUpdateActions;
import com.sequenceiq.freeipa.flow.stack.update.handler.UpdateUserDataHandler;
import com.sequenceiq.freeipa.flow.stack.update.handler.UpdateUserDataOnProviderHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmFlowConfig;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmOperationAcceptor;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action.UpgradeCcmActions;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmChangeTunnelHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmCheckPrerequisitesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmHealthCheckHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmPushSaltStatesHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmReconfigureHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRegisterCcmHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmRemoveMinaHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.UpgradeCcmUpgradeHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
class UpgradeCcmFlowChainIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String USER_DATA = "hello hello is there anybody out there";

    private static final long STACK_ID = 1L;

    private static final int ALL_CALLED_ONCE = 18;

    private static final int CALLED_ONCE_TILL_REMOVE_MINA = 16;

    private static final int CALLED_ONCE_TILL_GENERATE_USERDATA = 17;

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

    @MockBean
    private UserDataService userDataService;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @MockBean
    private CloudPlatformConnectors cloudPlatformConnectors;

    @MockBean
    private StackToCloudStackConverter cloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialConverter;

    @MockBean
    private CredentialService credentialService;

    @Mock
    private ResourceConnector<Object> resourcesApi;

    @BeforeEach
    public void setup() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        ImageEntity image = new ImageEntity();
        stack.setImage(image);
        image.setUserdata(USER_DATA);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        CloudConnector<Object> connector = mock(CloudConnector.class);
        AuthenticatedContext context = mock(AuthenticatedContext.class);
        Authenticator authApi = mock(Authenticator.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(connector);
        when(connector.authentication()).thenReturn(authApi);
        when(connector.resources()).thenReturn(resourcesApi);
        when(authApi.authenticate(any(), any())).thenReturn(context);
    }

    @Test
    @Ignore
    public void testCcmUpgradeFlowChainWhenSuccessful() throws Exception {
        testFlow(ALL_CALLED_ONCE, true, true);
    }

    @Test
    @Ignore
    public void testUpdateUserDataFailsInChain() throws Exception {
        doThrow(new BadRequestException()).when(userDataService).createUserData(STACK_ID);
        testFlow(CALLED_ONCE_TILL_GENERATE_USERDATA, true, false);
    }

    @Test
    @Ignore
    public void testCcmUpgradeWhenRemoveMinaFailsInChain() throws Exception {
        doThrow(new BadRequestException()).when(upgradeCcmService).removeMina(STACK_ID);
        testFlow(CALLED_ONCE_TILL_REMOVE_MINA, false, false);
    }

    private void testFlow(int calledOnce, boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess) throws Exception {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully(ccmUpgradeSuccess ? 2 : 1);
        verifyServiceCalls(calledOnce);
        verifyFinishingStatCalls(ccmUpgradeSuccess, userDataUpdateSuccess);
    }

    private void verifyFinishingStatCalls(boolean ccmUpgradeSuccess, boolean userDataUpdateSuccess) throws Exception {
        verify(upgradeCcmService, times(ccmUpgradeSuccess ? 1 : 0)).finishedState(STACK_ID);
        verify(resourcesApi, times(userDataUpdateSuccess ? 1 : 0)).updateUserData(any(), any(), any(), eq(USER_DATA));

        verify(upgradeCcmService, times(ccmUpgradeSuccess ? 0 : 1)).failedState(STACK_ID);

        verify(operationService, times(ccmUpgradeSuccess && userDataUpdateSuccess ? 1 : 0)).completeOperation(any(), any(), any(), any());
        verify(operationService, times(ccmUpgradeSuccess && userDataUpdateSuccess ? 0 : 1)).failOperation(any(), any(), any());

    }

    private void verifyServiceCalls(int calledOnceCount) throws Exception {
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
        verify(userDataService, times(expected[i++])).createUserData(STACK_ID);
        verify(resourcesApi, times(expected[i++])).updateUserData(any(), any(), any(), eq(USER_DATA));
    }

    private void flowFinishedSuccessfully(int numberOfRanFlows) {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2 * numberOfRanFlows)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(f -> f.getFinalized()), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        String selector = UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(selector,
                        new UpgradeCcmFlowChainTriggerEvent(selector, "opi", STACK_ID)));
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
            UpgradeCcmFlowEventChainFactory.class,
            UpdateUserDataFlowConfig.class,
            UserDataUpdateActions.class,
            UpdateUserDataHandler.class,
            UpdateUserDataOnProviderHandler.class,
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