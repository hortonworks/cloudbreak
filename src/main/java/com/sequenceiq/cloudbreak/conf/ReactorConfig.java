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
    public static final String DELETE_COMPLETE_EVENT = "DELETE_COMPLETE";
    public static final String CLUSTER_REQUESTED_EVENT = "CLUSTER_REQUESTED";
    public static final String AMBARI_STARTED_EVENT = "AMBARI_STARTED";
    public static final String CLUSTER_CREATE_SUCCESS_EVENT = "CLUSTER_CREATE_SUCCESS";
    public static final String CLUSTER_CREATE_FAILED_EVENT = "CLUSTER_CREATE_FAILED";

    public static final String UPDATE_INSTANCES_REQUEST_EVENT = "UPDATE_INSTANCES_REQUEST";
    public static final String ADD_INSTANCES_COMPLETE_EVENT = "ADD_INSTANCES_COMPLETE";
    public static final String METADATA_UPDATE_COMPLETE_EVENT = "METADATA_UPDATE_COMPLETE";
    public static final String STACK_UPDATE_SUCCESS_EVENT = "STACK_UPDATE_SUCCESS";
    public static final String STACK_UPDATE_FAILED_EVENT = "STACK_UPDATE_FAILED";

    public static final String STACK_STATUS_UPDATE_EVENT = "STACK_STATUS_UPDATE_REQUEST";
    public static final String CLUSTER_STATUS_UPDATE_EVENT = "CLUSTER_STATUS_UPDATE_REQUEST";

    public static final String UPDATE_AMBARI_HOSTS_REQUEST_EVENT = "UPDATE_AMBARI_HOSTS_REQUEST";
    public static final String UPDATE_AMBARI_HOSTS_SUCCESS_EVENT = "UPDATE_AMBARI_HOSTS_SUCCESS";
    public static final String UPDATE_AMBARI_HOSTS_FAILED_EVENT = "UPDATE_AMBARI_HOSTS_FAILED";

    public static final String CLOUDBREAK_EVENT = "CLOUDBREAK_EVENT";

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
