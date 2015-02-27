package com.sequenceiq.cloudbreak.core.flow;

import static reactor.event.selector.Selectors.$;

import java.util.Map;

import javax.annotation.Resource;

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
import reactor.event.Event;
import reactor.function.Consumer;

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

    @Resource
    private Map<Class, FlowHandler> flowHandlersMap;

    @Override
    public void afterPropertiesSet() throws Exception {

        flowManager.registerTransition(ProvisioningSetupHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.PROVISIONING_SETUP.name(), Phases.PROVISIONING.name(), "PROVISIONING_SETUP_FAILED"));

        flowManager.registerTransition(ProvisioningHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.PROVISIONING.name(), Phases.METADATA_SETUP.name(), "PROVISIONING_FAILED"));

        flowManager.registerTransition(MetadataSetupHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.METADATA_SETUP.name(), Phases.AMBARI_ROLE_ALLOCATION.name(), "METADATA_SETUP_FAILED"));

        flowManager.registerTransition(AmbariRoleAllocationHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.AMBARI_START.name(), "AMBARI_ROLE_ALLOCATION_FAILED"));

        flowManager.registerTransition(AmbariStartHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.AMBARI_START.name(), Phases.CLUSTER_CREATION.name(), "AMBARI_START_FAILED"));

        reactor.on($(Phases.PROVISIONING_SETUP.name()), getHandlerForClass(ProvisioningSetupHandler.class));
        reactor.on($(Phases.PROVISIONING.name()), getHandlerForClass(ProvisioningHandler.class));
        reactor.on($(Phases.METADATA_SETUP.name()), getHandlerForClass(MetadataSetupHandler.class));
        reactor.on($(Phases.AMBARI_ROLE_ALLOCATION.name()), getHandlerForClass(AmbariRoleAllocationHandler.class));
        reactor.on($(Phases.AMBARI_START.name()), getHandlerForClass(AmbariStartHandler.class));
        reactor.on($(Phases.CLUSTER_CREATION.name()), getHandlerForClass(ClusterCreationHandler.class));

    }

    private Consumer<Event<?>> getHandlerForClass(Class handlerClass) {
        FlowHandler handler = null;
        if (flowHandlersMap.containsKey(handlerClass)) {
            handler = flowHandlersMap.get(handlerClass);
        } else {
            throw new IllegalStateException("No flowhandler found for" + handlerClass + " Check your configuration");
        }
        return (Consumer<Event<?>>) handler;
    }
}
