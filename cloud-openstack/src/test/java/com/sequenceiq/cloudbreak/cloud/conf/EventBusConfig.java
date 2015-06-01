package com.sequenceiq.cloudbreak.cloud.conf;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.core.dispatch.ThreadPoolExecutorDispatcher;
import reactor.fn.timer.Timer;

@Configuration
public class EventBusConfig {

    @Value("${cb.eventbus.threadpool.core.size:10}")
    private int eventBusThreadPoolSize;

    @Bean
    public ListeningScheduledExecutorService listeningScheduledExecutorService() {
        return MoreExecutors.listeningDecorator(new ScheduledThreadPoolExecutor(10));
    }

    @Bean
    public Environment env() {
        return Environment.initializeIfEmpty().setDispatcher(Environment.THREAD_POOL, getEventBusDispatcher()).assignErrorJournal();
    }

    @Bean
    public Timer timer(Environment env) {
        return env.getTimer();
    }

    @Bean
    public EventBus eventBus(Environment env) {
        return EventBus.create(env, Environment.THREAD_POOL);
    }

    private ThreadPoolExecutorDispatcher getEventBusDispatcher() {
        return new ThreadPoolExecutorDispatcher(eventBusThreadPoolSize, eventBusThreadPoolSize, "cloud");
    }
}
