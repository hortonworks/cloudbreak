package com.sequenceiq.cloudbreak.conf;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;

import com.sequenceiq.cloudbreak.service.aws.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.aws.Ec2InstanceRunner;

@Configuration
public class ReactorConfig {

    public static final String CF_STACK_COMPLETED_EVENT = "CF_STACK_COMPLETED";
    public static final String CLUSTER_REQUESTED_EVENT = "CLUSTER_REQUESTED";
    public static final String AMBARI_STARTED_EVENT = "AMBARI_STARTED";

    @Autowired
    private Ec2InstanceRunner ec2InstanceRunner;

    @Autowired
    private ClusterRequestHandler clusterRequestHandler;

    @Bean
    public Environment env() {
        return new Environment();
    }

    @Bean
    public Reactor createReactor(Environment env) {
        Reactor reactor = Reactors.reactor()
                .env(env)
                .dispatcher(Environment.THREAD_POOL)
                .get();
        reactor.on($(CF_STACK_COMPLETED_EVENT), ec2InstanceRunner);
        reactor.on($(CLUSTER_REQUESTED_EVENT), clusterRequestHandler);
        reactor.on($(AMBARI_STARTED_EVENT), clusterRequestHandler);
        return reactor;
    }
}
