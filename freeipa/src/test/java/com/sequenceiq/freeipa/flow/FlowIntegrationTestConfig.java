package com.sequenceiq.freeipa.flow;

import java.util.concurrent.ExecutorService;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import com.sequenceiq.cloudbreak.common.metrics.CommonMetricService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.listener.FlowEventCommonListener;
import com.sequenceiq.flow.core.metrics.FlowMetricSender;
import com.sequenceiq.freeipa.metrics.FreeIpaMetricService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaEventParameterFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

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
        FreeIpaFlowManager.class,
        ErrorHandlerAwareReactorEventFactory.class})
public class FlowIntegrationTestConfig {

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
