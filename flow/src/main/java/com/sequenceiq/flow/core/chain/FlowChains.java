package com.sequenceiq.flow.core.chain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

@Component
public class FlowChains {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChains.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Lazy
    @Inject
    private FlowStatCache flowStatCache;

    private final Map<String, FlowTriggerEventQueue> flowChainMap = new ConcurrentHashMap<>();

    private final Set<String> notSavedFlowChains = new ConcurrentSkipListSet<>();

    public void addNotSavedFlowChainLog(String flowChainId) {
        notSavedFlowChains.add(flowChainId);
    }

    public void putFlowChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue flowChain) {
        if (parentFlowChainId != null) {
            FlowTriggerEventQueue parentFlowChain = flowChainMap.get(parentFlowChainId);
            if (parentFlowChain != null) {
                flowChain = new FlowTriggerEventQueue(parentFlowChain.getFlowChainName() + "/" + flowChain.getFlowChainName(),
                        flowChain.getTriggerEvent(), flowChain.getQueue());
                flowChain.setParentFlowChainId(parentFlowChainId);
            }
        }
        flowChainMap.put(flowChainId, flowChain);
    }

    public Optional<Pair<String, Payload>> getRootTriggerEvent(String flowChainId) {
        String rootFlowChainId = getRootFlowChainId(flowChainId);
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(rootFlowChainId);
        if (flowTriggerEventQueue != null) {
            return Optional.of(Pair.of(rootFlowChainId, flowTriggerEventQueue.getTriggerEvent()));
        }
        Optional<FlowChainLog> initFlowChainLog = flowChainLogService.findRootInitFlowChainLog(flowChainId);
        if (initFlowChainLog.isEmpty()) {
            return Optional.empty();
        }

        LOGGER.info("Found root init flow chain log {} for flow chain {}", initFlowChainLog.get(), flowChainId);
        Payload triggerEvent = FlowLogUtil.tryDeserializeTriggerEvent(initFlowChainLog.get());
        if (triggerEvent == null) {
            return Optional.empty();
        } else {
            return Optional.of(Pair.of(initFlowChainLog.get().getFlowChainId(), triggerEvent));
        }
    }

    private String getRootFlowChainId(String flowChainId) {
        String parentFlowChainId = flowChainId;
        while (null != getParentFlowChainId(parentFlowChainId)) {
            parentFlowChainId = getParentFlowChainId(parentFlowChainId);
        }
        return parentFlowChainId;
    }

    public void removeFlowChain(String flowChainId, boolean success) {
        LOGGER.debug("Remove FlowChain: [{}]", flowChainId);
        if (flowChainId != null) {
            flowChainMap.remove(flowChainId);
            notSavedFlowChains.remove(flowChainId);
            flowStatCache.removeByFlowChainId(flowChainId, success);
        }
    }

    public void removeFullFlowChain(String flowChainId, boolean success) {
        LOGGER.debug("Remove FullFlowChain: [{}]", flowChainId);
        while (flowChainId != null) {
            String parentFlowChainId = getParentFlowChainId(flowChainId);
            removeFlowChain(flowChainId, success);
            flowChainId = parentFlowChainId;
        }
    }

    public void removeLastTriggerEvent(String flowChainId, String flowTriggerUserCrn) {
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
        LOGGER.debug("Removing LastTriggerEvent in [{}] with EventQueue: {}", flowChainId, flowTriggerEventQueue);
        if (flowTriggerEventQueue != null) {
            Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
            if (queue != null) {
                Selectable selectable = queue.poll();
                if (selectable != null) {
                    flowLogService.saveChain(flowChainId, getParentFlowChainId(flowChainId), flowTriggerEventQueue, flowTriggerUserCrn);
                }
            }
        }
    }

    public void cleanFlowChain(String flowChainId, String flowTriggerUserCrn) {
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
        LOGGER.debug("Cleaning FlowChain [{}] with EventQueue: {}", flowChainId, flowTriggerEventQueue);
        if (flowTriggerEventQueue != null) {
            Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
            if (queue != null) {
                if (!queue.isEmpty()) {
                    queue.clear();
                    flowLogService.saveChain(flowChainId, getParentFlowChainId(flowChainId), flowTriggerEventQueue, flowTriggerUserCrn);
                }
            }
        }
    }

    public void triggerNextFlow(String flowChainId, String flowTriggerUserCrn, Map<Object, Object> contextParams, String operationType,
            Optional<Runnable> finalizerCallback) {
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
        if (flowTriggerEventQueue != null) {
            LOGGER.info("Triggering the next flow in FlowChain [{}] with EventQueue: {}", flowChainId, flowTriggerEventQueue);
            Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
            if (queue != null) {
                Selectable selectable = queue.peek();
                if (selectable != null) {
                    sendEvent(flowTriggerEventQueue.getFlowChainName(), flowChainId, flowTriggerUserCrn, selectable, contextParams, operationType);
                } else {
                    String parentFlowChainId = getParentFlowChainId(flowChainId);
                    if (parentFlowChainId != null) {
                        flowChainMap.get(parentFlowChainId).getQueue().poll();
                        flowLogService.saveChain(parentFlowChainId, getParentFlowChainId(parentFlowChainId), flowChainMap.get(parentFlowChainId),
                                flowTriggerUserCrn);
                    }
                    triggerParentFlowChain(flowChainId, flowTriggerUserCrn, contextParams, operationType, finalizerCallback);
                }
            }
        }
    }

    protected void sendEvent(String flowChainType, String flowChainId, String flowTriggerUserCrn, Selectable selectable,
            Map<Object, Object> contextParams, String operationType) {
        LOGGER.debug("Triggering event: {}", selectable);
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_CHAIN_TYPE, flowChainType);
        headers.put(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        headers.put(FlowConstants.FLOW_TRIGGER_USERCRN, flowTriggerUserCrn);
        headers.put(FlowConstants.FLOW_OPERATION_TYPE, operationType);
        if (!CollectionUtils.isEmpty(contextParams)) {
            headers.put(FlowConstants.FLOW_CONTEXTPARAMS_ID, contextParams);
        }
        eventBus.notify(selectable.selector(), eventFactory.createEvent(headers, selectable));
    }

    private void triggerParentFlowChain(String flowChainId, String flowTriggerUserCrn, Map<Object, Object> contextParams, String operationType,
            Optional<Runnable> finalizerCallback) {
        String parentFlowChainId = getParentFlowChainId(flowChainId);
        LOGGER.debug("Triggering ParentFlowChain [{}] with flowChainId [{}]", parentFlowChainId, flowChainId);
        removeFlowChain(flowChainId, true);
        if (parentFlowChainId != null) {
            triggerNextFlow(parentFlowChainId, flowTriggerUserCrn, contextParams, operationType, finalizerCallback);
        } else {
            finalizerCallback.ifPresent(Runnable::run);
        }
    }

    public void saveAllUnsavedFlowChains(String flowChainId, String flowTriggerUserCrn) {
        LOGGER.debug("Saving all unsaved FlowChains for [{}]", flowChainId);
        List<Pair<String, FlowTriggerEventQueue>> flowTriggerEventQueues = new ArrayList<>();
        while (flowChainId != null && notSavedFlowChains.contains(flowChainId)) {
            String parentFlowChainId = getParentFlowChainId(flowChainId);
            FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
            if (flowTriggerEventQueue != null) {
                flowTriggerEventQueues.add(Pair.of(flowChainId, flowTriggerEventQueue));
            }
            flowChainId = parentFlowChainId;
        }
        Collections.reverse(flowTriggerEventQueues);
        flowTriggerEventQueues.forEach(chainIdTriggerQueue -> {
            String chainId = chainIdTriggerQueue.getLeft();
            FlowTriggerEventQueue queue = chainIdTriggerQueue.getRight();
            flowLogService.saveChain(chainId, queue.getParentFlowChainId(), queue, flowTriggerUserCrn);
        });
        notSavedFlowChains.removeAll(flowTriggerEventQueues.stream().map(Pair::getLeft).collect(Collectors.toList()));
    }

    private String getParentFlowChainId(String flowChainId) {
        if (flowChainId == null) {
            return null;
        }
        FlowTriggerEventQueue triggerEventQueue = flowChainMap.get(flowChainId);
        if (null == triggerEventQueue) {
            return null;
        }
        return triggerEventQueue.getParentFlowChainId();
    }
}
