package com.sequenceiq.flow.reactor.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.concurrent.MDCCleanerThreadPoolExecutor;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.reactor.eventbus.ConsumerCheckerEventBus;
import com.sequenceiq.flow.reactor.eventbus.EventCanNotBeDeliveredException;
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

    private void handleFlowFail(Throwable throwable) {
        try {
            String flowId = getFlowIdFromThrowable(throwable);
            if (flowId == null) {
                flowId = getFlowIdFromMDC();
            }
            if (flowId != null) {
                LOGGER.error("Unhandled exception happened in flow {}, lets cancel it", flowId, throwable);
                flowLogDBService.getLastFlowLog(flowId).ifPresent(flowLog -> {
                    applicationFlowInformation.handleFlowFail(flowLog);
                    flowLogDBService.updateLastFlowLogStatus(flowLog, true);
                    flowLogDBService.finalize(flowLog.getFlowId());
                });
            } else {
                LOGGER.error("We were not able to guess flowId on thread: {}", generateStackTrace());
            }
        } catch (Exception e) {
            LOGGER.error("can't handle flow fail", e);
        }
    }

    private String generateStackTrace() {
        return String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private String getFlowIdFromMDC() {
        String flowId;
        LOGGER.info("FlowId parse failed from headers.. let's try to get it from MDC context");
        flowId = MDCBuilder.getMdcContextMap().get("flowId");
        return flowId;
    }

    private String getFlowIdFromThrowable(Throwable throwable) {
        try {
            if (throwable.getCause() instanceof Exceptions.ValueCause) {
                if (((Exceptions.ValueCause) throwable.getCause()).getValue() instanceof Event) {
                    Event event = (Event) ((Exceptions.ValueCause) throwable.getCause()).getValue();
                    LOGGER.info("Failed event: {}", event);
                    return getFlowIdFromEventHeaders(event);
                }
            } else if (throwable instanceof EventCanNotBeDeliveredException) {
                Event event = ((EventCanNotBeDeliveredException) throwable).getEvent();
                return getFlowIdFromEventHeaders(event);
            }
        } catch (Exception e) {
            LOGGER.error("Something wrong happened when we tried to get flowId from headers", e);
        }
        return null;
    }

    private String getFlowIdFromEventHeaders(Event event) {
        Event.Headers headers = event.getHeaders();
        if (headers != null) {
            LOGGER.info("Failed event headers: {}", headers);
            if (headers.get("FLOW_ID") != null) {
                return headers.get("FLOW_ID").toString();
            }
        } else {
            LOGGER.info("Headers is null object for the event {}", event);
        }
        return null;
    }

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
        EventBus eventBus = new EventBusSpec()
                .env(env)
                .dispatcher(new ThreadPoolExecutorDispatcher(eventBusThreadPoolBacklogSize, eventBusThreadPoolCoreSize, threadPoolExecutor))
                .traceEventPath()
                .dispatchErrorHandler(throwable -> {
                    handleFlowFail(throwable);
                    LOGGER.error("Exception happened in dispatcher", throwable);
                })
                .uncaughtErrorHandler(throwable -> {
                    handleFlowFail(throwable);
                    LOGGER.error("Uncaught exception happened", throwable);
                })
                .consumerNotFoundHandler(new ConsumerNotFoundHandler())
                .get();
        return new ConsumerCheckerEventBus(eventBus);
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
