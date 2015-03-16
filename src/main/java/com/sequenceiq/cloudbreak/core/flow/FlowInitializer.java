package com.sequenceiq.cloudbreak.core.flow;

import static reactor.event.selector.Selectors.$;

import java.util.Map;

import javax.annotation.Resource;

import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStopHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStopHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariRoleAllocationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterCreationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.MetadataSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackCreationFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackTerminationFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackTerminationHandler;

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
        CLUSTER_CREATION,
        STACK_CREATION_FAILED,
        STACK_START,
        STACK_START_FAILED,
        STACK_STOP,
        STACK_STOP_FAILED,
        CLUSTER_START,
        CLUSTER_START_FAILED,
        CLUSTER_STOP,
        CLUSTER_STOP_FAILED,
        TERMINATION,
        TERMINATION_FAILED,
        SUCCESS
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
                .createTransition(Phases.PROVISIONING_SETUP.name(), Phases.PROVISIONING.name(), Phases.STACK_CREATION_FAILED.name()));

        flowManager.registerTransition(ProvisioningHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.PROVISIONING.name(), Phases.METADATA_SETUP.name(), Phases.STACK_CREATION_FAILED.name()));

        flowManager.registerTransition(MetadataSetupHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.METADATA_SETUP.name(), Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.STACK_CREATION_FAILED.name()));

        flowManager.registerTransition(AmbariRoleAllocationHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.AMBARI_START.name(), Phases.STACK_CREATION_FAILED.name()));

        flowManager.registerTransition(AmbariStartHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.AMBARI_START.name(), Phases.CLUSTER_CREATION.name(), Phases.STACK_CREATION_FAILED.name()));

        flowManager.registerTransition(StackTerminationHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.TERMINATION.name(), Phases.SUCCESS.name(), Phases.TERMINATION_FAILED.name()));

        flowManager.registerTransition(StackStartHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.STACK_START.name(), Phases.CLUSTER_START.name(), Phases.STACK_START_FAILED.name()));

        flowManager.registerTransition(ClusterStopHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.CLUSTER_STOP.name(), Phases.STACK_STOP.name(), Phases.CLUSTER_STOP_FAILED.name()));

        flowManager.registerTransition(ClusterStartHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.CLUSTER_START.name(), Phases.SUCCESS.name(), Phases.CLUSTER_START_FAILED.name()));

        flowManager.registerTransition(StackStopHandler.class, ReactorFlowManager.TransitionFactory
                .createTransition(Phases.STACK_STOP.name(), Phases.SUCCESS.name(), Phases.STACK_STOP_FAILED.name()));

        reactor.on($(Phases.PROVISIONING_SETUP.name()), getHandlerForClass(ProvisioningSetupHandler.class));
        reactor.on($(Phases.PROVISIONING.name()), getHandlerForClass(ProvisioningHandler.class));
        reactor.on($(Phases.METADATA_SETUP.name()), getHandlerForClass(MetadataSetupHandler.class));
        reactor.on($(Phases.AMBARI_ROLE_ALLOCATION.name()), getHandlerForClass(AmbariRoleAllocationHandler.class));
        reactor.on($(Phases.AMBARI_START.name()), getHandlerForClass(AmbariStartHandler.class));
        reactor.on($(Phases.CLUSTER_CREATION.name()), getHandlerForClass(ClusterCreationHandler.class));
        reactor.on($(Phases.TERMINATION.name()), getHandlerForClass(StackTerminationHandler.class));

        reactor.on($(Phases.STACK_START.name()), getHandlerForClass(StackStartHandler.class));
        reactor.on($(Phases.STACK_STOP.name()), getHandlerForClass(StackStopHandler.class));
        reactor.on($(Phases.CLUSTER_START.name()), getHandlerForClass(ClusterStartHandler.class));
        reactor.on($(Phases.CLUSTER_STOP.name()), getHandlerForClass(ClusterStopHandler.class));
        reactor.on($(Phases.STACK_CREATION_FAILED.name()), getHandlerForClass(StackCreationFailureHandler.class));
        reactor.on($(Phases.TERMINATION_FAILED.name()), getHandlerForClass(StackTerminationFailureHandler.class));
        reactor.on($(Phases.STACK_STOP_FAILED.name()), getHandlerForClass(StackStatusUpdateFailureHandler.class));
        reactor.on($(Phases.STACK_START_FAILED.name()), getHandlerForClass(StackStatusUpdateFailureHandler.class));
        reactor.on($(Phases.CLUSTER_STOP_FAILED.name()), getHandlerForClass(ClusterStatusUpdateFailureHandler.class));
        reactor.on($(Phases.CLUSTER_START_FAILED.name()), getHandlerForClass(ClusterStatusUpdateFailureHandler.class));


    }

    private Consumer<Event<?>> getHandlerForClass(Class handlerClass) {
        FlowHandler handler = null;
        if (flowHandlersMap.containsKey(handlerClass)) {
            handler = flowHandlersMap.get(handlerClass);
        } else {
            throw new IllegalStateException("No registered handler found for " + handlerClass + " Check your configuration");
        }
        return (Consumer<Event<?>>) handler;
    }
}
