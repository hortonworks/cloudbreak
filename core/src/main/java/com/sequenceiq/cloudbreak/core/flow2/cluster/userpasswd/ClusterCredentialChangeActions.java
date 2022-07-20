package com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CHANGE_CREDENTIAL_FAILED;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCredentialChangeTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest.Type;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult;

@Configuration
public class ClusterCredentialChangeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCredentialChangeActions.class);

    @Inject
    private ClusterCredentialChangeService clusterCredentialChangeService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_STATE")
    public Action<?, ?> changingClusterCredential() {
        return new AbstractClusterAction<>(ClusterCredentialChangeTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, ClusterCredentialChangeTriggerEvent payload, Map<Object, Object> variables) {
                clusterCredentialChangeService.credentialChange(ctx.getStackId());
                ClusterCredentialChangeRequest request;
                switch (payload.getType()) {
                    case REPLACE:
                        request = new ClusterCredentialChangeRequest(ctx.getStackId(), payload.getUser(), payload.getPassword(), Type.REPLACE);
                        break;
                    case UPDATE:
                        request = new ClusterCredentialChangeRequest(ctx.getStackId(), null, payload.getPassword(), Type.UPDATE);
                        break;
                    default:
                        throw new UnsupportedOperationException("Ambari credential update request not supported: " + payload.getType());
                }
                sendEvent(ctx, request);
            }
        };
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FINISHED_STATE")
    public Action<?, ?> clusterCredentialChangeFinished() {
        return new AbstractClusterAction<>(ClusterCredentialChangeResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterCredentialChangeResult payload, Map<Object, Object> variables) {
                switch (payload.getRequest().getType()) {
                    case REPLACE:
                        clusterCredentialChangeService.finishCredentialReplace(context.getStackId(), context.getCluster().getId(),
                                payload.getRequest().getUser(), payload.getRequest().getPassword());
                        break;
                    case UPDATE:
                        clusterCredentialChangeService.finishCredentialUpdate(context.getStackId(), context.getCluster().getId(),
                                payload.getRequest().getPassword());
                        break;
                    default:
                        LOGGER.error("Ambari credential update request not supported: " + payload.getRequest().getType());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterCredentialChangeEvent.FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CLUSTER_CREDENTIALCHANGE_FAILED_STATE")
    public Action<?, ?> clusterCredentialChangeFailedAction() {
        return new AbstractStackFailureAction<ClusterCredentialChangeState, ClusterCredentialChangeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Exception during cluster authentication change!: {}", payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(), UPDATE_FAILED.name(), CLUSTER_CHANGE_CREDENTIAL_FAILED);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ClusterCredentialChangeEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
