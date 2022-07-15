package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateResult;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class UpgradeCcmActions {

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Bean(name = "UPGRADE_CCM_TUNNEL_UPDATE_STATE")
    public Action<?, ?> tunnelUpdate() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmTriggerRequest.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmTriggerRequest payload, Map<Object, Object> variables) {
                upgradeCcmService.tunnelUpdateState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmTunnelUpdateRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_PUSH_SALT_STATES_STATE")
    public Action<?, ?> pushSaltStates() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmTunnelUpdateResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmTunnelUpdateResult payload, Map<Object, Object> variables) {
                upgradeCcmService.pushSaltStatesState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmPushSaltStatesRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_RECONFIGURE_NGINX_STATE")
    public Action<?, ?> reconfigureNginx() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmPushSaltStatesResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmPushSaltStatesResult payload, Map<Object, Object> variables) {
                upgradeCcmService.reconfigureNginxState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmReconfigureNginxRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE")
    public Action<?, ?> registerClusterToClusterProxy() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmReconfigureNginxResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmReconfigureNginxResult payload, Map<Object, Object> variables) {
                upgradeCcmService.registerClusterProxyState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmRegisterClusterProxyRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_HEALTH_CHECK_STATE")
    public Action<?, ?> healthCheck() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmRegisterClusterProxyResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmRegisterClusterProxyResult payload, Map<Object, Object> variables) {
                upgradeCcmService.healthCheckState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmHealthCheckRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_REMOVE_AGENT_STATE")
    public Action<?, ?> removeAgent() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmHealthCheckResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmHealthCheckResult payload, Map<Object, Object> variables) {
                upgradeCcmService.removeAgentState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmRemoveAgentRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_DEREGISTER_AGENT_STATE")
    public Action<?, ?> deregisterAgent() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmRemoveAgentResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmRemoveAgentResult payload, Map<Object, Object> variables) {
                upgradeCcmService.deregisterAgentState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new UpgradeCcmDeregisterAgentRequest(context.getStackId(), context.getClusterId(), context.getOldTunnel());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FINISHED_STATE")
    public Action<?, ?> upgradeCcmFinished() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmDeregisterAgentResult.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmDeregisterAgentResult payload, Map<Object, Object> variables) {
                upgradeCcmService.ccmUpgradeFinished(payload.getResourceId(), context.getClusterId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeCcmContext context) {
                return new StackEvent(UpgradeCcmEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FAILED_STATE")
    public Action<?, ?> upgradeCcmFailed() {
        return new AbstractStackFailureAction<UpgradeCcmState, UpgradeCcmEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                UpgradeCcmFailedEvent concretePayload = (UpgradeCcmFailedEvent) payload;
                upgradeCcmService.ccmUpgradeFailed(concretePayload.getResourceId(),
                        Optional.ofNullable(context.getStack()).map(StackView::getClusterId).orElse(null),
                        concretePayload.getOldTunnel(), concretePayload.getFailureOrigin());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeCcmEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
