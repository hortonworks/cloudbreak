package com.sequenceiq.cloudbreak.core.flow;

import static reactor.event.selector.Selectors.$;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.handlers.AddClusterContainersHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.AddInstancesHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.BootstrapClusterHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.BootstrapNewNodesHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterContainersHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterDownscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterInstallHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterResetHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterSecurityHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStartRequestedHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterStopHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterSyncHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ClusterUpscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ConsulMetadataSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ExtendConsulMetadataHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ExtendMetadataHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.MetadataSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackCreationFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackDownscaleHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStartHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStatusUpdateFailureHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStopHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackStopRequestedHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackSyncHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.StackTerminationHandler;
import com.sequenceiq.cloudbreak.core.flow.handlers.UpdateAllowedSubnetsHandler;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class FlowInitializer implements InitializingBean {

    @Autowired
    private Reactor reactor;

    @Resource
    private Map<Class, FlowHandler> flowHandlersMap;

    @Autowired
    private TransitionKeyService transitionKeyService;

    @Override
    public void afterPropertiesSet() throws Exception {

        registerProvisioningFlows();
        registerTerminationFlow();
        registerStartFlows();
        registerStopFlows();
        registerUpscaleFlows();
        registerDownscaleFlows();
        registerResetFlows();
        registerUpdateAllowedSubnetFlow();
        registerStopRequestedFlows();
        registerStartRequestedFlows();
        registerSyncCluster();
        registerSyncStack();

        reactor.on($(FlowPhases.PROVISIONING_SETUP.name()), getHandlerForClass(ProvisioningSetupHandler.class));
        reactor.on($(FlowPhases.PROVISIONING.name()), getHandlerForClass(ProvisioningHandler.class));
        reactor.on($(FlowPhases.METADATA_SETUP.name()), getHandlerForClass(MetadataSetupHandler.class));
        reactor.on($(FlowPhases.BOOTSTRAP_CLUSTER.name()), getHandlerForClass(BootstrapClusterHandler.class));
        reactor.on($(FlowPhases.CONSUL_METADATA_SETUP.name()), getHandlerForClass(ConsulMetadataSetupHandler.class));
        reactor.on($(FlowPhases.RUN_CLUSTER_CONTAINERS.name()), getHandlerForClass(ClusterContainersHandler.class));
        reactor.on($(FlowPhases.AMBARI_START.name()), getHandlerForClass(AmbariStartHandler.class));
        reactor.on($(FlowPhases.CLUSTER_INSTALL.name()), getHandlerForClass(ClusterInstallHandler.class));
        reactor.on($(FlowPhases.CLUSTER_RESET.name()), getHandlerForClass(ClusterResetHandler.class));
        reactor.on($(FlowPhases.ENABLE_KERBEROS.name()), getHandlerForClass(ClusterSecurityHandler.class));
        reactor.on($(FlowPhases.TERMINATION.name()), getHandlerForClass(StackTerminationHandler.class));
        reactor.on($(FlowPhases.STACK_START.name()), getHandlerForClass(StackStartHandler.class));
        reactor.on($(FlowPhases.STACK_STOP.name()), getHandlerForClass(StackStopHandler.class));
        reactor.on($(FlowPhases.ADD_INSTANCES.name()), getHandlerForClass(AddInstancesHandler.class));
        reactor.on($(FlowPhases.EXTEND_METADATA.name()), getHandlerForClass(ExtendMetadataHandler.class));
        reactor.on($(FlowPhases.BOOTSTRAP_NEW_NODES.name()), getHandlerForClass(BootstrapNewNodesHandler.class));
        reactor.on($(FlowPhases.EXTEND_CONSUL_METADATA.name()), getHandlerForClass(ExtendConsulMetadataHandler.class));
        reactor.on($(FlowPhases.ADD_CLUSTER_CONTAINERS.name()), getHandlerForClass(AddClusterContainersHandler.class));
        reactor.on($(FlowPhases.STACK_DOWNSCALE.name()), getHandlerForClass(StackDownscaleHandler.class));
        reactor.on($(FlowPhases.CLUSTER_START.name()), getHandlerForClass(ClusterStartHandler.class));
        reactor.on($(FlowPhases.CLUSTER_STOP.name()), getHandlerForClass(ClusterStopHandler.class));
        reactor.on($(FlowPhases.CLUSTER_UPSCALE.name()), getHandlerForClass(ClusterUpscaleHandler.class));
        reactor.on($(FlowPhases.CLUSTER_DOWNSCALE.name()), getHandlerForClass(ClusterDownscaleHandler.class));
        reactor.on($(FlowPhases.UPDATE_ALLOWED_SUBNETS.name()), getHandlerForClass(UpdateAllowedSubnetsHandler.class));
        reactor.on($(FlowPhases.STACK_CREATION_FAILED.name()), getHandlerForClass(StackCreationFailureHandler.class));
        reactor.on($(FlowPhases.STACK_STOP_REQUESTED.name()), getHandlerForClass(StackStopRequestedHandler.class));
        reactor.on($(FlowPhases.CLUSTER_START_REQUESTED.name()), getHandlerForClass(ClusterStartRequestedHandler.class));
        reactor.on($(FlowPhases.STACK_STATUS_UPDATE_FAILED.name()), getHandlerForClass(StackStatusUpdateFailureHandler.class));
        reactor.on($(FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name()), getHandlerForClass(ClusterStatusUpdateFailureHandler.class));
        reactor.on($(FlowPhases.STACK_SYNC.name()), getHandlerForClass(StackSyncHandler.class));
        reactor.on($(FlowPhases.CLUSTER_SYNC.name()), getHandlerForClass(ClusterSyncHandler.class));

    }

    private void registerSyncStack() {
        transitionKeyService.registerTransition(StackSyncHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_SYNC.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerSyncCluster() {
        transitionKeyService.registerTransition(ClusterSyncHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_SYNC.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerProvisioningFlows() {
        transitionKeyService.registerTransition(ProvisioningSetupHandler.class, TransitionFactory
                .createTransition(FlowPhases.PROVISIONING_SETUP.name(), FlowPhases.PROVISIONING.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(ProvisioningHandler.class, TransitionFactory
                .createTransition(FlowPhases.PROVISIONING.name(), FlowPhases.METADATA_SETUP.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(MetadataSetupHandler.class, TransitionFactory
                .createTransition(FlowPhases.METADATA_SETUP.name(), FlowPhases.BOOTSTRAP_CLUSTER.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(BootstrapClusterHandler.class, TransitionFactory
                .createTransition(FlowPhases.BOOTSTRAP_CLUSTER.name(), FlowPhases.CONSUL_METADATA_SETUP.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(ConsulMetadataSetupHandler.class, TransitionFactory
                .createTransition(FlowPhases.CONSUL_METADATA_SETUP.name(), FlowPhases.RUN_CLUSTER_CONTAINERS.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(ClusterContainersHandler.class, TransitionFactory
                .createTransition(FlowPhases.RUN_CLUSTER_CONTAINERS.name(), FlowPhases.AMBARI_START.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(AmbariStartHandler.class, TransitionFactory
                .createTransition(FlowPhases.AMBARI_START.name(), FlowPhases.CLUSTER_INSTALL.name(), FlowPhases.STACK_CREATION_FAILED.name()));

        transitionKeyService.registerTransition(StackCreationFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_CREATION_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterInstallHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_INSTALL.name(), FlowPhases.ENABLE_KERBEROS.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterSecurityHandler.class, TransitionFactory
                .createTransition(FlowPhases.ENABLE_KERBEROS.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerTerminationFlow() {
        transitionKeyService.registerTransition(StackTerminationHandler.class, TransitionFactory
                .createTransition(FlowPhases.TERMINATION.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerStartFlows() {
        transitionKeyService.registerTransition(StackStartHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_START.name(), FlowPhases.CLUSTER_START.name(), FlowPhases.STACK_STATUS_UPDATE_FAILED.name()));

        transitionKeyService.registerTransition(ClusterStartHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_START.name(), FlowPhases.NONE.name(), FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name()));

        transitionKeyService.registerTransition(StackStatusUpdateFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_STATUS_UPDATE_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterStatusUpdateFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerStopFlows() {
        transitionKeyService.registerTransition(ClusterStopHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_STOP.name(), FlowPhases.STACK_STOP.name(), FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name()));

        transitionKeyService.registerTransition(StackStopHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_STOP.name(), FlowPhases.NONE.name(), FlowPhases.STACK_STATUS_UPDATE_FAILED.name()));

        transitionKeyService.registerTransition(StackStatusUpdateFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_STATUS_UPDATE_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterStatusUpdateFailureHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerStopRequestedFlows() {
        transitionKeyService.registerTransition(StackStopRequestedHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_STOP_REQUESTED.name(), FlowPhases.NONE.name(), FlowPhases.STACK_STATUS_UPDATE_FAILED.name()));
    }

    private void registerStartRequestedFlows() {
        transitionKeyService.registerTransition(ClusterStartRequestedHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_START_REQUESTED.name(), FlowPhases.NONE.name(), FlowPhases.CLUSTER_STATUS_UPDATE_FAILED.name()));
    }

    private void registerUpscaleFlows() {
        transitionKeyService.registerTransition(AddInstancesHandler.class, TransitionFactory
                .createTransition(FlowPhases.ADD_INSTANCES.name(), FlowPhases.EXTEND_METADATA.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ExtendMetadataHandler.class, TransitionFactory
                .createTransition(FlowPhases.EXTEND_METADATA.name(), FlowPhases.BOOTSTRAP_NEW_NODES.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(BootstrapNewNodesHandler.class, TransitionFactory
                .createTransition(FlowPhases.BOOTSTRAP_NEW_NODES.name(), FlowPhases.EXTEND_CONSUL_METADATA.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ExtendConsulMetadataHandler.class, TransitionFactory
                .createTransition(FlowPhases.EXTEND_CONSUL_METADATA.name(), FlowPhases.ADD_CLUSTER_CONTAINERS.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(AddClusterContainersHandler.class, TransitionFactory
                .createTransition(FlowPhases.ADD_CLUSTER_CONTAINERS.name(), FlowPhases.CLUSTER_UPSCALE.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterUpscaleHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_UPSCALE.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));
    }

    private void registerDownscaleFlows() {
        transitionKeyService.registerTransition(StackDownscaleHandler.class, TransitionFactory
                .createTransition(FlowPhases.STACK_DOWNSCALE.name(), FlowPhases.NONE.name(), FlowPhases.NONE.name()));

        transitionKeyService.registerTransition(ClusterDownscaleHandler.class, TransitionFactory
                .createTransition(FlowPhases.CLUSTER_DOWNSCALE.name(), FlowPhases.STACK_DOWNSCALE.name(), FlowPhases.NONE.name()));
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
