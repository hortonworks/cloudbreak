package com.sequenceiq.environment.environment.flow.hybrid.cancel;


import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_ENTITY_DELETE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_SALT_UPDATE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_CONFIG_REMOVAL_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_CONFIG_REMOVAL_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_SALT_UPDATE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_TRUST_ENTITY_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_REQUIRED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_CONFIG_REMOVAL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_SALT_UPDATE_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_VALIDATION_STATE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.action.EnvironmentCrossRealmTrustCancelActions;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.config.EnvironmentCrossRealmTrustCancelFlowConfig;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.handler.EnvironmentCrossRealmTrustCancelConfigRemovalHandler;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.handler.EnvironmentCrossRealmTrustCancelHandler;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.handler.EnvironmentCrossRealmTrustCancelSaltUpdateHandler;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.handler.EnvironmentCrossRealmTrustEntityDeleteHandler;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.handler.EnvironmentValidateCrossRealmTrustCancelHandler;
import com.sequenceiq.environment.environment.flow.hybrid.setup.converter.SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.cluster.ClusterService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.experience.ExperienceConnectorService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.operation.service.OperationService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsPersister;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;
import com.sequenceiq.notification.WebSocketNotificationService;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(SpringExtension.class)
class EnvironmentCrossRealmTrustCancelFlowIntegrationTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long ENVIRONMENT_ID = 1L;

    private static final String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String ACCOUNT_ID = "accId";

    private static final String REALM = "EXAMPLE.COM";

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private EnvironmentReactorFlowManager environmentReactorFlowManager;

    @MockBean
    private FlowOperationStatsRepository flowOperationStatsRepository;

    @MockBean
    private FlowCancelService flowCancelService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventCommonListener flowEventCommonListener;

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
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @MockBean
    private FreeIpaService freeIpaService;

    @MockBean
    private FreeIpaPollerService freeIpaPollerService;

    @MockBean
    private ClusterService clusterService;

    @MockBean
    private StackPollerService stackPollerService;

    @MockBean
    private DatahubPollerProvider datahubPollerProvider;

    @MockBean
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @MockBean
    private StackService stackService;

    @MockBean
    private SetupCrossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter crossRealmTrustRequestToEnvironmentCrossRealmTrustSetupEventConverter;

    @MockBean
    private Environment environment;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    public void cancel() throws QuotaExceededException {
        environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setResourceCrn(ENVIRONMENT_CRN);
        environment.setName(ENVIRONMENT_NAME);
        environment.setAccountId(ACCOUNT_ID);

        // Default stub: FreeIPA is available and has a trust configured.
        // Tests that simulate FreeIPA failure override this with doThrow.
        TrustResponse trustResponse = new TrustResponse();
        trustResponse.setRealm(REALM);
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setStatus(Status.AVAILABLE);
        describeFreeIpaResponse.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        describeFreeIpaResponse.setTrust(trustResponse);
        when(freeIpaService.describe(anyString())).thenReturn(Optional.of(describeFreeIpaResponse));

        // Default stub: environment exists so config-removal handler can proceed.
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setName(ENVIRONMENT_NAME);
        environmentDto.setEnvironmentType(EnvironmentType.HYBRID);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(environmentDto));
    }

    @Test
    void testCancelCrossRealmTrustWhenSuccessful() {
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        // 1. Validation
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED),
                eq(TRUST_CANCEL_VALIDATION_STATE)
        );
        // 2. FreeIPA trust cancel (runs before CM config removal so realm is still available)
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_STARTED),
                eq(TRUST_CANCEL_STATE)
        );
        // 3. CM config removal
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_STARTED),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_STATE)
        );
        // 4. FreeIPA trust entity deletion
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_TRUST_ENTITY_DELETE_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_ENTITY_DELETE_STARTED),
                eq(TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE)
        );
        // 5. Salt update to remove trust.conf after the trust entity is gone
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_SALT_UPDATE_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_SALT_UPDATE_STARTED),
                eq(TRUST_CANCEL_SALT_UPDATE_STATE)
        );
        // 6. Finished
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_REQUIRED),
                eq(ENVIRONMENT_CANCEL_TRUST_FINISHED),
                eq(TRUST_CANCEL_FINISHED_STATE)
        );
    }

    @Test
    void testCancelCrossRealmTrustForNonHybridEnvSetsAvailable() {
        EnvironmentDto publicCloudEnvironmentDto = new EnvironmentDto();
        publicCloudEnvironmentDto.setId(ENVIRONMENT_ID);
        publicCloudEnvironmentDto.setResourceCrn(ENVIRONMENT_CRN);
        publicCloudEnvironmentDto.setName(ENVIRONMENT_NAME);
        publicCloudEnvironmentDto.setEnvironmentType(EnvironmentType.PUBLIC_CLOUD);
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(Optional.of(publicCloudEnvironmentDto));

        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(TRUST_CANCEL_VALIDATION_IN_PROGRESS), eq(ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED), eq(TRUST_CANCEL_VALIDATION_STATE));
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(TRUST_CANCEL_IN_PROGRESS), eq(ENVIRONMENT_CANCEL_TRUST_STARTED), eq(TRUST_CANCEL_STATE));
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_IN_PROGRESS), eq(ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_STARTED), eq(TRUST_CANCEL_CONFIG_REMOVAL_STATE));
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(TRUST_CANCEL_TRUST_ENTITY_DELETE_IN_PROGRESS), eq(ENVIRONMENT_CANCEL_TRUST_ENTITY_DELETE_STARTED),
                eq(TRUST_CANCEL_TRUST_ENTITY_DELETE_STATE));
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(TRUST_CANCEL_SALT_UPDATE_IN_PROGRESS), eq(ENVIRONMENT_CANCEL_TRUST_SALT_UPDATE_STARTED), eq(TRUST_CANCEL_SALT_UPDATE_STATE));
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class), any(Payload.class),
                eq(AVAILABLE), eq(ENVIRONMENT_CANCEL_TRUST_FINISHED), eq(TRUST_CANCEL_FINISHED_STATE));
    }

    @Test
    public void testCancelCrossRealmTrustWhenCmRemovalFailure() {
        doThrow(new CloudbreakServiceException("Failed to remove CM configuration"))
                .when(clusterService)
                .removeTrustedRealmConfigFromClusters(any(), any());
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED),
                eq(TRUST_CANCEL_VALIDATION_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_STARTED),
                eq(TRUST_CANCEL_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_STARTED),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(BaseFailedFlowEvent.class),
                eq(TRUST_CANCEL_CONFIG_REMOVAL_FAILED),
                eq(ENVIRONMENT_CANCEL_TRUST_CONFIG_REMOVAL_FAILED),
                eq(TRUST_CANCEL_FAILED_STATE)
        );
    }

    @Test
    public void testCancelCrossRealmTrustWhenFailure() {
        // FreeIPA describe throws during TRUST_CANCEL_STATE (FreeIPA cancel handler),
        // which is now the step immediately after validation.
        doThrow(new CloudbreakServiceException("Freeipa not exist on provider side"))
                .when(freeIpaService)
                .describe(anyString());
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED),
                eq(TRUST_CANCEL_VALIDATION_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_STARTED),
                eq(TRUST_CANCEL_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(BaseFailedFlowEvent.class),
                eq(TRUST_CANCEL_FAILED),
                eq(ENVIRONMENT_CANCEL_TRUST_FAILED),
                eq(TRUST_CANCEL_FAILED_STATE)
        );
    }

    @Test
    public void testCancelCrossRealmTrustWhenValidateFailureHappens() {
        doThrow(new CloudbreakServiceException("Freeipa not exist on provider side"))
                .when(environmentService)
                .validateCancelCrossRealmSetup();
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_CANCEL_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED),
                eq(TRUST_CANCEL_VALIDATION_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(BaseFailedFlowEvent.class),
                eq(TRUST_CANCEL_FAILED),
                eq(ENVIRONMENT_CANCEL_TRUST_FAILED),
                eq(TRUST_CANCEL_FAILED_STATE)
        );
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
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> environmentReactorFlowManager.triggerCancelCrossRealmTrust(
                        ENVIRONMENT_ID,
                        ENVIRONMENT_NAME,
                        USER_CRN,
                        ENVIRONMENT_CRN
                )
        );
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

    @Configuration
    @Import({
            FlowMetricSender.class,
            CommonMetricService.class,
            EnvironmentCrossRealmTrustCancelActions.class,
            EnvironmentCrossRealmTrustCancelHandler.class,
            EnvironmentValidateCrossRealmTrustCancelHandler.class,
            EnvironmentCrossRealmTrustCancelConfigRemovalHandler.class,
            EnvironmentCrossRealmTrustEntityDeleteHandler.class,
            EnvironmentCrossRealmTrustCancelSaltUpdateHandler.class,
            WebApplicationExceptionMessageExtractor.class,
            EnvironmentCrossRealmTrustCancelFlowConfig.class,
            EnvironmentReactorFlowManager.class,
            TransactionService.class,
            TransactionMetricsService.class,
            EnvironmentMetricService.class,
            Clock.class,
            EventSender.class,
            EntitlementService.class,
            EnvironmentService.class
    })
    @ComponentScan(basePackages = {
            "com.sequenceiq.flow",
    })
    static class Config {

        @MockBean
        EnvironmentService environmentService;

        @MockBean
        PlatformParameterService platformParameterService;

        @MockBean
        EnvironmentDtoConverter environmentDtoConverter;

        @MockBean
        GrpcUmsClient grpcUmsClient;

        @MockBean
        RoleCrnGenerator roleCrnGenerator;

        @MockBean
        ExperienceConnectorService experienceConnectorService;

        @MockBean
        ManagedChannelWrapper channelWrapper;

        @MockBean
        UmsClientConfig umsClientConfig;

        @MockBean
        private FlowLogRepository flowLogRepository;

        @MockBean
        private ApplicationFlowInformation applicationFlowInformation;

        @MockBean
        private FlowChainLogRepository flowChainLogRepository;

        @MockBean
        private EnvironmentMetricService environmentMetricService;

        @MockBean
        private OwnerAssignmentService ownerAssignmentService;

        @MockBean
        private WebSocketNotificationService webSocketNotificationService;

        @MockBean
        private Client client;

        @MockBean
        private SecretService secretService;

        @MockBean
        private FreeIpaV1Endpoint freeIpaV1Endpoint;

        @MockBean
        private TransactionalScheduler scheduler;

        @MockBean
        private FlowOperationStatisticsService flowOperationStatisticsService;

        @MockBean
        private CcmResourceTerminationListener ccmResourceTerminationListener;

        @MockBean
        private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

        @MockBean
        private FlowOperationStatisticsPersister flowOperationStatisticsPersister;

        @Bean
        public EventBus reactor(ExecutorService threadPoolExecutor) {
            return EventBus.builder()
                    .executor(threadPoolExecutor)
                    .exceptionHandler((exception, context) -> {
                    })
                    .unhandledEventHandler(event -> {
                    })
                    .build();
        }

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any())).thenReturn(
                    Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}

