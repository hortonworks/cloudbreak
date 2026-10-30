package com.sequenceiq.flow.core.chain;

import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.tryDeserializeTriggerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowContextParamsUtil;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.config.FlowChainOperationTypeConfig;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;

@Component
public class FlowChainHandler implements Consumer<Event<? extends Payload>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChainHandler.class);

    @Resource
    private Map<String, FlowEventChainFactory<Payload>> flowChainConfigMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowStatCache flowStatCache;

    @Inject
    private FlowChainOperationTypeConfig flowChainOperationTypeConfig;

    @Inject
    private Optional<List<InheritedFlowChainContextParamsProvider>> inheritedContextParamsProviders;

    private Set<String> inheritedContextParamKeys = Set.of();

    @PostConstruct
    void initInheritedContextParamKeys() {
        inheritedContextParamKeys = inheritedContextParamsProviders.orElse(List.of()).stream()
                .map(InheritedFlowChainContextParamsProvider::inheritedContextParamKeys)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.debug("Nested flow chains inherit the following context params: {}", inheritedContextParamKeys);
    }

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = event.getKey();
        String parentFlowChainId = getFlowChainId(event);
        String flowTriggerUserCrn = getFlowTriggerUserCrn(event);
        FlowEventChainFactory<Payload> flowEventChainFactory = flowChainConfigMap.get(key);
        String flowOperationType = flowEventChainFactory.getFlowOperationType().name();
        String flowChainId = UUID.randomUUID().toString();
        Map<Object, Object> contextParams = FlowContextParamsUtil.filter(FlowContextParamsUtil.getContextParams(event), inheritedContextParamKeys);
        flowChains.putFlowChain(flowChainId, parentFlowChainId, flowEventChainFactory.createFlowTriggerEventQueue(event.getData(), contextParams));
        flowChains.addNotSavedFlowChainLog(flowChainId);
        flowStatCache.putByFlowChainId(flowChainId, event.getData().getResourceId(), flowOperationType, false);
        flowChains.triggerNextFlow(flowChainId, flowTriggerUserCrn, contextParams,
                flowOperationType, Optional.empty());
    }

    public void restoreFlowChain(String flowChainId) {
        Optional<FlowChainLog> chainLogOpt = flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        if (chainLogOpt.isPresent()) {
            FlowChainLog chainLog = chainLogOpt.get();
            String flowChainType = chainLog.getFlowChainType();
            Queue<Selectable> queue = chainLog.getChainAsQueue();
            Payload triggerEvent = tryDeserializeTriggerEvent(chainLog);
            FlowTriggerEventQueue chain = new FlowTriggerEventQueue(flowChainType, triggerEvent, queue);
            if (chainLog.getParentFlowChainId() != null) {
                chain.setParentFlowChainId(chainLog.getParentFlowChainId());
            }
            flowChains.putFlowChain(flowChainId, chainLog.getParentFlowChainId(), chain);
            Selectable selectable = queue.peek();
            if (selectable != null) {
                OperationType operationType = flowChainOperationTypeConfig.getFlowTypeOperationTypeMap().getOrDefault(flowChainType, OperationType.UNKNOWN);
                flowStatCache.putByFlowChainId(flowChainId, selectable.getResourceId(), operationType.name(), true);
            }
            if (chainLog.getParentFlowChainId() != null) {
                restoreFlowChain(chainLog.getParentFlowChainId());
            }
        }
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_CHAIN_ID);
    }

    private String getFlowTriggerUserCrn(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
    }
}
