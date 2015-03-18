package com.sequenceiq.cloudbreak.conf;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.UpdateAmbariHostsFailureHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.UpdateAmbariHostsRequestHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.UpdateAmbariHostsSuccessHandler;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.AmbariRoleAllocationCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.MetadataSetupCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionSetupCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackUpdateFailureHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.UpdateAllowedSubnetSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.UpdateAllowedSubnetsHandler;

import reactor.core.Reactor;

@Component
public class ReactorInitializer implements InitializingBean {

    @Autowired
    private ClusterRequestHandler clusterRequestHandler;

//    @Autowired
//    private StackCreationFailureHandler stackCreationFailureHandler;

    @Autowired
    private StackCreationSuccessHandler stackCreationSuccessHandler;

    @Autowired
    private ClusterCreationFailureHandler clusterCreationFailureHandler;

    @Autowired
    private ClusterCreationSuccessHandler clusterCreationSuccessHandler;

    @Autowired
    private ProvisionRequestHandler provisionRequestHandler;

    @Autowired
    private ProvisionSetupCompleteHandler provisionSetupCompleteHandler;

    @Autowired
    private ProvisionCompleteHandler provisionCompleteHandler;

    @Autowired
    private MetadataSetupCompleteHandler metadataSetupCompleteHandler;

    @Autowired
    private AmbariRoleAllocationCompleteHandler ambariRoleAllocationCompleteHandler;

    @Autowired
    private StackUpdateFailureHandler stackUpdateFailureHandler;

    @Autowired
    private UpdateAmbariHostsRequestHandler updateAmbariHostsRequestHandler;

    @Autowired
    private UpdateAmbariHostsFailureHandler updateAmbariHostsFailureHandler;

    @Autowired
    private UpdateAmbariHostsSuccessHandler updateAmbariHostsSuccessHandler;

    @Autowired
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Autowired
    private UpdateAllowedSubnetsHandler updateAllowedSubnetsHandler;

    @Autowired
    private UpdateAllowedSubnetSuccessHandler updateAllowedSubnetSuccessHandler;

    @Autowired
    private Reactor reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on($(ReactorConfig.PROVISION_REQUEST_EVENT), provisionRequestHandler);
        reactor.on($(ReactorConfig.PROVISION_SETUP_COMPLETE_EVENT), provisionSetupCompleteHandler);
        reactor.on($(ReactorConfig.PROVISION_COMPLETE_EVENT), provisionCompleteHandler);
        reactor.on($(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT), metadataSetupCompleteHandler);
        reactor.on($(ReactorConfig.AMBARI_ROLE_ALLOCATION_COMPLETE_EVENT), ambariRoleAllocationCompleteHandler);
        reactor.on($(ReactorConfig.STACK_CREATE_SUCCESS_EVENT), stackCreationSuccessHandler);
//        reactor.on($(ReactorConfig.STACK_CREATE_FAILED_EVENT), stackCreationFailureHandler);

        reactor.on($(ReactorConfig.CLUSTER_REQUESTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.AMBARI_STARTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT), clusterCreationSuccessHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT), clusterCreationFailureHandler);

        reactor.on($(ReactorConfig.UPDATE_SUBNET_REQUEST_EVENT), updateAllowedSubnetsHandler);
        reactor.on($(ReactorConfig.UPDATE_SUBNET_COMPLETE_EVENT), updateAllowedSubnetSuccessHandler);
        reactor.on($(ReactorConfig.STACK_UPDATE_FAILED_EVENT), stackUpdateFailureHandler);

        reactor.on($(ReactorConfig.UPDATE_AMBARI_HOSTS_REQUEST_EVENT), updateAmbariHostsRequestHandler);
        reactor.on($(ReactorConfig.UPDATE_AMBARI_HOSTS_SUCCESS_EVENT), updateAmbariHostsSuccessHandler);
        reactor.on($(ReactorConfig.UPDATE_AMBARI_HOSTS_FAILED_EVENT), updateAmbariHostsFailureHandler);

        reactor.on($(ReactorConfig.CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

}
