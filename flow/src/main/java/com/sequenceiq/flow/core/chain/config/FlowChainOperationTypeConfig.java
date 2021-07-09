package com.sequenceiq.flow.core.chain.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Configuration
public class FlowChainOperationTypeConfig {

    @Resource
    private List<FlowEventChainFactory<?>> flowChainFactories;

    private final Map<String, OperationType> flowTypeOperationTypeMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (FlowEventChainFactory<?> flowEventChainFactory : flowChainFactories) {
            String flowType = flowEventChainFactory.getName();
            OperationType operationType = flowEventChainFactory.getFlowOperationType();
            if (flowTypeOperationTypeMap.get(flowType) != null) {
                throw new UnsupportedOperationException("Flow type already registered: " + flowType);
            }
            flowTypeOperationTypeMap.put(flowType, operationType);
        }
    }

    public Map<String, OperationType> getFlowTypeOperationTypeMap() {
        return flowTypeOperationTypeMap;
    }
}
