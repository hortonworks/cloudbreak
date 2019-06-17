package com.sequenceiq.flow.reactor.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusConfig.class);

    @Value("${cb.eventbus.threadpool.core.size:100}")
    private int eventBusThreadPoolCoreSize;

    @Value("${cb.eventbus.threadpool.max.size:150}")
    private int eventBusThreadPoolMaxSize;

    @Value("${cb.eventbus.threadpool.backlog.size:1000}")
    private int eventBusThreadPoolBacklogSize;

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
        MDCCleanerThreadPoolExecutor executorService = new MDCCleanerThreadPoolExecutor(eventBusThreadPoolCoreSize,
                eventBusThreadPoolMaxSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(eventBusThreadPoolBacklogSize),
                new NamedDaemonThreadFactory("reactorDispatcher", context),
                (r, executor) -> LOGGER.error("Task has been rejected from 'reactorDispatcher' threadpool. Executor state: " + executor));
        return new ThreadPoolExecutorDispatcher(eventBusThreadPoolBacklogSize, eventBusThreadPoolCoreSize, executorService);
    }
}
