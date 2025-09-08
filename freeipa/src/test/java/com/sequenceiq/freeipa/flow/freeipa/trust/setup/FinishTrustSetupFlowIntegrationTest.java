package com.sequenceiq.freeipa.flow.freeipa.trust.setup;

import static com.sequenceiq.common.api.type.Tunnel.CLUSTER_PROXY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.FlowIntegrationTestConfig;
import com.sequenceiq.freeipa.flow.StackStatusFinalizer;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowConfig;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupAddTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupFailedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.action.FinishTrustSetupFinishedAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.handler.AddTrustHandler;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.freeipa.trust.setup.AddTrustService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.sync.crossrealmtrust.CrossRealmTrustStatusSyncJobService;

import io.micrometer.core.instrument.MeterRegistry;

@ActiveProfiles("integration-test")
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "cb.max.salt.restore.dl_and_validate.retry=90",
        "cb.max.salt.restore.dl_and_validate.retry.onerror=5"
})
class FinishTrustSetupFlowIntegrationTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String OPERATION_ID = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = "accId";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FreeIpaFlowManager freeIpaFlowManager;

    @MockBean
    private StackToCloudStackConverter stackToCloudStackConverter;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private StackUpdater stackUpdater;

    @MockBean
    private FlowOperationStatsRepository flowOperationStatsRepository;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @MockBean
    private MeterRegistry meterRegistry;

    @MockBean
    private NodeConfig nodeConfig;

    @MockBean
    private OperationService operationService;

    @MockBean
    private NodeValidator nodeValidator;

    @MockBean
    private AddTrustService addTrustService;

    @MockBean
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private StackService stackService;

    @MockBean
    private CrossRealmTrustStatusSyncJobService crossRealmTrustStatusSyncJobService;

    @MockBean
    private StackStatusFinalizer stackStatusFinalizer;

    private Stack stack;

    private InstanceMetaData instanceMetaData;

    @BeforeEach
    public void setup() throws FreeIpaClientException, QuotaExceededException, CloudbreakOrchestratorException {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setTunnel(CLUSTER_PROXY);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        InstanceGroup ig = new InstanceGroup();
        instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        instanceMetaData.setDiscoveryFQDN("ipaserver0.example.com");
        ig.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(ig));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    @Test
    void testFinishCrossRealmTrustWhenSuccessful() {
        testFlow();
        InOrder stackStatusVerify = inOrder(stackUpdater);

        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, TRUST_SETUP_FINISH_IN_PROGRESS, "Add cross-realm trust to FreeIPA");
        stackStatusVerify.verify(stackUpdater)
                .updateStackStatus(stack, DetailedStackStatus.TRUST_SETUP_FINISH_SUCCESSFUL,
                        "Finish setting up cross-realm trust was successful");

        InOrder crossRealmStatusVerify = inOrder(crossRealmTrustService);
        crossRealmStatusVerify.verify(crossRealmTrustService).updateTrustStateByStackId(stack.getId(), TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS);
        crossRealmStatusVerify.verify(crossRealmTrustService).updateTrustStateByStackId(stack.getId(), TrustStatus.TRUST_ACTIVE);
        verify(crossRealmTrustStatusSyncJobService).schedule(STACK_ID);
    }

    @Test
    public void testAddTrustFails() throws FreeIpaClientException {
        doThrow(new FreeIpaClientException("Cross-realm add trust failed")).when(addTrustService).addAndValidateTrust(STACK_ID);
        testFlow();
        InOrder stackStatusVerify = inOrder(stackUpdater);

        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, TRUST_SETUP_FINISH_IN_PROGRESS, "Add cross-realm trust to FreeIPA");
        stackStatusVerify.verify(stackUpdater).updateStackStatus(stack, TRUST_SETUP_FINISH_FAILED,
                "Failed to finish cross-realm trust FreeIPA: Cross-realm add trust failed");

        InOrder crossRealmStatusVerify = inOrder(crossRealmTrustService);
        crossRealmStatusVerify.verify(crossRealmTrustService).updateTrustStateByStackId(stack.getId(), TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS);
        crossRealmStatusVerify.verify(crossRealmTrustService).updateTrustStateByStackId(stack.getId(), TrustStatus.TRUST_SETUP_FINISH_FAILED);
    }

    private void testFlow() {
        FlowIdentifier flowIdentifier = triggerFlow();
        letItFlow(flowIdentifier);

        flowFinishedSuccessfully();
    }

    private void flowFinishedSuccessfully() {
        ArgumentCaptor<FlowLog> flowLog = ArgumentCaptor.forClass(FlowLog.class);
        verify(flowLogRepository, times(2)).save(flowLog.capture());
        assertTrue(flowLog.getAllValues().stream().anyMatch(FlowLog::getFinalized), "flow has not finalized");
    }

    private FlowIdentifier triggerFlow() {
        FinishTrustSetupEvent prepareCrossRealmTrustEvent = new FinishTrustSetupEvent(STACK_ID, OPERATION_ID);
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> freeIpaFlowManager.notify(prepareCrossRealmTrustEvent.selector(), prepareCrossRealmTrustEvent));
    }

    private void letItFlow(FlowIdentifier flowIdentifier) {
        int i = 0;
        do {
            i++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        } while (flowRegister.get(flowIdentifier.getPollableId()) != null && i < 30);
    }

    @Profile("integration-test")
    @TestConfiguration
    @Import(value = {
            FreeIpaFinishTrustSetupFlowConfig.class,
            FlowIntegrationTestConfig.class,
            WebApplicationExceptionMessageExtractor.class,
            FinishTrustSetupFailedAction.class,
            FinishTrustSetupAddTrustAction.class,
            FinishTrustSetupFinishedAction.class,
            AddTrustHandler.class,
            CrossRealmTrustService.class,
            TaskResultConverter.class
    })
    static class Config {
        @MockBean
        private StackService stackService;
    }
}
