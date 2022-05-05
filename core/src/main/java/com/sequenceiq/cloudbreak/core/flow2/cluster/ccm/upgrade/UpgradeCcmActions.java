package com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade;

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
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmHealthCheckResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmPushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmReconfigureNginxResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRemoveAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmRegisterClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmDeregisterAgentResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTunnelUpdateResult;

@Configuration
public class UpgradeCcmActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmActions.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Bean(name = "UPGRADE_CCM_TUNNEL_UPDATE_STATE")
    public Action<?, ?> tunnelUpdate() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                upgradeCcmService.tunnelUpdateState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmTunnelUpdateRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_PUSH_SALT_STATES_STATE")
    public Action<?, ?> pushSaltStates() {
        return new AbstractClusterAction<>(UpgradeCcmTunnelUpdateResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmTunnelUpdateResult payload, Map<Object, Object> variables) {
                upgradeCcmService.pushSaltStatesState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmPushSaltStatesRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_RECONFIGURE_NGINX_STATE")
    public Action<?, ?> reconfigureNginx() {
        return new AbstractClusterAction<>(UpgradeCcmPushSaltStatesResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmPushSaltStatesResult payload, Map<Object, Object> variables) {
                upgradeCcmService.reconfigureNginxState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmReconfigureNginxRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE")
    public Action<?, ?> registerClusterToClusterProxy() {
        return new AbstractClusterAction<>(UpgradeCcmReconfigureNginxResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmReconfigureNginxResult payload, Map<Object, Object> variables) {
                upgradeCcmService.registerClusterProxyState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmRegisterClusterProxyRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_HEALTH_CHECK_STATE")
    public Action<?, ?> healthCheck() {
        return new AbstractClusterAction<>(UpgradeCcmRegisterClusterProxyResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmRegisterClusterProxyResult payload, Map<Object, Object> variables) {
                upgradeCcmService.healthCheckState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmHealthCheckRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_REMOVE_AGENT_STATE")
    public Action<?, ?> removeAgent() {
        return new AbstractClusterAction<>(UpgradeCcmHealthCheckResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmHealthCheckResult payload, Map<Object, Object> variables) {
                upgradeCcmService.removeAgentState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmRemoveAgentRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_DEREGISTER_AGENT_STATE")
    public Action<?, ?> deregisterAgent() {
        return new AbstractClusterAction<>(UpgradeCcmRemoveAgentResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmRemoveAgentResult payload, Map<Object, Object> variables) {
                upgradeCcmService.deregisterAgentState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new UpgradeCcmDeregisterAgentRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FINISHED_STATE")
    public Action<?, ?> upgradeCcmFinished() {
        return new AbstractClusterAction<>(UpgradeCcmDeregisterAgentResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, UpgradeCcmDeregisterAgentResult payload, Map<Object, Object> variables) {
                upgradeCcmService.ccmUpgradeFinished(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(UpgradeCcmEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_CCM_FAILED_STATE")
    public Action<?, ?> upgradeCcmFailed() {
        return new AbstractStackFailureAction<UpgradeCcmState, UpgradeCcmEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                upgradeCcmService.ccmUpgradeFailed(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeCcmEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
