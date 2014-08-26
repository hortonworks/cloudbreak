package com.sequenceiq.cloudbreak.conf;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.Reactor;

import com.sequenceiq.cloudbreak.service.cluster.handler.AddAmbariHostsFailureHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.AddAmbariHostsRequestHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.AddAmbariHostsSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.cluster.handler.ClusterRequestHandler;
import com.sequenceiq.cloudbreak.service.history.HistoryEventHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.AddInstancesCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.AmbariRoleAllocationCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.MetadataSetupCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.MetadataUpdateCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.ProvisionSetupCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackCreationFailureHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackCreationSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackDeleteCompleteHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackDeleteRequestHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackUpdateFailureHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.StackUpdateSuccessHandler;
import com.sequenceiq.cloudbreak.service.stack.handler.UpdateInstancesRequestHandler;

@Component
public class ReactorInitializer implements InitializingBean {

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
    private StackDeleteCompleteHandler stackDeleteCompleteHandler;

    @Autowired
    private StackDeleteRequestHandler stackDeleteRequestHandler;

    @Autowired
    private UpdateInstancesRequestHandler updateInstancesRequestHandler;

    @Autowired
    private AddInstancesCompleteHandler addInstancesCompleteHandler;

    @Autowired
    private MetadataUpdateCompleteHandler metadataUpdateCompleteHandler;

    @Autowired
    private StackUpdateSuccessHandler stackUpdateSuccessHandler;

    @Autowired
    private StackUpdateFailureHandler stackUpdateFailureHandler;

    @Autowired
    private AddAmbariHostsRequestHandler addAmbariHostsRequestHandler;

    @Autowired
    private AddAmbariHostsFailureHandler addAmbariHostsFailureHandler;

    @Autowired
    private AddAmbariHostsSuccessHandler addAmbariHostsSuccessHandler;

    @Autowired
    private HistoryEventHandler historyEventHandler;

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
        reactor.on($(ReactorConfig.STACK_CREATE_FAILED_EVENT), stackCreationFailureHandler);
        reactor.on($(ReactorConfig.DELETE_COMPLETE_EVENT), stackDeleteCompleteHandler);
        reactor.on($(ReactorConfig.DELETE_REQUEST_EVENT), stackDeleteRequestHandler);

        reactor.on($(ReactorConfig.CLUSTER_REQUESTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.AMBARI_STARTED_EVENT), clusterRequestHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_SUCCESS_EVENT), clusterCreationSuccessHandler);
        reactor.on($(ReactorConfig.CLUSTER_CREATE_FAILED_EVENT), clusterCreationFailureHandler);

        reactor.on($(ReactorConfig.UPDATE_INSTANCES_REQUEST_EVENT), updateInstancesRequestHandler);
        reactor.on($(ReactorConfig.ADD_INSTANCES_COMPLETE_EVENT), addInstancesCompleteHandler);
        reactor.on($(ReactorConfig.METADATA_UPDATE_COMPLETE_EVENT), metadataUpdateCompleteHandler);
        reactor.on($(ReactorConfig.STACK_UPDATE_SUCCESS_EVENT), stackUpdateSuccessHandler);
        reactor.on($(ReactorConfig.STACK_UPDATE_FAILED_EVENT), stackUpdateFailureHandler);

        reactor.on($(ReactorConfig.ADD_AMBARI_HOSTS_REQUEST_EVENT), addAmbariHostsRequestHandler);
        reactor.on($(ReactorConfig.ADD_AMBARI_HOSTS_SUCCESS_EVENT), addAmbariHostsSuccessHandler);
        reactor.on($(ReactorConfig.ADD_AMBARI_HOSTS_FAILED_EVENT), addAmbariHostsFailureHandler);

        reactor.on($(ReactorConfig.HISTORY_EVENT), historyEventHandler);
    }

}
