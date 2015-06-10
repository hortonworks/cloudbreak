package com.sequenceiq.cloudbreak.reactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.handler.ConsumerNotFoundHandler;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;

@Configuration
public class EventBusConfig {

    @Value("${cb.eventbus.threadpool.core.size:100}")
    private int eventBusThreadPoolSize;

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public EventBus reactor(Environment env) {
        return new EventBusSpec()
                .env(env)
                .dispatcher(getEventBusDispatcher())
                .traceEventPath()
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();

    }

    private ThreadPoolExecutorDispatcher getEventBusDispatcher() {
        return new ThreadPoolExecutorDispatcher(eventBusThreadPoolSize, eventBusThreadPoolSize);
    }
}
