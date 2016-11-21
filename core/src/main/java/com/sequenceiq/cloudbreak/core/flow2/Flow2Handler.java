package com.sequenceiq.cloudbreak.core.flow2;

import static com.sequenceiq.cloudbreak.core.flow2.FlowTriggers.STACK_FORCE_TERMINATE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.FlowTriggers.STACK_TERMINATE_TRIGGER_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.termination.ClusterTerminationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.userpasswd.ClusterCredentialChangeFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {
    public static final String FLOW_FINAL = "FLOWFINAL";

    public static final String FLOW_CANCEL = "FLOWCANCEL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.unmodifiableList(Arrays.asList(
            STACK_FORCE_TERMINATE_TRIGGER_EVENT, STACK_TERMINATE_TRIGGER_EVENT
    ));

    private static final List<Class<? extends FlowConfiguration>> RESTARTABLE_FLOWS = Collections.unmodifiableList(Arrays.asList(
            StackSyncFlowConfig.class, ClusterSyncFlowConfig.class, ClusterTerminationFlowConfig.class, ClusterCredentialChangeFlowConfig.class,
            ClusterStartFlowConfig.class, ClusterStopFlowConfig.class
    ));

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private List<FlowConfiguration<?>> flowConfigs;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowChainHandler flowChainHandler;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowLogRepository flowLogRepository;

    private Lock lock = new ReentrantLock(true);

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);

        if (FLOW_CANCEL.equals(key)) {
            cancelRunningFlows(payload.getStackId());
        } else if (FLOW_FINAL.equals(key)) {
            finalizeFlow(flowId, flowChainId, payload.getStackId());
        } else {
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                if (flowConfig != null && flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getStackId())) {
                    Flow flow;
                    lock.lock();
                    try {
                        if (!acceptFlow(key, payload)) {
                            LOGGER.info("Flow operation not allowed, other flow is running. Stack ID {}, event {}", payload.getStackId(), key);
                            return;
                        }
                        flowId = UUID.randomUUID().toString();
                        flow = flowConfig.createFlow(flowId);
                        flow.initialize();
                        flowLogService.save(flowId, flowChainId, key, payload, flowConfig.getClass(), flow.getCurrentState());
                    } finally {
                        lock.unlock();
                    }
                    runningFlows.put(flow, flowChainId);
                    flow.sendEvent(key, payload);
                }
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                Flow flow = runningFlows.get(flowId);
                if (flow != null) {
                    flowLogService.save(flowId, flowChainId, key, payload, flow.getFlowConfigClass(), flow.getCurrentState());
                    flow.sendEvent(key, payload);
                } else {
                    LOGGER.info("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getStackId(), flowId, key);
                }
            }
        }
    }

    private boolean acceptFlow(String key, Payload payload) {
        if (payload instanceof Acceptable) {
            Acceptable acceptable = (Acceptable) payload;
            if (!ALLOWED_PARALLEL_FLOWS.contains(key) && isOtherFlowRunning(payload.getStackId())) {
                acceptable.accepted().accept(Boolean.FALSE);
                return false;
            }
            acceptable.accepted().accept(Boolean.TRUE);
        }
        return true;
    }

    private boolean isOtherFlowRunning(Long stackId) {
        Set<String> flowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        return !flowIds.isEmpty();
    }

    private void cancelRunningFlows(Long stackId) {
        Set<String> flowIds = flowLogRepository.findAllRunningNonTerminationFlowIdsByStackId(stackId);
        LOGGER.debug("flow cancellation arrived: ids: {}", flowIds);
        for (String id : flowIds) {
            String flowChainId = runningFlows.getFlowChainId(id);
            if (flowChainId != null) {
                flowChains.removeFullFlowChain(flowChainId);
            }
            Flow flow = runningFlows.remove(id);
            if (flow != null) {
                flowLogService.cancel(stackId, id);
            }
        }
    }

    private void finalizeFlow(String flowId, String flowChainId, Long stackId) {
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
        if (RESTARTABLE_FLOWS.contains(flowLog.getFlowType())) {
            Optional<FlowConfiguration<?>> flowConfig = flowConfigs.stream()
                    .filter(fc -> fc.getClass().equals(flowLog.getFlowType())).findFirst();
            Flow flow = flowConfig.get().createFlow(flowId);
            runningFlows.put(flow, flowLog.getFlowChainId());
            if (flowLog.getFlowChainId() != null) {
                flowChainHandler.restoreFlowChain(flowLog.getFlowChainId());
            }
            flow.initialize(flowLog.getCurrentState());
            Object payload = JsonReader.jsonToJava(flowLog.getPayload());
            RestartAction restartAction = flowConfig.get().getRestartAction(flowLog.getNextEvent());
            if (restartAction != null) {
                restartAction.restart(flowId, flowLog.getFlowChainId(), flowLog.getNextEvent(), payload);
                return;
            }
        }
        flowLogService.terminate(flowLog.getStackId(), flowId);
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get("FLOW_ID");
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get("FLOW_CHAIN_ID");
    }
}
