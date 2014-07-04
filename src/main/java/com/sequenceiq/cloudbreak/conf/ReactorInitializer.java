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
import com.sequenceiq.cloudbreak.service.stack.handler.MetadataSetupCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.MetadataSetupContext;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionSetupContext;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackCreationContext;

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
    private ProvisionSetupContext provisionSetupContext;

    @Autowired
    private StackCreationContext stackCreationContext;

    @Autowired
    private MetadataSetupContext metadataSetupContext;

    @Autowired
    private MetadataSetupCompleteHandler metadataSetupCompleteHandler;

    @Autowired
    private ProvisionSuccessHandler provisionSuccessHandler;

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

        reactor.on($(ReactorConfig.PROVISION_REQUEST_EVENT), provisionSetupContext);
        reactor.on($(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT), stackCreationContext);
        reactor.on($(ReactorConfig.STACK_CREATE_COMPLETE_EVENT), metadataSetupContext);
        reactor.on($(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT), metadataSetupCompleteHandler);
        reactor.on($(ReactorConfig.PROVISION_SUCCESS_EVENT), provisionSuccessHandler);

    }

}
