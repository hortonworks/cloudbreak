package com.sequenceiq.cloudbreak.cloud.reactor.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.cloudbreak.cloud.handler.ConsumerNotFoundHandler

import reactor.Environment
import reactor.bus.EventBus
import reactor.bus.spec.EventBusSpec
import reactor.core.dispatch.ThreadPoolExecutorDispatcher
import reactor.fn.timer.Timer

@Configuration
class EventBusConfig {

    @Value("${cb.eventbus.threadpool.core.size:}")
    private val eventBusThreadPoolSize: Int = 0

    @Bean
    fun timer(env: Environment): Timer {
        return env.timer
    }

    @Bean
    fun env(): Environment {
        return Environment.initializeIfEmpty()
    }

    @Bean
    fun reactor(env: Environment): EventBus {
        return EventBusSpec().env(env).dispatcher(eventBusDispatcher).traceEventPath().consumerNotFoundHandler(ConsumerNotFoundHandler()).get()
    }

    private val eventBusDispatcher: ThreadPoolExecutorDispatcher
        get() = ThreadPoolExecutorDispatcher(eventBusThreadPoolSize, eventBusThreadPoolSize, "reactorDispatcher")
}
