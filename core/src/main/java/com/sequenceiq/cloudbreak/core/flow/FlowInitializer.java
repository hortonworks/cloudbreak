package com.sequenceiq.cloudbreak.core.flow;

import static reactor.bus.selector.Selectors.$;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterCredentialChangeHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterResetHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStartRequestedHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.MetadataCollectHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.UpdateAllowedSubnetsHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.UpscaleMetadataCollectHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.UpscaleStackSyncHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

@Component
public class FlowInitializer implements InitializingBean {

    @Inject
    private EventBus reactor;

    @Resource
    private Map<Class, FlowHandler> flowHandlersMap;

    @Inject
    private TransitionKeyService transitionKeyService;

    @Override
    public void afterPropertiesSet() throws Exception {

        registerStartFlows();
        registerUpscaleFlows();
        registerResetFlows();
        registerUpdateAllowedSubnetFlow();
        registerStartRequestedFlows();
        registerAuthenticationClusterChangeFlows();

        reactor.on($(FlowPhases.METADATA_COLLECT.name()), getHandlerForClass(MetadataCollectHandler.class));
        reactor.on($(FlowPhases.UPSCALE_METADATA_COLLECT.name()), getHandlerForClass(UpscaleMetadataCollectHandler.class));
        reactor.on($(FlowPhases.CLUSTER_RESET.name()), getHandlerForClass(ClusterResetHandler.class));
        reactor.on($(FlowPhases.UPSCALE_STACK_SYNC.name()), getHandlerForClass(UpscaleStackSyncHandler.class));
        reactor.on($(FlowPhases.UPDATE_ALLOWED_SUBNETS.name()), getHandlerForClass(UpdateAllowedSubnetsHandler.class));
        reactor.on($(FlowPhases.CLUSTER_START_REQUESTED.name()), getHandlerForClass(ClusterStartRequestedHandler.class));
        reactor.on($(FlowPhases.STACK_STATUS_UPDATE_FAILED.name()), getHandlerForClass(StackStatusUpdateFailureHandler.class));
        reactor.on($(FlowPhases.CLUSTER_USERNAME_PASSWORD_UPDATE.name()), getHandlerForClass(ClusterCredentialChangeHandler.class));
    }

    private void registerAuthenticationClusterChangeFlows() {
        transitionKeyService.registerTransition(ClusterCredentialChangeHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_USERNAME_PASSWORD_UPDATE.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerStartFlows() {
        transitionKeyService.registerTransition(MetadataCollectHandler.class, TransitionFactory
                .createTransition(FlowPhases.METADATA_COLLECT.name(), FlowPhases.CLUSTER_START.name(), FlowPhases.STACK_STATUS_UPDATE_FAILED.name()));

        transitionKeyService.registerTransition(StackStatusUpdateFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_STATUS_UPDATE_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));

    }

    private void registerStartRequestedFlows() {
        transitionKeyService.registerTransition(ClusterStartRequestedHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_START_REQUESTED.name(), FlowPhases.NONE.name(), FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name()));
    }

    private void registerUpscaleFlows() {
        transitionKeyService.registerTransition(UpscaleStackSyncHandler.class, TransitionFactory
                .createTransition(FlowPhases.UPSCALE_STACK_SYNC.name(), FlowPhases.UPSCALE_METADATA_COLLECT.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(UpscaleMetadataCollectHandler.class, TransitionFactory
                .createTransition(FlowPhases.UPSCALE_METADATA_COLLECT.name(), FlowPhases.ADD_INSTANCES.name(), FlowPhases.NONE.name()));
    }

    private void registerResetFlows() {
        transitionKeyService.registerTransition(ClusterResetHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_RESET.name(), FlowPhases.AMBARI_START.name(), FlowPhases.NONE.name()));
    }

    private void registerUpdateAllowedSubnetFlow() {
        transitionKeyService.registerTransition(UpdateAllowedSubnetsHandler.class, TransitionFactory
                .createTransition(FlowPhases.UPDATE_ALLOWED_SUBNETS.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }


    private Consumer<Event<?>> getHandlerForClass(Class handlerClass) {
        FlowHandler handler;
        if (flowHandlersMap.containsKey(handlerClass)) {
            handler = flowHandlersMap.get(handlerClass);
        } else {
            throw new IllegalStateException("No registered handler found for " + handlerClass + " Check your configuration");
        }
        return (Consumer<Event<?>>) handler;
    }
}
