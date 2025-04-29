package com.sequenceiq.freeipa.flow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.FreeIpaFlowInformation;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaEventParameterFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaParallelFlowValidator;
import com.sequenceiq.notification.NotificationService;

@Profile("integration-test")
@TestConfiguration
@ComponentScan(basePackages = {
        "com.sequenceiq.flow",
})
@Import({
        TransactionService.class,
        TransactionMetricsService.class,
        FlowEventCommonListener.class,
        FlowMetricSender.class,
        CommonMetricService.class,
        FreeIpaMetricService.class,
        Clock.class,
        FreeIpaEventParameterFactory.class,
        FreeIpaFlowManager.class})
public class FlowIntegrationTestConfig {

    @MockBean
    private FlowLogRepository flowLogRepository;

    @MockBean
    private FlowChainLogRepository flowChainLogRepository;

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
    private FreeIpaService freeIpaService;

    @MockBean
    private OperationRepository operationRepository;

    @MockBean
    private FreeIpaFlowInformation freeIpaFlowInformation;

    @MockBean
    private FreeIpaParallelFlowValidator freeIpaParallelFlowValidator;

    @MockBean
    private CrnUserDetailsService crnUserDetailsService;

    @BeforeEach
    void init() {
        when(crnUserDetailsService.getUmsUser(anyString())).thenReturn(mock(CrnUser.class));
    }

    @Bean
    public EventBus reactor(ExecutorService threadPoolExecutor) {
        return EventBus.builder()
                .executor(threadPoolExecutor)
                .exceptionHandler((exception, context) -> {
                })
                .unhandledEventHandler(deadEvent -> {
                })
                .build();
    }
}
