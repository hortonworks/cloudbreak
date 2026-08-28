package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.CLUSTER_OPERATION;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.UPDATE_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerDeletionEvent.LOAD_BALANCER_DELETION_FINISHED_EVENT;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.LoadBalancerDeletionTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionRequest;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerCloudDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDeregistrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event.deletion.LoadBalancerDomainDeregistrationRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

@Configuration
public class FreeIpaLoadBalancerDeletionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerDeletionActions.class);

    @Bean(name = "LOAD_BALANCER_DNS_DEREGISTRATION_STATE")
    public Action<?, ?> loadBalancerDnsDeregistration() {
        return new AbstractLoadBalancerDeletionAction<>(LoadBalancerDeletionTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext context, LoadBalancerDeletionTriggerEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                getStackUpdater().updateStackStatus(stack, CLUSTER_OPERATION, "Deleting FreeIPA load balancer: removing DNS entry");
                getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_STARTED);
                sendEvent(context, new LoadBalancerDomainDeregistrationRequest(stack.getId()));
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_CLOUD_DELETION_STATE")
    public Action<?, ?> loadBalancerCloudDeletion() {
        return new AbstractLoadBalancerDeletionAction<>(LoadBalancerDeregistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, LoadBalancerDeregistrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                getStackUpdater().updateStackStatus(stack, CLUSTER_OPERATION, "Deleting FreeIPA load balancer: removing cloud resources");
                sendEvent(context, new LoadBalancerCloudDeletionRequest(stack.getId(), context.getCloudContext(), context.getCloudCredential(),
                        context.getCloudStack()));
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_DELETION_FINISHED_STATE")
    public Action<?, ?> loadBalancerDeletionFinished() {
        return new AbstractLoadBalancerDeletionAction<>(LoadBalancerCloudDeletionSuccess.class) {
            @Override
            protected void doExecute(StackContext context, LoadBalancerCloudDeletionSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.debug("FreeIPA load balancer deletion completed for stack {}", stack.getId());
                getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_FINISHED);
                getStackUpdater().updateStackStatus(stack, CLUSTER_OPERATION, "Finished deleting FreeIPA load balancer.");
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(LOAD_BALANCER_DELETION_FINISHED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "LOAD_BALANCER_DELETION_FAILED_STATE")
    public Action<?, ?> loadBalancerDeletionFailed() {
        return new AbstractLoadBalancerDeletionAction<>(LoadBalancerDeletionFailureEvent.class) {
            @Override
            protected void doExecute(StackContext context, LoadBalancerDeletionFailureEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String errorReason = payload.getException() == null ? "Unknown error" : payload.getException().getMessage();
                LOGGER.error("FreeIPA load balancer deletion failed for stack {}: {}", stack.getId(), errorReason);
                getStackUpdater().updateStackStatus(stack, UPDATE_FAILED, "FreeIPA load balancer deletion failed: " + errorReason);
                getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), ResourceEvent.FREEIPA_LOAD_BALANCER_DELETION_FAILED,
                        List.of(errorReason));
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackContext context) {
                return new StackEvent(LOAD_BALANCER_DELETION_FAILURE_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}
