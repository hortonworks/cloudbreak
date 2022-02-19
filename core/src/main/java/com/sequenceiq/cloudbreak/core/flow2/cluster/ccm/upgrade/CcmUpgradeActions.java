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
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmRemoveAutoSshRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmRemoveAutoSshResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmReregisterToClusterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmReregisterToClusterProxyResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUnregisterHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUnregisterHostsResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.CcmUpgradePreparationResult;

@Configuration
public class CcmUpgradeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcmUpgradeActions.class);

    @Inject
    private CcmUpgradeService ccmUpgradeService;

    @Bean(name = "CCM_UPGRADE_PREPARATION_STATE")
    public Action<?, ?> ccmUpgradePreparation() {
        return new AbstractClusterAction<>(StackEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, StackEvent payload, Map<Object, Object> variables) {
                ccmUpgradeService.prepareCcmUpgradeOnCluster(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new CcmUpgradePreparationRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_RE_REGISTER_TO_CP")
    public Action<?, ?> reRegisterClusterToClusterProxy() {
        return new AbstractClusterAction<>(CcmUpgradePreparationResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CcmUpgradePreparationResult payload, Map<Object, Object> variables) {
                ccmUpgradeService.reRegisterClusterToClusterProxyOnCluster(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new CcmReregisterToClusterProxyRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_REMOVE_AUTOSSH")
    public Action<?, ?> removeAutoSsh() {
        return new AbstractClusterAction<>(CcmReregisterToClusterProxyResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CcmReregisterToClusterProxyResult payload, Map<Object, Object> variables) {
                ccmUpgradeService.removeAutoSshOnCluster(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new CcmRemoveAutoSshRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_UNREGISTER_HOSTS")
    public Action<?, ?> unregisterHosts() {
        return new AbstractClusterAction<>(CcmRemoveAutoSshResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CcmRemoveAutoSshResult payload, Map<Object, Object> variables) {
                ccmUpgradeService.unregisterHostsOnCluster(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new CcmUnregisterHostsRequest(context.getStackId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_FINISHED_STATE")
    public Action<?, ?> ccmUpgradeFinished() {
        return new AbstractClusterAction<>(CcmUnregisterHostsResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CcmUnregisterHostsResult payload, Map<Object, Object> variables) {
                ccmUpgradeService.ccmUpgradeFinished(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(CcmUpgradeEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_PREPARATION_FAILED")
    public Action<?, ?> ccmUpgradePreparationFailed() {
        return new AbstractStackFailureAction<CcmUpgradeState, CcmUpgradeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                if (payload.getException() != null) {
                    ccmUpgradeService.ccmUpgradePreparationFailed(payload.getResourceId());
                    sendEvent(context);
                }
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CcmUpgradeEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    @Bean(name = "CCM_UPGRADE_FAILED_STATE")
    public Action<?, ?> ccmUpgradeFailed() {
        return new AbstractStackFailureAction<CcmUpgradeState, CcmUpgradeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                ccmUpgradeService.ccmUpgradeFailed(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CcmUpgradeEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
