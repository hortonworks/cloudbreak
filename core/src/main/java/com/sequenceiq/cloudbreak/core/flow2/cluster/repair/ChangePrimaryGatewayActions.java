package com.sequenceiq.cloudbreak.core.flow2.cluster.repair;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
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
            protected void doExecute(ClusterViewContext context, ChangePrimaryGatewayTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.changePrimaryGatewayStarted(context.getStackId());
                ChangePrimaryGatewayRequest request = new ChangePrimaryGatewayRequest(context.getStackId());
                sendEvent(context.getFlowId(), request.selector(), request);
            }
        };
    }

    @Bean(name = "WAITING_FOR_AMBARI_SERVER_STATE")
    public Action waitingForAmbariServer() {
        return new AbstractClusterAction<ChangePrimaryGatewaySuccess>(ChangePrimaryGatewaySuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ChangePrimaryGatewaySuccess payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.primaryGatewayChanged(context.getStackId(), payload.getNewPrimaryGatewayFQDN());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new WaitForAmbariServerRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPDATE_PROXY_STATE")
    public Action registerProxyAction() {
        return new AbstractClusterAction<WaitForAmbariServerSuccess>(WaitForAmbariServerSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, WaitForAmbariServerSuccess payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new RegisterProxyRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FINISHED_STATE")
    public Action changeGatewayFinishedAction() {
        return new AbstractClusterAction<RegisterProxySuccess>(RegisterProxySuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, RegisterProxySuccess payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.ambariServerStarted(context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FLOW_FINISHED.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CHANGE_PRIMARY_GATEWAY_FAILED_STATE")
    public Action repairGatewayFailedAction() {
        return new AbstractStackFailureAction<ChangePrimaryGatewayState, ChangePrimaryGatewayEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                changePrimaryGatewayService.changePrimaryGatewayFailed(context.getStackView().getId(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_FAILURE_HANDLED.event(), context.getStackView().getId());
            }
        };
    }
}
