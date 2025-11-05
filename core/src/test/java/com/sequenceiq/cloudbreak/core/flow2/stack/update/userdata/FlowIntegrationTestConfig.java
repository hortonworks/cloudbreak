package com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata;

import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.client.Client;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.CloudbreakFlowInformation;
import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.cloudbreak.core.flow2.service.CbEventParameterFactory;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.notification.WebSocketNotificationService;

@Profile("integration-test")
@TestConfiguration
@ComponentScan(basePackages = {
        "com.sequenceiq.flow",
})
@Import({
        FlowEventCommonListener.class,
        FlowMetricSender.class,
        CommonMetricService.class,
        TransactionService.class,
        TransactionMetricsService.class,
        CloudbreakMetricService.class,
        Clock.class,
        CbEventParameterFactory.class,
        ReactorNotifier.class})
public class FlowIntegrationTestConfig {

    @MockBean
    private FlowLogRepository flowLogRepository;

    @MockBean
    private FlowChainLogRepository flowChainLogRepository;

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
    private ClusterService clusterService;

    @MockBean
    private CloudbreakFlowInformation cloudbreakFlowInformation;

    @MockBean
    private StackStatusFinalizer stackStatusFinalizer;

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

