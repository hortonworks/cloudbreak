package com.sequenceiq.cloudbreak.core.flow2.cluster.repair;

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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewaySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxySuccess;

@Configuration
public class ChangePrimaryGatewayActions {
    @Inject
    private ChangePrimaryGatewayService changePrimaryGatewayService;

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_STATE")
    public Action repairGatewayAction() {
        return new AbstractClusterAction<ChangePrimaryGatewayTriggerEvent>(ChangePrimaryGatewayTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterContext context, ChangePrimaryGatewayTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.changePrimaryGatewayStarted(context.getStack());
                ChangePrimaryGatewayRequest request = new ChangePrimaryGatewayRequest(context.getStack().getId());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "WAITING_FOR_AMBARI_SERVER_STATE")
    public Action waitingForAmbariServer() {
        return new AbstractClusterAction<ChangePrimaryGatewaySuccess>(ChangePrimaryGatewaySuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, ChangePrimaryGatewaySuccess payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.primaryGatewayChanged(context.getStack(), payload.getNewPrimaryGatewayFQDN());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new WaitForAmbariServerRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "UPDATE_PROXY_STATE")
    public Action registerProxyAction() {
        return new AbstractClusterAction<WaitForAmbariServerSuccess>(WaitForAmbariServerSuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, WaitForAmbariServerSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new RegisterProxyRequest(context.getStack().getId());
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FINISHED_STATE")
    public Action changeGatewayFinishedAction() {
        return new AbstractClusterAction<RegisterProxySuccess>(RegisterProxySuccess.class) {
            @Override
            protected void doExecute(ClusterContext context, RegisterProxySuccess payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.ambariServerStarted(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterContext context) {
                return new StackEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FAILED_STATE")
    public Action repairGatewayFailedAction() {
        return new AbstractStackFailureAction<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.changePrimaryGatewayFailed(context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED.event(), context.getStack().getId());
            }
        };
    }
}
