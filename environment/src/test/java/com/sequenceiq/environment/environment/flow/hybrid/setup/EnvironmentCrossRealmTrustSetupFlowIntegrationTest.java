package com.sequenceiq.environment.environment.flow.hybrid.setup;


import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_VALIDATION_STATE;
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
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.flow.hybrid.setup.action.EnvironmentCrossRealmTrustSetupActions;
import com.sequenceiq.environment.environment.flow.hybrid.setup.config.EnvironmentCrossRealmTrustSetupFlowConfig;
import com.sequenceiq.environment.environment.flow.hybrid.setup.handler.EnvironmentCrossRealmTrustSetupHandler;
import com.sequenceiq.environment.environment.flow.hybrid.setup.handler.EnvironmentValidateCrossRealmTrustSetupHandler;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
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
import com.sequenceiq.notification.NotificationService;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(SpringExtension.class)
class EnvironmentCrossRealmTrustSetupFlowIntegrationTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final long ENVIRONMENT_ID = 1L;

    private static final String ENVIRONMENT_NAME = "ENVIRONMENT_NAME";

    private static final String ENVIRONMENT_CRN = "ENVIRONMENT_CRN";

    private static final String ACCOUNT_ID = "accId";

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
    private StackService stackService;

    private Environment environment;

    @BeforeEach
    public void setup() throws QuotaExceededException {
        environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setResourceCrn(ENVIRONMENT_CRN);
        environment.setName(ENVIRONMENT_NAME);
        environment.setAccountId(ACCOUNT_ID);
    }

    @Test
    void testPrepareCrossRealmTrustWhenSuccessful() {
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED),
                eq(TRUST_SETUP_VALIDATION_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_IN_PROGRESS),
                eq(ENVIRONMENT_SETUP_TRUST_STARTED),
                eq(TRUST_SETUP_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_FINISH_REQUIRED),
                eq(ENVIRONMENT_SETUP_TRUST_FINISHED),
                eq(TRUST_SETUP_FINISHED_STATE)
        );
    }

    @Test
    public void testValidationFails() {
        doThrow(new CloudbreakServiceException("Freeipa not exist on provider side"))
                .when(freeIpaService)
                .describe(anyString());
        testFlow();
        InOrder environmentStatusVerify = inOrder(environmentStatusUpdateService);

        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_VALIDATION_IN_PROGRESS),
                eq(ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED),
                eq(TRUST_SETUP_VALIDATION_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(Payload.class),
                eq(TRUST_SETUP_IN_PROGRESS),
                eq(ENVIRONMENT_SETUP_TRUST_STARTED),
                eq(TRUST_SETUP_STATE)
        );
        environmentStatusVerify.verify(environmentStatusUpdateService).updateFailedEnvironmentStatusAndNotify(
                any(CommonContext.class),
                any(BaseFailedFlowEvent.class),
                eq(TRUST_SETUP_FAILED),
                eq(ENVIRONMENT_SETUP_TRUST_FAILED),
                eq(TRUST_SETUP_FAILED_STATE)
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
        SetupCrossRealmTrustRequest setupCrossRealmTrustRequest = new SetupCrossRealmTrustRequest();
        setupCrossRealmTrustRequest.setRealm("realm");
        setupCrossRealmTrustRequest.setTrustSecret("trust-secret");
        setupCrossRealmTrustRequest.setFqdn("fqdn");
        setupCrossRealmTrustRequest.setIp("10.0.0.1");
        setupCrossRealmTrustRequest.setRemoteEnvironmentCrn("remoteenvcrn");
        return ThreadBasedUserCrnProvider.doAs(
                USER_CRN,
                () -> environmentReactorFlowManager.triggerSetupCrossRealmTrust(
                        ENVIRONMENT_ID,
                        ACCOUNT_ID,
                        ENVIRONMENT_NAME,
                        USER_CRN,
                        ENVIRONMENT_CRN,
                        setupCrossRealmTrustRequest
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
            EnvironmentCrossRealmTrustSetupActions.class,
            EnvironmentCrossRealmTrustSetupHandler.class,
            EnvironmentValidateCrossRealmTrustSetupHandler.class,
            WebApplicationExceptionMessageExtractor.class,
            EnvironmentCrossRealmTrustSetupFlowConfig.class,
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
        private NotificationService notificationService;

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