package com.sequenceiq.cloudbreak.core.flow2;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent.TERMINATION_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.reset.ClusterResetFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upgrade.ClusterUpgradeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.repair.ManualStackRepairTriggerFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {
    public static final String FLOW_ID = "FLOW_ID";

    public static final String FLOW_CHAIN_ID = "FLOW_CHAIN_ID";

    public static final String FLOW_FINAL = "FLOWFINAL";

    public static final String FLOW_CANCEL = "FLOWCANCEL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(TERMINATION_EVENT.event());

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = Arrays.asList(
            StackCreationFlowConfig.class,
            StackSyncFlowConfig.class, StackTerminationFlowConfig.class, StackStopFlowConfig.class, StackStartFlowConfig.class,
            StackUpscaleConfig.class, StackDownscaleConfig.class,
            InstanceTerminationFlowConfig.class,
            ManualStackRepairTriggerFlowConfig.class,
            ClusterCreationFlowConfig.class,
            ClusterSyncFlowConfig.class, ClusterTerminationFlowConfig.class, ClusterCredentialChangeFlowConfig.class,
            ClusterStartFlowConfig.class, ClusterStopFlowConfig.class,
            ClusterUpscaleFlowConfig.class, ClusterDownscaleFlowConfig.class,
            ClusterUpgradeFlowConfig.class, ClusterResetFlowConfig.class, ChangePrimaryGatewayFlowConfig.class
    );

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Resource
    private List<String> failHandledEvents;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowChainHandler flowChainHandler;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private TransactionService transactionService;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);

        try {
            handle(key, payload, flowId, flowChainId);
        } catch (TransactionExecutionException e) {
            LOGGER.error("Failed update last flow log status and save new flow log entry.", e);
            runningFlows.remove(flowId);
        }
    }

    private void handle(String key, Payload payload, String flowId, String flowChainId) throws TransactionExecutionException {
        switch (key) {
            case FLOW_CANCEL:
                cancelRunningFlows(payload.getStackId());
                break;
            case FLOW_FINAL:
                finalizeFlow(flowId, flowChainId, payload.getStackId());
                break;
            default:
                if (flowId == null) {
                    LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                    FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                    if (flowConfig != null && flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getStackId())) {
                        if (!isFlowAcceptable(key, payload)) {
                            LOGGER.info("Flow operation not allowed, other flow is running. Stack ID {}, event {}", payload.getStackId(), key);
                            return;
                        }
                        flowId = UUID.randomUUID().toString();
                        Flow flow = flowConfig.createFlow(flowId, payload.getStackId());
                        flow.initialize();
                        flowLogService.save(flowId, flowChainId, key, payload, null, flowConfig.getClass(), flow.getCurrentState());
                        acceptFlow(payload);
                        logFlowId(flowId);
                        runningFlows.put(flow, flowChainId);
                        flow.sendEvent(key, payload);
                    }
                } else {
                    LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                    Flow flow = runningFlows.get(flowId);
                    if (flow != null) {
                        transactionService.required(() -> {
                            flowLogService.updateLastFlowLogStatus(flow.getFlowId(), failHandledEvents.contains(key));
                            flowLogService.save(flow.getFlowId(), flowChainId, key, payload, flow.getVariables(),
                                    flow.getFlowConfigClass(), flow.getCurrentState());
                            return null;
                        });
                        flow.sendEvent(key, payload);
                    } else {
                        LOGGER.debug("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getStackId(), flowId, key);
                    }
                }
                break;
        }
    }

    private boolean isFlowAcceptable(String key, Payload payload) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!ALLOWED_PARALLEL_FLOWS.contains(key) && flowLogService.isOtherFlowRunning(payload.getStackId())) {
                acceptable.accepted().accept(Boolean.FALSE);
                return false;
            }
        }
        return true;
    }

    private void acceptFlow(Payload payload) {
        if (payload instanceof Acceptable && ((Acceptable) payload).accepted() != null) {
            Acceptable acceptable = (Acceptable) payload;
            if (!acceptable.accepted().isComplete()) {
                acceptable.accepted().accept(Boolean.TRUE);
            }
        }
    }

    private void cancelRunningFlows(Long stackId) throws TransactionExecutionException {
        Set<String> flowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        LOGGER.debug("flow cancellation arrived: ids: {}", flowIds);
        for (String id : flowIds) {
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            Flow flow = runningFlows.remove(id);
            if (flow != null) {
                flow.stop();
                flowLogService.cancel(stackId, id);
            }
        }
    }

    private void finalizeFlow(String flowId, String flowChainId, Long stackId) throws TransactionExecutionException {
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId);
        if (flowChainId != null) {
            if (flow.isFlowFailed()) {
                flowChains.removeFullFlowChain(flowChainId);
            } else {
                flowChains.triggerNextFlow(flowChainId);
            }
        }
    }

    public void restartFlow(String flowId) {
        FlowLog flowLog = flowLogRepository.findFirstByFlowIdOrderByCreatedDesc(flowId);
        restartFlow(flowLog);
    }

    public void restartFlow(FlowLog flowLog) {
        if (RESTARTABLE_FLOWS.contains(flowLog.getFlowType())) {
            Optional<FlowConfiguration<?>> flowConfig = flowConfigs.stream()
                    .filter(fc -> fc.getClass().equals(flowLog.getFlowType())).findFirst();
            Payload payload = (Payload) JsonReader.jsonToJava(flowLog.getPayload());
            Flow flow = flowConfig.get().createFlow(flowLog.getFlowId(), payload.getStackId());
            runningFlows.put(flow, flowLog.getFlowChainId());
            if (flowLog.getFlowChainId() != null) {
                flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
            }
            Map<Object, Object> variables = (Map<Object, Object>) JsonReader.jsonToJava(flowLog.getVariables());
            flow.initialize(flowLog.getCurrentState(), variables);
            RestartAction restartAction = flowConfig.get().getRestartAction(flowLog.getNextEvent());
            if (restartAction != null) {
                restartAction.restart(flowLog.getFlowId(), flowLog.getFlowChainId(), flowLog.getNextEvent(), payload);
                return;
            }
        }
        try {
            flowLogService.terminate(flowLog.getStackId(), flowLog.getFlowId());
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get(FLOW_ID);
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FLOW_CHAIN_ID);
    }

    private void logFlowId(String flowId) {
        String trackingId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.TRACKING_ID.toString());
        LOGGER.debug("Flow has been created with id: '{}' and the related tracking id: '{}'.", flowId, trackingId);
    }
}
