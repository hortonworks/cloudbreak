package com.sequenceiq.flow.reactor.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.reactor.handler.ConsumerNotFoundHandler;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.core.Dispatcher;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.core.support.NamedDaemonThreadFactory;
import reactor.fn.timer.Timer;

@Configuration
public class EventBusConfig {

    @Value("${cb.eventbus.threadpool.core.size:50}")
    private int eventBusThreadPoolSize;

    @Bean
    public Timer timer(Environment env) {
        return env.getTimer();
    }

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

    private Dispatcher getEventBusDispatcher() {
        ClassLoader context = new ClassLoader(Thread.currentThread()
                .getContextClassLoader()) {
        };
        MDCCleanerThreadPoolExecutor executorService = new MDCCleanerThreadPoolExecutor(eventBusThreadPoolSize,
                eventBusThreadPoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(eventBusThreadPoolSize),
                new NamedDaemonThreadFactory("reactorDispatcher", context),
                (r, executor) -> r.run());
        return new ThreadPoolExecutorDispatcher(eventBusThreadPoolSize, eventBusThreadPoolSize, executorService);
    }
}
