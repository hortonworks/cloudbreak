package com.sequenceiq.flow.reactor.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.ConcurrencyLimitDecorator;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerDecorator;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Configuration
public class EventBusConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusConfig.class);

    @Value("${cb.eventbus.threadpool.core.size:100}")
    private int eventBusThreadPoolCoreSize;

    @Value("${cb.eventbus.threadpool.max.size:150}")
    private int eventBusThreadPoolMaxSize;

    @Value("${cb.eventbus.threadpool.backlog.size:1000}")
    private int eventBusThreadPoolBacklogSize;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean virtualThreadsAvailable;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Inject
    @Lazy
    private FlowLogDBService flowLogDBService;

    private String generateStackTrace() {
        return String.join("\n\t", Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @PostConstruct
    public void init() {
        if (virtualThreadsAvailable) {
            try {
                Thread virtualThread = Thread.ofVirtual().unstarted(() -> {
                });
                Field schedulerField = virtualThread.getClass().getDeclaredField("scheduler");
                schedulerField.setAccessible(true);
                Object scheduler = schedulerField.get(virtualThread);
                if (scheduler.getClass().equals(ForkJoinPool.class)) {
                    LOGGER.info("Setup metrics for virtualThreadScheduler thread pool.");
                    commonExecutorServiceFactory.monitorForkJoinPool((ForkJoinPool) scheduler, "virtualThreadScheduler");
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't setup monitoring for common virtual thread pool.", e);
            }
        }
        commonExecutorServiceFactory.monitorForkJoinPool(ForkJoinPool.commonPool(), "commonForkJoinPool");
    }

    @Bean
    public Timer timer() {
        return new Timer();
    }

    @Bean
    public EventBus reactor(@Named("eventBusThreadPoolExecutor") ExecutorService threadPoolExecutor) {
        return EventBus.builder()
                .executor(threadPoolExecutor)
                .exceptionHandler((event, exception) -> handleException(event, exception, threadPoolExecutor))
                .unhandledEventHandler(event -> handleUnhandledEvent(event, threadPoolExecutor))
                .build();
    }

    private String tryGetFlowIdFromEvent(Object event) {
        if (event instanceof Event) {
            return ((Event) event).getHeaders().get(FlowConstants.FLOW_ID);
        }
        return null;
    }

    private String getFlowIdFromMDC() {
        String flowId;
        LOGGER.info("FlowId parse failed from headers.. let's try to get it from MDC context");
        flowId = MDCBuilder.getMdcContextMap().get("flowId");
        return flowId;
    }

    @Bean("eventBusThreadPoolExecutor")
    public ExecutorService getPoolExecutor() {
        if (virtualThreadsAvailable) {
            return commonExecutorServiceFactory.newVirtualThreadExecutorService("reactorDispatcher", "eventBusThreadPoolExecutor",
                    List.of(new MDCCleanerDecorator(), new ConcurrencyLimitDecorator(eventBusThreadPoolMaxSize)));
        } else {
            return commonExecutorServiceFactory.newThreadPoolExecutorService("reactorDispatcher", "eventBusThreadPoolExecutor", eventBusThreadPoolCoreSize,
                    eventBusThreadPoolMaxSize, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(eventBusThreadPoolBacklogSize),
                    (runnable, executor) ->
                            LOGGER.error("Task has been rejected from 'reactorDispatcher' threadpool. Executor state: {}, task: {}", executor, runnable),
                    List.of(new MDCCleanerDecorator()));
        }
    }

    private void handleException(Event<?> event, Throwable exception, ExecutorService executorService) {
        try {
            LOGGER.error("Exception during event: {}", event, exception);
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                String flowId = Optional.ofNullable(tryGetFlowIdFromEvent(event))
                        .or(() -> Optional.ofNullable(getFlowIdFromMDC()))
                        .orElse(null);
                if (flowId != null) {
                    LOGGER.error("Unhandled exception happened in flow {}, lets cancel it", flowId, exception);
                    flowLogDBService.closeFlowOnError(flowId, String.format("Unhandled exception happened in flow, type: %s, message: %s",
                            exception.getClass().getName(), exception.getMessage()));
                } else {
                    LOGGER.error("We were not able to guess flowId on thread: {}", generateStackTrace());
                }
            } else {
                LOGGER.info("Dispatcher is not alive there is no need to handle flow failure.", exception);
            }
        } catch (Exception e) {
            LOGGER.error("can't handle flow fail", e);
        }
    }

    private void handleUnhandledEvent(Event<?> event, ExecutorService executorService) {
        try {
            LOGGER.error("Unhandled event: {}", event);
            if (!executorService.isShutdown() && !executorService.isTerminated()) {
                String flowId = tryGetFlowIdFromEvent(event);
                if (flowId != null) {
                    flowLogDBService.closeFlowOnError(flowId, String.format("Unhanded event: %s", event.getClass().getName()));
                } else {
                    LOGGER.error("We were not able to guess flowId on thread: {}", generateStackTrace());
                }
            } else {
                LOGGER.info("Dispatcher is not alive there is no need to handle flow failure.");
            }
        } catch (Exception e) {
            LOGGER.error("can't handle flow fail", e);
        }
    }
}
