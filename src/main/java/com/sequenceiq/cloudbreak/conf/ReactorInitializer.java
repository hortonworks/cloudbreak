package com.sequenceiq.cloudbreak.conf;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;

import com.sequenceiq.cloudbreak.service.cluster.ClusterCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.StackCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.stack.StackCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.aws.Ec2InstanceRunner;

@Component
public class ReactorInitializer implements InitializingBean {

    @Autowired
    private Ec2InstanceRunner ec2InstanceRunner;

    @Autowired
    private ClusterRequestHandler clusterRequestHandler;

    @Autowired
    private StackCreationFailureHandler stackCreationFailureHandler;

    @Autowired
    private StackCreationSuccessHandler stackCreationSuccessHandler;

    @Autowired
    private ClusterCreationFailureHandler clusterCreationFailureHandler;

    @Autowired
    private ClusterCreationSuccessHandler clusterCreationSuccessHandler;

    @Autowired
    private Reactor reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on($(ReactorConfig.CF_STACK_COMPLETED_EVENT), ec2InstanceRunner);
        reactor.on($(ReactorConfig.CLUSTER_REQUESTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.AMBARI_STARTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.STACK_CREATE_SUCCESS_EVENT), stackCreationSuccessHandler);
        reactor.on($(ReactorConfig.STACK_CREATE_FAILED_EVENT), stackCreationFailureHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT), clusterCreationSuccessHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT), clusterCreationFailureHandler);
    }

}
