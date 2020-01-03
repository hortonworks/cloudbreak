package com.sequenceiq.flow.reactor.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.reactor.handler.ConsumerNotFoundHandler;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.spec.EventBusSpec;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.core.support.Exceptions;
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

    @Inject
    @Lazy
    private ApplicationFlowInformation applicationFlowInformation;

    @Inject
    @Lazy
    private FlowLogDBService flowLogDBService;

    @Bean
    public Timer timer(Environment env) {
        return env.getTimer();
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty();
    }

    @Bean
    public EventBus reactor(MDCCleanerThreadPoolExecutor threadPoolExecutor, Environment env) {
        return new EventBusSpec()
                .env(env)
                .dispatcher(new ThreadPoolExecutorDispatcher(eventBusThreadPoolBacklogSize, eventBusThreadPoolCoreSize, threadPoolExecutor))
                .traceEventPath()
                .dispatchErrorHandler(throwable -> {
                    handleFlowFail(throwable);
                    LOGGER.error("Exception happened in dispatcher", throwable);
                })
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();
    }

    private void handleFlowFail(Throwable throwable) {
        if (throwable.getCause() instanceof Exceptions.ValueCause) {
            try {
                if (((Exceptions.ValueCause) throwable.getCause()).getValue() instanceof Event) {
                    Event event = (Event) ((Exceptions.ValueCause) throwable.getCause()).getValue();
                    LOGGER.info("Failed event: {}", event);
                    Event.Headers headers = event.getHeaders();
                    if (headers != null) {
                        LOGGER.info("Failed event headers: {}", headers);
                        if (headers.get("FLOW_ID") != null) {
                            String flowId = headers.get("FLOW_ID").toString();
                            LOGGER.error("Unhandled exception happened in flow {}, lets cancel it", flowId, throwable);
                            flowLogDBService.getLastFlowLog(flowId).ifPresent(flowLog -> {
                                flowLogDBService.updateLastFlowLogStatus(flowLog, true);
                                applicationFlowInformation.handleFlowFail(flowLog);
                            });
                        }
                    } else {
                        LOGGER.info("Headers is null object for the event {}", event);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("can't handle flow fail", e);
            }
        }
    }

    @Bean("eventBusThreadPoolExecutor")
    public MDCCleanerThreadPoolExecutor getPoolExecutor() {
        return new MDCCleanerThreadPoolExecutor(eventBusThreadPoolCoreSize,
                eventBusThreadPoolMaxSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(eventBusThreadPoolBacklogSize),
                new NamedDaemonThreadFactory("reactorDispatcher"),
                (r, executor) -> LOGGER.error("Task has been rejected from 'reactorDispatcher' threadpool. Executor state: " + executor));

    }
}
