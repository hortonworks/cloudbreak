package com.sequenceiq.cloudbreak.core.flow;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariRoleAllocationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterCreationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.MetadataSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningSetupHandler;

import reactor.core.Reactor;

@Component
public class FlowInitializer implements InitializingBean {

    public static enum Phases {
        PROVISIONING_SETUP,
        PROVISIONING,
        METADATA_SETUP,
        AMBARI_ROLE_ALLOCATION,
        AMBARI_START,
        CLUSTER_CREATION
    }

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private Reactor reactor;

    @Autowired
    private ProvisioningSetupHandler provisioningSetupHandler;

    @Autowired
    private ProvisioningHandler provisioningHandler;

    @Autowired
    private MetadataSetupHandler metadataSetupHandler;

    @Autowired
    private AmbariRoleAllocationHandler ambariRoleAllocationHandler;

    @Autowired
    private AmbariStartHandler ambariStartHandler;

    @Autowired
    private ClusterCreationHandler clusterCreationHandler;

    @Override
    public void afterPropertiesSet() throws Exception {

        flowManager.registerTransition(ProvisioningSetupHandler.class,
                ReactorFlowManager.TransitionFactory
                        .createTransition(Phases.PROVISIONING_SETUP.name(), Phases.PROVISIONING.name(), "PROVISIONING_SETUP_FAILED"));
        flowManager.registerTransition(ProvisioningHandler.class,
                ReactorFlowManager.TransitionFactory.createTransition(Phases.PROVISIONING.name(), Phases.METADATA_SETUP.name(), "PROVISIONING_FAILED"));
        flowManager.registerTransition(MetadataSetupHandler.class,
                ReactorFlowManager.TransitionFactory
                        .createTransition(Phases.METADATA_SETUP.name(), Phases.AMBARI_ROLE_ALLOCATION.name(), "METADATA_SETUP_FAILED"));
        flowManager.registerTransition(AmbariRoleAllocationHandler.class,
                ReactorFlowManager.TransitionFactory
                        .createTransition(Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.AMBARI_START.name(), "AMBARI_ROLE_ALLOCATION_FAILED"));
        flowManager.registerTransition(AmbariStartHandler.class,
                ReactorFlowManager.TransitionFactory
                        .createTransition(Phases.AMBARI_START.name(), Phases.CLUSTER_CREATION.name(), "AMBARI_START_FAILED"));

        reactor.on($(Phases.PROVISIONING_SETUP.name()), provisioningSetupHandler);
        reactor.on($(Phases.PROVISIONING.name()), provisioningHandler);
        reactor.on($(Phases.METADATA_SETUP.name()), metadataSetupHandler);
        reactor.on($(Phases.AMBARI_ROLE_ALLOCATION.name()), ambariRoleAllocationHandler);
        reactor.on($(Phases.AMBARI_START.name()), ambariStartHandler);
        reactor.on($(Phases.CLUSTER_CREATION.name()), clusterCreationHandler);

    }
}
