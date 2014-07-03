package com.sequenceiq.cloudbreak.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;

@Configuration
public class ReactorConfig {

    public static final String CF_STACK_COMPLETED_EVENT = "CF_STACK_COMPLETED";
    public static final String CLUSTER_REQUESTED_EVENT = "CLUSTER_REQUESTED";
    public static final String AMBARI_STARTED_EVENT = "AMBARI_STARTED";
    public static final String STACK_CREATE_SUCCESS_EVENT = "STACK_CREATE_SUCCESS";
    public static final String STACK_CREATE_FAILED_EVENT = "STACK_CREATE_FAILED";
    public static final String CLUSTER_CREATE_SUCCESS_EVENT = "CLUSTER_CREATE_SUCCESS";
    public static final String CLUSTER_CREATE_FAILED_EVENT = "CLUSTER_CREATE_FAILED";

    public static final String PROVISION_REQUEST_EVENT = "PROVISION_REQUEST";
    public static final String PROVISION_SETUP_COMPLETE_EVENT = "PROVISION_SETUP_COMPLETE";
    public static final String STACK_CREATE_COMPLETE_EVENT = "STACK_CREATE_COMPLETE";
    public static final String PROVISION_SUCCESS_EVENT = "PROVISION_SUCCESS";

    @Bean
    public Environment env() {
        return new Environment();
    }

    @Bean
    public Reactor reactor(Environment env) {
        return Reactors.reactor()
                .env(env)
                .dispatcher(Environment.THREAD_POOL)
                .get();
    }
}
