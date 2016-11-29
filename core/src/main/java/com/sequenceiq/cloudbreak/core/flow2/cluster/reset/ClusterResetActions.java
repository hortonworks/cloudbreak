package com.sequenceiq.cloudbreak.core.flow2.cluster.reset;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StartAmbariSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;

@Configuration
public class ClusterResetActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResetActions.class);

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterResetService clusterResetService;

    @Bean(name = "CLUSTER_RESET_STATE")
    public Action syncCluster() {
        return new AbstractClusterResetAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                clusterResetService.resetCluster(context.getStack(), context.getCluster());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new ClusterResetRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_FINISHED_STATE")
    public Action finishResetCluster() {
        return new AbstractClusterResetAction<ClusterResetResult>(ClusterResetResult.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterResetResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StartAmbariRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_START_AMBARI_FINISHED_STATE")
    public Action finishStartAmbari() {
        return new AbstractClusterResetAction<StartAmbariSuccess>(StartAmbariSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, StartAmbariSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterResetEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_RESET_FAILED_STATE")
    public Action clusterResetFailedAction() {
        return new AbstractStackFailureAction<ClusterResetState, ClusterResetEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                clusterResetService.handleResetClusterFailure(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterResetEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
