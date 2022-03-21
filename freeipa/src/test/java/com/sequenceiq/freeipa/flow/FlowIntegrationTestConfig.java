package com.sequenceiq.freeipa.flow;

import javax.ws.rs.client.Client;

import org.quartz.Scheduler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.reactor.eventbus.ConsumerCheckerEventBus;
import com.sequenceiq.flow.reactor.handler.ConsumerNotFoundHandler;
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

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;
import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.core.dispatch.MpscDispatcher;

@Profile("integration-test")
@TestConfiguration
@ComponentScan(basePackages = {
        "com.sequenceiq.flow",
})
@Import({
        TransactionService.class,
        TransactionMetricsService.class,
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
    private Scheduler scheduler;

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

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        final Reporter reporter = new InMemoryReporter();
        final Sampler sampler = new ConstSampler(false);
        return new JaegerTracer.Builder("untraced-service")
                .withReporter(reporter)
                .withSampler(sampler)
                .build();
    }

    @Bean
    public EventBus reactor(MDCCleanerThreadPoolExecutor threadPoolExecutor, Environment env) {
        MpscDispatcher dispatcher = new MpscDispatcher("test-dispatcher");
        EventBus eventBus = new EventBusSpec()
                .env(env)
                .dispatcher(dispatcher)
                .traceEventPath()
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();
        return new ConsumerCheckerEventBus(eventBus);
    }
}
