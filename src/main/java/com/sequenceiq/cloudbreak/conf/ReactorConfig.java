package com.sequenceiq.cloudbreak.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;

@Configuration
public class ReactorConfig {

    public static final String PROVISION_REQUEST_EVENT = "PROVISION_REQUEST";
    public static final String PROVISION_SETUP_COMPLETE_EVENT = "PROVISION_SETUP_COMPLETE";
    public static final String PROVISION_COMPLETE_EVENT = "PROVISION_COMPLETE";
    public static final String METADATA_SETUP_COMPLETE_EVENT = "METADATA_SETUP_COMPLETE";
    public static final String AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT = "AMBARI_ROLE_ALLOCATION_COMPLETE";
    public static final String STACK_CREATE_SUCCESS_EVENT = "STACK_CREATE_SUCCESS";
    public static final String STACK_CREATE_FAILED_EVENT = "STACK_CREATE_FAILED";

    public static final String DELETE_REQUEST_EVENT = "DELETE_REQUEST";
    public static final String DELETE_COMPLETE_EVENT = "DELETE_COMLPETE";
    public static final String CLUSTER_REQUESTED_EVENT = "CLUSTER_REQUESTED";
    public static final String AMBARI_STARTED_EVENT = "AMBARI_STARTED";
    public static final String CLUSTER_CREATE_SUCCESS_EVENT = "CLUSTER_CREATE_SUCCESS";
    public static final String CLUSTER_CREATE_FAILED_EVENT = "CLUSTER_CREATE_FAILED";

    public static final String HISTORY_EVENT = "HISTORY_EVENT";

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
