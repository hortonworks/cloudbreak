package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CHANGE_TUNNEL_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CHECK_PREREQUISITES_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_CLEANING_FAILURE_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_DEREGISTER_MINA_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FAILED_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINALIZE_FAILED_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_FINISHED_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_PUSH_SALT_STATES_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_RECONFIGURE_NGINX_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REMOVE_MINA_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REVERT_ALL_FAILURE_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_REVERT_FAILURE_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState.UPGRADE_CCM_UPGRADE_STATE_NAME;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_APPLY_UPGRADE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_CHANGE_TUNNEL_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_CHECK_PREREQUISITES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_DEREGISTER_MINA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_OBTAIN_AGENT_DATA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_PUSH_SALT_STATES_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_RECONFIGURE_NGINX_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REMOVE_MINA_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_REVERT_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINALIZE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_FINISHED_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmState;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmFailureEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler.AbstractUpgradeCcmEventHandler;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector;

@Configuration
public class UpgradeCcmActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmActions.class);

    @Inject
    private UpgradeCcmService upgradeCcmService;

    @Bean(name = UPGRADE_CCM_CHECK_PREREQUISITES_STATE_NAME)
    public Action<?, ?> checkPrerequisites() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmTriggerEvent.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
                setFinalChain(variables, payload.isFinalFlow());
                setChainedAction(variables, payload.isChained());
                LOGGER.info("Starting checking prerequisites for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.checkPrerequisitesState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_CHECK_PREREQUISITES_EVENT.create(context.getStack().getId(), payload.getOldTunnel()));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_CHANGE_TUNNEL_STATE_NAME)
    public Action<?, ?> changeTunnel() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting changing tunnel type for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.changeTunnelState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_CHANGE_TUNNEL_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_OBTAIN_AGENT_DATA_STATE_NAME)
    public Action<?, ?> obtainAgentData() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting obtaining agent data for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.obtainAgentDataState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_OBTAIN_AGENT_DATA_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_PUSH_SALT_STATES_STATE_NAME)
    public Action<?, ?> pushSaltStates() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting pushing salt states for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.pushSaltStatesState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_PUSH_SALT_STATES_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_UPGRADE_STATE_NAME)
    public Action<?, ?> upgrade() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting running upgrade for FreeIPA CCM {}", payload);
                upgradeCcmService.upgradeState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_APPLY_UPGRADE_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_RECONFIGURE_NGINX_STATE_NAME)
    public Action<?, ?> reconfigure() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting running reconfiguration for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.reconfigureNginxState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_RECONFIGURE_NGINX_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_REGISTER_CLUSTER_PROXY_STATE_NAME)
    public Action<?, ?> registerCcm() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting registering CCM for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.registerClusterProxyState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_REGISTER_CLUSTER_PROXY_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_REMOVE_MINA_STATE_NAME)
    public Action<?, ?> removeMina() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting removing mina for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.removeMinaState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_REMOVE_MINA_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_DEREGISTER_MINA_STATE_NAME)
    public Action<?, ?> deregisterMina() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting deregistering from mina for FreeIPA CCM upgrade {}", payload);
                upgradeCcmService.deregisterMinaState(context.getStack().getId());
                sendEvent(context, UPGRADE_CCM_DEREGISTER_MINA_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_FINISHED_STATE_NAME)
    public Action<?, ?> finished() {
        return new AbstractUpgradeCcmEventAction() {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmEvent payload, Map<Object, Object> variables) throws CloudbreakOrchestratorException {
                LOGGER.info("FreeIPA CCM upgrade finished {}", payload);
                StackEvent stackEvent = new StackEvent(context.getStack().getId());
                String event = UPGRADE_CCM_FINISHED_EVENT.event();
                try {
                    upgradeCcmService.finishedState(context.getStack().getId(), payload.getMinaRemoved());
                    completeOperation(context.getStack().getAccountId(), context.getStack().getEnvironmentCrn(), variables);
                } catch (CloudbreakOrchestratorException e) {
                    LOGGER.warn("Finalizing CCM upgrade is failed with {}", e);
                    stackEvent = new UpgradeCcmFailureEvent(UPGRADE_CCM_FINALIZE_FAILED_EVENT.event(),
                            payload.getResourceId(),
                            payload.getOldTunnel(),
                            AbstractUpgradeCcmEventHandler.class,
                            e,
                            payload.getRevertTime(),
                            "Finalizing CCM upgrade is failed");
                    event = UPGRADE_CCM_FINALIZE_FAILED_EVENT.event();
                }
                sendEvent(context, event, stackEvent);
            }
        };
    }

    @Bean(name = UPGRADE_CCM_FAILED_STATE_NAME)
    public Action<?, ?> failedRevertTunnel() {

        return new AbstractUpgradeCcmAction<>(UpgradeCcmFailureEvent.class) {
            @Override
            protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<UpgradeCcmState,
                    UpgradeCcmStateSelector> stateContext, UpgradeCcmFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("FreeIPA CCM upgrade failed {}", payload);
                upgradeCcmService.changeTunnel(payload.getResourceId(), payload.getOldTunnel());
                upgradeCcmService.failedState(context, payload);
                failOperation(context.getStack().getAccountId(), payload.getException().getMessage(), variables);
                sendEvent(context, UPGRADE_CCM_FAILURE_HANDLED_EVENT.event(), new StackEvent(context.getStack().getId()));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_FINALIZE_FAILED_STATE_NAME)
    public Action<?, ?> failedButDoNothing() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmFailureEvent.class) {
            @Override
            protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<UpgradeCcmState,
                    UpgradeCcmStateSelector> stateContext, UpgradeCcmFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("FreeIPA CCM upgrade failed {}", payload);
                upgradeCcmService.failedState(context, payload);
                failOperation(context.getStack().getAccountId(), payload.getException().getMessage(), variables);
                sendEvent(context, UPGRADE_CCM_FAILURE_HANDLED_EVENT.event(), new StackEvent(context.getStack().getId()));
            }
        };
    }


    @Bean(name = UPGRADE_CCM_REVERT_FAILURE_STATE_NAME)
    public Action<?, ?> failedRevertTunnelAndSaltStates() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmFailureEvent.class) {
            @Override
            protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<UpgradeCcmState,
                    UpgradeCcmStateSelector> stateContext, UpgradeCcmFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("FreeIPA CCM upgrade failed, reverting {}", payload);
                sendEvent(context, UPGRADE_CCM_REVERT_FAILURE_EVENT.createBasedOn(payload));
            }
        };
    }

    @Bean(name = UPGRADE_CCM_REVERT_ALL_FAILURE_STATE_NAME)
    public Action<?, ?> failedRevertAll() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmFailureEvent.class) {
            @Override
            protected UpgradeCcmContext createFlowContext(FlowParameters flowParameters, StateContext<UpgradeCcmState,
                    UpgradeCcmStateSelector> stateContext, UpgradeCcmFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("FreeIPA CCM upgrade failed, reverting everything {}", payload);
                sendEvent(context, UPGRADE_CCM_REVERT_ALL_FAILURE_EVENT.createBasedOn(payload));
            }
        };
    }
}
