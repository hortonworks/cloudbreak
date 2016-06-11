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
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.InstallClusterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;

@Configuration
public class ClusterCreationActions {
    @Inject
    private ClusterCreationService clusterCreationService;

    @Bean(name = "STARTING_AMBARI_SERVICES_STATE")
    public Action startingAmbariServicesAction() {
        return new AbstractClusterAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                clusterCreationService.startingAmbariServices(context.getStack(), context.getCluster());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StartAmbariServicesRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "STARTING_AMBARI_STATE")
    public Action startingAmbariAction() {
        return new AbstractClusterAction<StartAmbariServicesSuccess>(StartAmbariServicesSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, StartAmbariServicesSuccess payload, Map<Object, Object> variables) throws Exception {
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
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FINISHED_EVENT.stringRepresentation(), context.getStack().getId());
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
                return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
