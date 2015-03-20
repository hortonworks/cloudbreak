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
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterDownscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStopHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterUpscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.MetadataSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackCreationFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackDownscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStopHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackTerminationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackUpscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.UpdateAllowedSubnetsHandler;

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
        STACK_UPSCALE,
        STACK_DOWNSCALE,
        CLUSTER_START,
        CLUSTER_START_FAILED,
        CLUSTER_STOP,
        CLUSTER_STOP_FAILED,
        CLUSTER_UPSCALE,
        CLUSTER_DOWNSCALE,
        TERMINATION,
        TERMINATION_FAILED,
        UPDATE_ALLOWED_SUBNETS,
        SUCCESS,
        NONE
    }

    @Autowired
    private Reactor reactor;

    @Resource
    private Map<Class, FlowHandler> flowHandlersMap;

    @Autowired
    private TransitionKeyService transitionKeyService;

    @Override
    public void afterPropertiesSet() throws Exception {

        transitionKeyService.registerTransition(ProvisioningSetupHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.PROVISIONING_SETUP.name(), Phases.PROVISIONING.name(), Phases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(ProvisioningHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.PROVISIONING.name(), Phases.METADATA_SETUP.name(), Phases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(MetadataSetupHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.METADATA_SETUP.name(), Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(AmbariRoleAllocationHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.AMBARI_ROLE_ALLOCATION.name(), Phases.AMBARI_START.name(), Phases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(AmbariStartHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.AMBARI_START.name(), Phases.CLUSTER_CREATION.name(), Phases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(StackTerminationHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.TERMINATION.name(), Phases.SUCCESS.name(), Phases.NONE.name()));

        transitionKeyService.registerTransition(StackStartHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.STACK_START.name(), Phases.CLUSTER_START.name(), Phases.STACK_START_FAILED.name()));

        transitionKeyService.registerTransition(ClusterStopHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.CLUSTER_STOP.name(), Phases.STACK_STOP.name(), Phases.CLUSTER_STOP_FAILED.name()));

        transitionKeyService.registerTransition(ClusterStartHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.CLUSTER_START.name(), Phases.SUCCESS.name(), Phases.CLUSTER_START_FAILED.name()));

        transitionKeyService.registerTransition(StackStopHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.STACK_STOP.name(), Phases.SUCCESS.name(), Phases.STACK_STOP_FAILED.name()));

        transitionKeyService.registerTransition(StackUpscaleHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.STACK_UPSCALE.name(), Phases.SUCCESS.name(), Phases.NONE.name()));

        transitionKeyService.registerTransition(StackDownscaleHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.STACK_DOWNSCALE.name(), Phases.SUCCESS.name(), Phases.NONE.name()));

        transitionKeyService.registerTransition(ClusterUpscaleHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.CLUSTER_UPSCALE.name(), Phases.SUCCESS.name(), Phases.NONE.name()));

        transitionKeyService.registerTransition(ClusterDownscaleHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.CLUSTER_DOWNSCALE.name(), Phases.SUCCESS.name(), Phases.NONE.name()));

        transitionKeyService.registerTransition(UpdateAllowedSubnetsHandler.class, SimpleTransitionKeyService.TransitionFactory
                .createTransition(Phases.UPDATE_ALLOWED_SUBNETS.name(), Phases.SUCCESS.name(), null));

        reactor.on($(Phases.PROVISIONING_SETUP.name()), getHandlerForClass(ProvisioningSetupHandler.class));
        reactor.on($(Phases.PROVISIONING.name()), getHandlerForClass(ProvisioningHandler.class));
        reactor.on($(Phases.METADATA_SETUP.name()), getHandlerForClass(MetadataSetupHandler.class));
        reactor.on($(Phases.AMBARI_ROLE_ALLOCATION.name()), getHandlerForClass(AmbariRoleAllocationHandler.class));
        reactor.on($(Phases.AMBARI_START.name()), getHandlerForClass(AmbariStartHandler.class));
        reactor.on($(Phases.CLUSTER_CREATION.name()), getHandlerForClass(ClusterCreationHandler.class));
        reactor.on($(Phases.TERMINATION.name()), getHandlerForClass(StackTerminationHandler.class));
        reactor.on($(Phases.STACK_START.name()), getHandlerForClass(StackStartHandler.class));
        reactor.on($(Phases.STACK_STOP.name()), getHandlerForClass(StackStopHandler.class));
        reactor.on($(Phases.STACK_UPSCALE.name()), getHandlerForClass(StackUpscaleHandler.class));
        reactor.on($(Phases.STACK_DOWNSCALE.name()), getHandlerForClass(StackDownscaleHandler.class));
        reactor.on($(Phases.CLUSTER_START.name()), getHandlerForClass(ClusterStartHandler.class));
        reactor.on($(Phases.CLUSTER_STOP.name()), getHandlerForClass(ClusterStopHandler.class));
        reactor.on($(Phases.CLUSTER_UPSCALE.name()), getHandlerForClass(ClusterUpscaleHandler.class));
        reactor.on($(Phases.CLUSTER_DOWNSCALE.name()), getHandlerForClass(ClusterDownscaleHandler.class));
        reactor.on($(Phases.UPDATE_ALLOWED_SUBNETS.name()), getHandlerForClass(UpdateAllowedSubnetsHandler.class));

        reactor.on($(Phases.STACK_CREATION_FAILED.name()), getHandlerForClass(StackCreationFailureHandler.class));
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
