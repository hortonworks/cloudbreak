package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.dispatch.ThreadPoolExecutorDispatcher;

@Configuration
public class ReactorConfig {
    public static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

    @Value("${cb.reactor.threadpool.core.size:100}")
    private int reactorThreadPoolSize;

    @Bean
    public Environment env() {
        return new Environment();
    }

    @Bean
    public Reactor reactor(Environment env) {
        return Reactors.reactor()
                .env(env)
                .dispatcher(getReactorDispatcher())
                .get();
    }

    private ThreadPoolExecutorDispatcher getReactorDispatcher() {
        return new ThreadPoolExecutorDispatcher(reactorThreadPoolSize, reactorThreadPoolSize);
    }
}
