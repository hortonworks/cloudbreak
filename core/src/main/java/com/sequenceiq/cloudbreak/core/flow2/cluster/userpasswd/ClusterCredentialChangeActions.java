package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterContext;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;

@Configuration
public class ClusterCredentialChangeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCredentialChangeActions.class);
    @Inject
    private ClusterCredentialChangeService clusterCredentialChangeService;

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_STATE")
    public Action changingClusterCredential() {
        return new AbstractClusterAction<ClusterCredentialChangeTriggerEvent>(ClusterCredentialChangeTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterCredentialChangeTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                clusterCredentialChangeService.credentialChange(context.getStack().getId());
                sendEvent(context.getFlowId(), new ClusterCredentialChangeRequest(context.getStack().getId(), payload.getUser(), payload.getPassword()));
            }
        };
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FINISHED_STATE")
    public Action clusterCredentialChangeFinished() {
        return new AbstractClusterAction<ClusterCredentialChangeResult>(ClusterCredentialChangeResult.class) {
            @Override
            protected void doExecute(ClusterContext context, ClusterCredentialChangeResult payload, Map<Object, Object> variables) throws Exception {
                clusterCredentialChangeService.finishCredentialChange(context.getStack().getId(), context.getCluster(),
                        payload.getRequest().getUser(), payload.getRequest().getPassword());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ClusterCredentialChangeEvent.FINALIZED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FAILED_STATE")
    public Action clusterCredentialChangeFailedAction() {
        return new AbstractStackFailureAction<ClusterCredentialChangeState, ClusterCredentialChangeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Exception during cluster authentication change!: {}", payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCredentialChangeEvent.FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
            }
        };
    }
}
