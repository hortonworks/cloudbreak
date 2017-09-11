package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.AbstractStackCreationAction;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.HostMetadataSetupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxySuccess;

@Configuration
public class ClusterCreationActions {
    @Inject
    private ClusterCreationService clusterCreationService;

    @Bean(name = "BOOTSTRAPPING_MACHINES_STATE")
    public Action bootstrappingMachinesAction() {
        return new AbstractStackCreationAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.bootstrappingMachines(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new BootstrapMachinesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "COLLECTING_HOST_METADATA_STATE")
    public Action collectingHostMetadataAction() {
        return new AbstractStackCreationAction<BootstrapMachinesSuccess>(BootstrapMachinesSuccess.class) {
            @Override
            protected void doExecute(StackContext context, BootstrapMachinesSuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.collectingHostMetadata(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new HostMetadataSetupRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "STARTING_AMBARI_SERVICES_STATE")
    public Action startingAmbariServicesAction() {
        return new AbstractClusterAction<HostMetadataSetupSuccess>(HostMetadataSetupSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, HostMetadataSetupSuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.startingAmbariServices(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StartAmbariServicesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "REGISTER_PROXY_STATE")
    public Action registerProxyAction() {
        return new AbstractClusterAction<StartAmbariServicesSuccess>(StartAmbariServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, StartAmbariServicesSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new RegisterProxyRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "STARTING_AMBARI_STATE")
    public Action startingAmbariAction() {
        return new AbstractClusterAction<RegisterProxySuccess>(RegisterProxySuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, RegisterProxySuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.startingAmbari(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StartAmbariRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "INSTALLING_CLUSTER_STATE")
    public Action installingClusterAction() {
        return new AbstractClusterAction<StartAmbariSuccess>(StartAmbariSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, StartAmbariSuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.installingCluster(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new InstallClusterRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FINISHED_STATE")
    public Action clusterCreationFinishedAction() {
        return new AbstractClusterAction<InstallClusterSuccess>(InstallClusterSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, InstallClusterSuccess payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.clusterInstallationFinished(context.getStack(), context.getCluster());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREATION_FAILED_STATE")
    public Action clusterCreationFailedAction() {
        return new AbstractStackFailureAction<ClusterCreationState, ClusterCreationEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.handleClusterCreationFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
