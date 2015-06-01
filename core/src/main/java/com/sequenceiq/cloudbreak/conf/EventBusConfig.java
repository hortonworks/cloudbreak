package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_EVENTBUS_THREADPOOL_CORE_SIZE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;

@Configuration
public class EventBusConfig {
    public static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    @Value("${cb.eventbus.threadpool.core.size:" + CB_EVENTBUS_THREADPOOL_CORE_SIZE + "}")
    private int eventBusThreadPoolSize;

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public EventBus reactor(Environment env) {
        return EventBus.create(env, getEventBusDispatcher());
    }

    private ThreadPoolExecutorDispatcher getEventBusDispatcher() {
        return new ThreadPoolExecutorDispatcher(eventBusThreadPoolSize, eventBusThreadPoolSize);
    }
}
