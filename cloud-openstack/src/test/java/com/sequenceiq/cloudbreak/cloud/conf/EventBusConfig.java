package com.sequenceiq.cloudbreak.cloud.conf;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sequenceiq.cloudbreak.cloud.handler.ConsumerNotFoundHandler;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.fn.timer.Timer;

@Configuration
public class EventBusConfig {

    @Value("${cb.eventbus.threadpool.core.size:10}")
    private int eventBusThreadPoolSize;

    @Bean
    public ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(eventBusThreadPoolSize));
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public Timer timer(Environment env) {
        return env.getTimer();
    }

    @Bean
    public EventBus eventBus(Environment env) {
        EventBus bus = new EventBusSpec()
                .env(env)
                .defaultDispatcher()
                .traceEventPath()
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();
        return bus;
    }

}
